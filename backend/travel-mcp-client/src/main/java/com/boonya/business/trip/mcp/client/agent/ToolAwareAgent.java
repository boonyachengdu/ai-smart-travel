package com.boonya.business.trip.mcp.client.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ToolAwareAgent {

    // 1. 注入 ToolCallbackProvider，而不是直接的 DatabaseQueryTool
    private final ToolCallbackProvider toolCallbackProvider;
    private final ChatClient chatClient;

    public String execute(String instruction) {
        return chatClient.prompt()
                .user(instruction)
                // 2. 使用 toolCallbacks() 方法，传入所有工具的回调
                .tools(toolCallbackProvider.getToolCallbacks())
                // 3. 如果使用了 ReReadingAdvisor，需要确认它是否兼容 2.0.0-M4
                // .advisors(new ReReadingAdvisor())  // 暂时注释，如果报错请移除
                .call()
                .content();
    }
}