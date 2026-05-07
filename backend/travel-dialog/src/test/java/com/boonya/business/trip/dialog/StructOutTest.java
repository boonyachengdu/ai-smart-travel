package com.boonya.business.trip.dialog;

import com.boonya.business.trip.common.models.dialog.constant.IntentType;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.junit.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import({ChatClient.class})
@SpringBootTest
public class StructOutTest {

    @Resource
    private ChatClient chatClient;

    // 定义输出类
    public record IntentResult(IntentType intent, double confidence, String reason) {}

    @Test
    public void test(){
        String userInput = "帮我订一张明天北京到上海的机票";
        // 自动映射
        IntentResult result = chatClient.prompt()
                .user(userInput)
                .call()
                .entity(IntentResult.class);  // Spring AI 自动解析
    }

}
