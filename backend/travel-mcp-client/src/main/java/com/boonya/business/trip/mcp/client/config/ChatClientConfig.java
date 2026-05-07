package com.boonya.business.trip.mcp.client.config;

import com.boonya.business.trip.cache.redis.service.RedisChatMemory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class ChatClientConfig {

    @Value("${spring.ai.message.limit:120}")
    private Integer messageLimit;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ChatMemory chatMemory(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        return new RedisChatMemory(stringRedisTemplate, objectMapper, messageLimit);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, McpSyncClient mcpSyncClient) {
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        SyncMcpToolCallbackProvider mcpProvider = new SyncMcpToolCallbackProvider(mcpSyncClient);
        return builder
                .defaultAdvisors(memoryAdvisor)
                .defaultTools(mcpProvider)
                .build();
    }
}
