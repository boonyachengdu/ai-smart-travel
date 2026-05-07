package com.boonya.business.trip.gateway.utils;

import com.boonya.business.trip.gateway.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * JWT 解析与验证工具
 */
@Component
public class JwtUtil {
    private SecretKey secretKey;

    public JwtUtil(JwtConfig jwtConfig) {
        // 支持 Base64 编码的密钥，若解码失败则直接使用原始字符串
        try {
            byte[] decodedKey = Base64.getDecoder().decode(jwtConfig.getSecret());
            this.secretKey = Keys.hmacShaKeyFor(decodedKey);
        } catch (IllegalArgumentException e) {
            this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 解析 JWT
     */
    public Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
    }

    /**
     * 验证 JWT 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从 JWT 中获取用户 ID（subject）
     */
    public String getUserIdFromToken(String token) {
        return parseToken(token).getPayload().getSubject();
    }
}
