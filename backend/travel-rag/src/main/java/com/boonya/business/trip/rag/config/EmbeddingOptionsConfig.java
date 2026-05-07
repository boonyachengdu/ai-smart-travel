package com.boonya.business.trip.rag.config;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingOptionsConfig {

    @Bean
    public EmbeddingOptions dashScopeEmbeddingOptions() {
        return new DashScopeEmbeddingOptions();
    }
}
