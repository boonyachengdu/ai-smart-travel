package com.boonya.business.trip.cache.redis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RedisChatMemory implements ChatMemory {

    private static final String REDIS_KEY_PREFIX = "spring:ai:chat:memory:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxMessages;

    public RedisChatMemory(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @Value("${spring.ai.message.limit:120}") int maxMessages) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        String key = REDIS_KEY_PREFIX + conversationId;

        try {
            for (Message message : messages) {
                String json = objectMapper.writeValueAsString(message);
                stringRedisTemplate.opsForList().rightPush(key, json);
            }

            if (maxMessages > 0) {
                Long size = stringRedisTemplate.opsForList().size(key);
                if (size != null && size > maxMessages) {
                    stringRedisTemplate.opsForList().trim(key, 0, maxMessages - 1);
                }
            }
        } catch (JsonProcessingException e) {
            log.error("序列化消息失败", e);
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        if (lastN <= 0) {
            return List.of();
        }

        String key = REDIS_KEY_PREFIX + conversationId;
        List<String> jsonMessages = stringRedisTemplate.opsForList().range(key, -lastN, -1);

        if (jsonMessages == null || jsonMessages.isEmpty()) {
            return List.of();
        }

        return jsonMessages.stream()
                .map(this::deserializeMessage)
                .filter(m -> m != null)
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        String key = REDIS_KEY_PREFIX + conversationId;
        stringRedisTemplate.delete(key);
    }

    private Message deserializeMessage(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            String messageType = (String) map.get("messageType");
            if (messageType == null) {
                messageType = (String) map.get("type");
            }
            String content = (String) map.get("content");

            if (content == null) {
                return null;
            }

            return switch (messageType) {
                case "USER" -> new UserMessage(content);
                case "ASSISTANT" -> new AssistantMessage(content);
                case "SYSTEM" -> new SystemMessage(content);
                default -> new UserMessage(content);
            };
        } catch (Exception e) {
            log.error("反序列化消息失败: {}", json, e);
            return null;
        }
    }
}
