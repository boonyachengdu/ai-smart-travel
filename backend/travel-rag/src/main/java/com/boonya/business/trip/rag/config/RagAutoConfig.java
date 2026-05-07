package com.boonya.business.trip.rag.config;

import com.boonya.business.trip.cache.redis.service.RedisChatMemory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RagAutoConfig {
    @Value("${spring.ai.message.limit:120}")
    private Integer messageLimit;

    @Bean
    public ChatMemory chatMemory(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        return new RedisChatMemory(stringRedisTemplate, objectMapper, messageLimit);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
