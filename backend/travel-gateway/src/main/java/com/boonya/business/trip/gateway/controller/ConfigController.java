package com.boonya.business.trip.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 动态刷新配置示例
 */
@RestController
@RefreshScope
public class ConfigController {

    @Value("${travel.policy.check-enabled:true}")
    private Boolean policyCheckEnabled;

    @GetMapping("/config/policy-check")
    public Boolean getPolicyCheckEnabled() {
        return policyCheckEnabled;
    }
}