package com.boonya.business.trip.dialog;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

@Slf4j
@Import({ChatMemory.class})
@SpringBootTest
public class ChatByStreamTest {

    @Resource
    private ChatClient chatClient;

    @Test
    public void test() {
        String userInput = "帮我订一张明天北京到上海的机票";
        ResponseEntity responseEntity = chat(userInput);
        log.info(responseEntity.toString());
    }

    private ResponseEntity chat(String userInput) {
        // SSE 流式返回给前端
        Flux<String> stream = chatClient.prompt()
                .user(userInput)
                .stream()
                .content();

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(stream);
    }
}
