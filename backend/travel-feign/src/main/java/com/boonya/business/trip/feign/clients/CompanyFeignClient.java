package com.boonya.business.trip.feign.clients;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.config.FeignClientConfig;
import com.boonya.business.trip.common.entity.Company;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * travel-user 服务的 Feign 声明式客户端
 * 注意：name 值必须与 travel-user 的 spring.application.name 完全一致
 */
@FeignClient(
        name = "travel-user",
        path = "/company",
        configuration = FeignClientConfig.class
)
public interface CompanyFeignClient {

    /**
     * 根据ID查询单个企业
     */
    @GetMapping("/{id}")
    public Response<Company> getById(@PathVariable("id") Long id);
}
