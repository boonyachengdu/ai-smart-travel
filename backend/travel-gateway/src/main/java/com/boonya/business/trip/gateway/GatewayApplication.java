package com.boonya.business.trip.gateway;

import com.boonya.business.trip.common.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Import;

//@Import(GlobalExceptionHandler.class)
@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        ReactiveSecurityAutoConfiguration.class
})
@EnableDiscoveryClient
@RefreshScope// 配置动态刷新
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
