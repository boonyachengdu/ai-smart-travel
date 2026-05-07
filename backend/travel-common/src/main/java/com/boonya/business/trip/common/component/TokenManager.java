package com.boonya.business.trip.common.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TokenManager {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String LOCK_PREFIX = "token:lock:";
    private static final long LOCK_WAIT_TIME_SECONDS = 5;
    private static final long LOCK_LEASE_TIME_SECONDS = 10;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> unlockScript;

    public TokenManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.unlockScript = new DefaultRedisScript<>();
        this.unlockScript.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "   return redis.call('del', KEYS[1]) " +
                        "else " +
                        "   return 0 " +
                        "end"
        );
        this.unlockScript.setResultType(Long.class);
    }

    /**
     * 将 token 加入黑名单（使用分布式锁保证并发安全）
     * @param token JWT token
     * @param ttlSeconds token 剩余有效时间（秒）
     */
    public void addToBlacklist(String token, long ttlSeconds) {
        if (token == null || ttlSeconds <= 0) {
            log.warn("无效的参数：token={}, ttlSeconds={}", token, ttlSeconds);
            return;
        }

        String blacklistKey = BLACKLIST_PREFIX + token;
        String lockKey = LOCK_PREFIX + token;
        String lockValue = generateLockValue();

        if (!tryLock(lockKey, lockValue)) {
            log.warn("获取分布式锁失败，token 可能正在被处理：{}", token);
            return;
        }

        try {
            Boolean exists = redisTemplate.hasKey(blacklistKey);
            if (Boolean.TRUE.equals(exists)) {
                log.debug("Token 已在黑名单中，跳过处理：{}", token);
                return;
            }

            redisTemplate.opsForValue().set(blacklistKey, "blacklisted", ttlSeconds, TimeUnit.SECONDS);
            log.info("Token 已加入黑名单，userId={}, TTL={}s", token, ttlSeconds);
        } catch (Exception e) {
            log.error("将 token 加入黑名单失败：{}", blacklistKey, e);
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    /**
     * 检查 token 是否在黑名单中
     * @param token JWT token
     * @return true-在黑名单中，false-不在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }

        String key = BLACKLIST_PREFIX + token;
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("检查 token 黑名单状态失败：{}", key, e);
            return false;
        }
    }

    /**
     * 从黑名单中移除 token（使用分布式锁保证并发安全）
     * @param token JWT token
     */
    public void removeFromBlacklist(String token) {
        if (token == null) {
            return;
        }

        String key = BLACKLIST_PREFIX + token;
        String lockKey = LOCK_PREFIX + token;
        String lockValue = generateLockValue();

        if (!tryLock(lockKey, lockValue)) {
            log.warn("获取分布式锁失败，无法移除 token：{}", token);
            return;
        }

        try {
            redisTemplate.delete(key);
            log.debug("Token 已从黑名单移除：{}", key);
        } catch (Exception e) {
            log.error("从黑名单移除 token 失败：{}", key, e);
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    /**
     * 获取黑名单 key
     */
    public String getBlacklistKey(String token) {
        return BLACKLIST_PREFIX + token;
    }

    /**
     * 尝试获取分布式锁（带锁值标识）
     */
    private boolean tryLock(String lockKey, String lockValue) {
        try {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey,
                            lockValue,
                            LOCK_LEASE_TIME_SECONDS,
                            TimeUnit.SECONDS);
            return success != null && success;
        } catch (Exception e) {
            log.error("获取分布式锁异常：{}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放分布式锁（使用 Lua 脚本保证原子性）
     */
    private void unlock(String lockKey, String expectedValue) {
        try {
            Long result = redisTemplate.execute(
                    unlockScript,
                    Collections.singletonList(lockKey),
                    expectedValue
            );

            if (result != null && result > 0) {
                log.debug("分布式锁已通过 Lua 脚本释放：{}", lockKey);
            } else {
                log.warn("锁不存在或值不匹配，未释放：lockKey={}", lockKey);
            }
        } catch (Exception e) {
            log.error("通过 Lua 脚本释放分布式锁异常：{}", lockKey, e);

            // 降级处理：如果 Lua 脚本执行失败，尝试直接删除（非原子操作）
            try {
                String currentValue = redisTemplate.opsForValue().get(lockKey);
                if (expectedValue.equals(currentValue)) {
                    redisTemplate.delete(lockKey);
                    log.debug("降级处理：分布式锁已直接删除：{}", lockKey);
                }
            } catch (Exception ex) {
                log.error("降级删除锁失败：{}", lockKey, ex);
            }
        }
    }

    /**
     * 生成唯一的锁值（UUID + 纳秒级时间戳）
     */
    private String generateLockValue() {
        return String.format("%s:%d",
                UUID.randomUUID().toString(),
                System.nanoTime());
    }
}
