package com.boonya.business.trip.dialog;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import(ChatMemory.class)
@SpringBootTest
public class ChatClientTest {

    @Resource
    private ChatMemory chatMemory;

    @Bean
    @Primary
    public ChatClient primaryChatClient(ChatClient.Builder dashscopeBuilder) {
        return dashscopeBuilder.build();// 默认使用dashscope
    }

    @Bean
//    @Fallback
    public ChatClient fallbackChatClient(ChatClient.Builder openaiBuilder) {
        return openaiBuilder.build();// 备用使用openai
    }

    @Bean
    public ChatClient resilientChatClient(ChatClient.Builder builder) {
//        return builder
//                .defaultAdvisors(
//                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
//                        new RetryAdvisor(3),  // 自动重试3次
//                        new CircuitBreakerAdvisor(circuitBreaker)  // 熔断保护
//                )
//                .build();
        return null;
    }
}
