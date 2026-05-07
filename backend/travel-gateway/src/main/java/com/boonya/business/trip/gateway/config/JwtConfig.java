package com.boonya.business.trip.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JWT 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    /**
     * 签名密钥（支持 Base64 编码或原始字符串）
     */
    private String secret;

    /**
     * 白名单路径（不验证 token）
     */
    private List<String> whiteList;

    /**
     * 登出路径（验证 token 后加入黑名单）
     */
    private String logoutPath = "/auth/logout";
}
