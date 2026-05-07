package com.boonya.business.trip.common.config;

import com.boonya.business.trip.common.context.UserHolder;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor userContextRequestInterceptor() {
        return template -> {
            try {
                // 从当前线程获取用户上下文
                UserHolder userHolder = UserHolder.get();

                if (userHolder != null && userHolder.getUserId() != null) {
                    // 通过 Header 传递用户信息
                    template.header("X-User-Id", userHolder.getUserId().toString());
                    template.header("X-User-Name", userHolder.getUsername());
                    template.header("X-Company-Id", userHolder.getCompanyId().toString());
                    template.header("X-Employee-Id", userHolder.getEmployeeId().toString());
                    template.header("X-Dept-Id", userHolder.getDeptId().toString());
                    template.header("X-Roles", userHolder.getRoles());

                    log.debug("Feign 请求添加用户上下文：userId={}, username={}",
                            userHolder.getUserId(), userHolder.getUsername());
                } else {
                    log.warn("UserHolder 为空或 userId 为空，无法传递上下文");
                }

            } catch (Exception e) {
                log.error("获取用户上下文失败", e);
            }
        };
    }
}

