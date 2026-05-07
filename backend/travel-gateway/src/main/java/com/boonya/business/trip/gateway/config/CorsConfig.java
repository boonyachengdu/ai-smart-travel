package com.boonya.business.trip.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 全局 CORS 配置，允许 Vue 前端跨域访问
 */
@Configuration
public class CorsConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${front-end.allowedOrigins}")
    private String allowedOrigins;

    @Value("${front-end.maxAge:3600}")
    private Long maxAge;

    @Bean
    public CorsWebFilter corsWebFilter() {
        if ("prod".equals(activeProfile)) {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowCredentials(true);

            for (String origin : allowedOrigins.split(",")) {
                config.addAllowedOriginPattern(origin.trim());
            }

            config.addAllowedHeader("*");
            config.addAllowedMethod("GET");
            config.addAllowedMethod("POST");
            config.addAllowedMethod("PUT");
            config.addAllowedMethod("DELETE");
            config.addAllowedMethod("OPTIONS");
            config.setMaxAge(maxAge);
            config.addExposedHeader("Authorization");
            config.addExposedHeader("Content-Disposition");

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", config);
            return new CorsWebFilter(source);
        } else {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowCredentials(true);
            //config.addAllowedOrigin(allowedOrigins); // 根据实际 Vue 地址修改
            config.addAllowedOriginPattern("*");// 允许所有来源（仅开发环境）
            config.addAllowedHeader("*");
            config.addAllowedMethod("*");
            config.addExposedHeader("*");

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", config);
            return new CorsWebFilter(source);
        }
    }
}