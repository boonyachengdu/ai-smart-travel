package com.boonya.business.trip.dialog;

import com.boonya.business.trip.dialog.context.DialogContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;

import java.util.Map;

@Slf4j
@Import({ResourceLoader.class})
@SpringBootTest
public class PromptTemplateTest {

    @Resource
    private ResourceLoader resourceLoader;
    @Test
    public void  test(){
        String userInput = "帮我订一张明天北京到上海的机票";
        // 构建上
        DialogContext context = new DialogContext();
        // 使用 PromptTemplate
        PromptTemplate template = new PromptTemplate(
                resourceLoader.getResource("classpath:prompts/intent-recognition.st")
        );
        Map<String, Object> model = Map.of(
                "stage", context.getCurrentStage(),
                "params", context.getCollectedParams(),
                "input", userInput
        );
        Prompt prompt = template.create(model);
        String promptText = prompt.getContents();
        log.info(promptText);
    }
}
