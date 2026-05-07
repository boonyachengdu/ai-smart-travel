//package com.boonya.business.trip.gateway.filter;
//
//import lombok.var;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.web.server.WebFilter;
//import org.springframework.web.server.WebFilterChain;
//import reactor.core.publisher.Mono;
//
//@Component
//@Order(Ordered.HIGHEST_PRECEDENCE)
//public class SecurityHeadersFilter implements WebFilter {
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
//        return chain.filter(exchange)
//                .then(Mono.fromRunnable(() -> {
//                    var headers = exchange.getResponse().getHeaders();
//                    headers.set("X-Content-Type-Options", "nosniff");
//                    headers.set("X-Frame-Options", "DENY");
//                    headers.set("X-XSS-Protection", "1; mode=block");
//                    headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
//                    headers.set("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'");
//                    headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
//                    headers.set("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
//                }));
//    }
//}
