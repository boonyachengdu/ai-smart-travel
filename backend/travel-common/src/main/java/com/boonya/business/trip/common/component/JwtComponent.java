package com.boonya.business.trip.common.component;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtComponent {

    private SecretKey secretKey;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    // 初始化密钥，使用 Base64 解码
    private SecretKey getSecretKey() {
        if (secretKey == null) {
            byte[] decodedKey = Base64.getDecoder().decode(secret);
            secretKey = Keys.hmacShaKeyFor(decodedKey);
        }
        return secretKey;
    }

    public String generateToken(String username, Long userId, Long companyId, Long employeeId, Long deptId,String roles) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("companyId", companyId)
                .claim("employeeId", employeeId)
                .claim("deptId", deptId)
                .claim("roles", roles)
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}