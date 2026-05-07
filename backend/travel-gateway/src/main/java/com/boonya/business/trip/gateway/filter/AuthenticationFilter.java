package com.boonya.business.trip.gateway.filter;

import com.boonya.business.trip.gateway.config.JwtConfig;
import com.boonya.business.trip.gateway.utils.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtConfig jwtConfig;
    private final JwtUtil jwtUtil;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final RedissonClient redissonClient;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String LOCK_PREFIX = "jwt:lock:";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final long LOCK_TIMEOUT_SECONDS = 5;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isWhiteListed(path)) {
            log.debug("白名单路径：{}, 直接放行", path);
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            log.debug("请求路径：{}, 无效的 token", path);
            return unauthorizedResponse(exchange, "Missing or invalid Authorization header");
        }

        if (!jwtUtil.validateToken(token)) {
            log.debug("请求路径：{}, 无效的 token", path);
            return unauthorizedResponse(exchange, "Invalid token");
        }

        return checkBlacklist(token).flatMap(isBlacklisted -> {
            if (isBlacklisted) {
                log.debug("请求路径：{}, token 已被加入黑名单", path);
                return unauthorizedResponse(exchange, "Token has been logged out");
            }

            String userId = jwtUtil.getUserIdFromToken(token);

            if (isLogoutPath(path)) {
                log.debug("请求路径：{}, 登出请求，将 token 加入黑名单", path);
                return addToBlacklistWithLock(token)
                        .then(forwardWithUserId(exchange, chain, userId));
            }

            return forwardWithUserId(exchange, chain, token);
        });
    }

    /**
     * 将用户 ID 加入请求头，继续转发
     */
    private Mono<Void> forwardWithUserId(ServerWebExchange exchange, GatewayFilterChain chain, String token) {
        Claims claims = jwtUtil.parseToken(token).getPayload();

        Object userIdObj = claims.get("userId");
        Object companyIdObj = claims.get("companyId");
        Object employeeIdObj = claims.get("employeeId");
        Object deptIdObj = claims.get("deptId");
        Object rolesObj = claims.get("roles");

        String userId = userIdObj != null ? userIdObj.toString() : null;
        String companyId = companyIdObj != null ? companyIdObj.toString() : null;
        String employeeId = employeeIdObj != null ? employeeIdObj.toString() : null;
        String deptId = deptIdObj != null ? deptIdObj.toString() : null;
        String roles = rolesObj != null ? rolesObj.toString() : null;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-User-Name", claims.getSubject())
                .header("X-Company-Id", companyId)
                .header("X-Employee-Id", employeeId)
                .header("X-Dept-Id", deptId)
                .header("X-Roles", roles)
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 判断是否在白名单
     */
    private boolean isWhiteListed(String path) {
        return jwtConfig.getWhiteList().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 判断是否为登出路径
     */
    private boolean isLogoutPath(String path) {
        return pathMatcher.match(jwtConfig.getLogoutPath(), path);
    }

    /**
     * 从 Authorization 头提取 Bearer token
     */
    private String extractToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /**
     * 返回 401 未授权响应
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("code", 401);
        body.put("message", message);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = message.getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 检查 token 是否在黑名单中
     */
    private Mono<Boolean> checkBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        return reactiveRedisTemplate.hasKey(key);
    }

    /**
     * 使用分布式锁将 token 加入黑名单（避免并发重复操作）
     */
    private Mono<Void> addToBlacklistWithLock(String token) {
        return Mono.fromCallable(() -> {
            String lockKey = LOCK_PREFIX + token;
            RLock lock = redissonClient.getLock(lockKey);

            boolean locked = false;
            try {
                locked = lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (!locked) {
                    log.warn("获取分布式锁超时，token 可能正在被处理：{}", token);
                    return null;
                }

                String blacklistKey = BLACKLIST_PREFIX + token;
                Boolean exists = reactiveRedisTemplate.hasKey(blacklistKey).block();

                if (Boolean.TRUE.equals(exists)) {
                    log.debug("Token 已在黑名单中，跳过处理：{}", token);
                    return null;
                }

                long ttlSeconds = getTokenRemainingValidity(token);
                reactiveRedisTemplate.opsForValue()
                        .set(blacklistKey, "blacklisted", Duration.ofSeconds(ttlSeconds))
                        .block();

                log.info("Token 已加入黑名单，key={}, TTL={}s", blacklistKey, ttlSeconds);
                return null;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("获取分布式锁被中断：{}", lockKey, e);
                throw new RuntimeException("获取锁被中断", e);
            } catch (Exception e) {
                log.error("将 token 加入黑名单失败：{}", token, e);
                throw new RuntimeException("加入黑名单失败", e);
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    try {
                        lock.unlock();
                        log.debug("分布式锁已释放：{}", lockKey);
                    } catch (Exception e) {
                        log.error("释放分布式锁异常：{}", lockKey, e);
                    }
                }
            }
        }).then();
    }

    /**
     * 获取 token 剩余有效秒数
     */
    private long getTokenRemainingValidity(String token) {
        try {
            var claims = jwtUtil.parseToken(token).getPayload();
            long exp = claims.getExpiration().getTime();
            long now = System.currentTimeMillis();
            return Math.max(0, (exp - now) / 1000);
        } catch (Exception e) {
            log.error("解析 token 过期时间失败，使用默认 24 小时", e);
            return 86400;
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
