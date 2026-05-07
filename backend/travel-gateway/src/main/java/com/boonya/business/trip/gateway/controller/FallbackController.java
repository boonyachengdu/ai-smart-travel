package com.boonya.business.trip.gateway.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RequestMapping
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public Mono<Map<String, Object>> fallback() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 404);
        result.put("message", "Service not found");
        return Mono.just(result);
    }
}
