package com.boonya.business.trip.mcp.client.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
public class McpAIService {

    private final ToolCallbackProvider toolCallbackProvider;
    private final ChatClient chatClient;

    // 普通响应
    public String askWithTools(String userQuestion) {
        return chatClient.prompt()
                .user(userQuestion)
                .tools(toolCallbackProvider.getToolCallbacks())
                .call()
                .content();
    }

    // 流式响应（新增）
    public Flux<String> askWithToolsStream(String userQuestion) {
        return chatClient.prompt()
                .user(userQuestion)
                .tools(toolCallbackProvider.getToolCallbacks())
                .stream()
                .content();
    }

    // 带系统提示的调用
    public String askWithSystemPrompt(String userQuestion, String systemPrompt) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userQuestion)
                .tools(toolCallbackProvider.getToolCallbacks())
                .call()
                .content();
    }

    // 带历史对话的调用
    public String askWithHistory(String userQuestion, List<Message> history) {
        var prompt = chatClient.prompt();
        history.forEach(prompt::messages);
        return prompt.user(userQuestion)
                .tools(toolCallbackProvider.getToolCallbacks())
                .call()
                .content();
    }
}