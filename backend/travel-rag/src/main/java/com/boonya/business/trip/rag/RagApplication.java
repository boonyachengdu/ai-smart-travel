package com.boonya.business.trip.rag;

import com.boonya.business.trip.cache.redis.config.RedisTemplateConfig;
import com.boonya.business.trip.common.aop.EntityNameFillAspect;
import com.boonya.business.trip.common.aop.UserHolderAspect;
import com.boonya.business.trip.common.component.cache.impl.DefaultCacheService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

@Import({
        RedisTemplateConfig.class,
        DefaultCacheService.class,
        EntityNameFillAspect.class,
        UserHolderAspect.class
})
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class RagApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagApplication.class, args);
    }

}
