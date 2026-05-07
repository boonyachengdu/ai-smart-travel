package com.boonya.business.trip.dialog.controller;

import com.alibaba.dashscope.common.Message;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.models.dialog.ChatRequest;
import com.boonya.business.trip.common.models.dialog.DialogRequest;
import com.boonya.business.trip.common.models.dialog.OrderGenerationResponse;
import com.boonya.business.trip.dialog.service.DialogSessionService;
import com.boonya.business.trip.dialog.vo.ChatMessageVO;
import com.boonya.business.trip.feign.clients.PolicyDocumentFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/dialog")
@RequiredArgsConstructor
public class DialogController {

    private final ChatClient chatClient;
    private final DialogSessionService dialogSessionService;
    private final PolicyDocumentFeignClient policyDocumentFeignClient;

    /**
     * 基础聊天测试接口（通义千问直接调用）
     */
    @GetMapping("/chat/test")
    public Response<String> testChat(@RequestParam String q) {
        log.info("基础聊天请求: {}", q);
        try {
            String answer = chatClient.prompt(q).call().content();
            return Response.ok(answer);
        } catch (Exception e) {
            log.error("基础聊天失败", e);
            return Response.error("聊天服务暂时不可用：" + e.getMessage());
        }
    }

    /**
     * 流式聊天测试接口（推荐用于 H5 实时聊天）
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> testChatStream(@RequestParam String q) {
        log.info("流式聊天请求: {}", q);
        return chatClient.prompt(q)
                .stream()
                .content()
                .onErrorResume(e -> {
                    log.error("流式聊天失败", e);
                    return Flux.just("抱歉，聊天服务暂时不可用");
                });
    }

    /**
     * RAG 增强问答（核心接口）
     * 将用户问题通过 Feign 转发至 travel-rag 的 /policy/check 接口，
     * 利用向量检索增强后由 LLM 生成包含差标校验的完整回答。
     */
    @PostMapping("/chat/rag")
    public Response<String> askWithRag(@RequestBody ChatRequest request) {
        try {
            String question = request.getUserInput();
            if (question == null || question.trim().isEmpty()) {
                return Response.error("问题不能为空");
            }
            // 构建 RAG 请求，将用户问题和场景传入
            DialogRequest dialogRequest = new DialogRequest();
            dialogRequest.setQuestion(question.trim());
            dialogRequest.setScene(request.getScene());

            log.info("RAG 增强问答请求：question={}, scene={}", question, request.getScene());
            Response<String> response = policyDocumentFeignClient.policyCheck(dialogRequest);
            if (response == null || !response.isSuccess()) {
                String errMsg = response != null ? response.getMessage() : "RAG 服务无响应";
                log.warn("RAG 问答调用失败：{}", errMsg);
                return Response.error("知识库查询失败：" + errMsg);
            }
            return Response.ok(response.getData());
        } catch (Exception e) {
            log.error("RAG 问答失败", e);
            return Response.error("知识库查询失败：" + e.getMessage());
        }
    }

    /**
     * 聊天接口（支持多轮对话）
     */
    @PostMapping("/chat/session")
    public Response<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        if (request.getUserInput() == null || request.getUserInput().trim().isEmpty()) {
            return Response.error("用户输入不能为空");
        }
        UserHolder userHolder = UserHolder.get();
        if (Objects.isNull(userHolder)) {
            return Response.error("未获取到用户信息，请先登录!");
        }
        Map<String, Object> data = new HashMap<>();

        if (Objects.nonNull(request.getNewSession()) && request.getNewSession()) {
            // 处理首次对话
            OrderGenerationResponse result = dialogSessionService.chat(
                    request.getScene(),
                    true,
                    request.getUserInput()
            );
            data.put("response", result);
            data.put("newSession", true);
        } else {
            OrderGenerationResponse result = dialogSessionService.chat(
                    request.getScene(),
                    false,
                    request.getUserInput()
            );
            data.put("response", result);
            data.put("newSession", false);
        }

        return Response.ok(data);
    }

    /**
     * 获取会话历史消息（前端拉取历史对话）
     */
    @GetMapping("/history/{sessionId}")
    public Response<List<ChatMessageVO>> getHistory(@PathVariable("sessionId") String sessionId) {
        try {
            List<ChatMessageVO> history = dialogSessionService.getHistory(sessionId);
            return Response.ok(history);
        } catch (Exception e) {
            log.error("获取历史失败", e);
            return Response.error("获取对话历史失败");
        }
    }

    /**
     * 清除/结束会话
     */
    @DeleteMapping("/session/{sessionId}")
    public Response<String> clearSession(@PathVariable("sessionId") String sessionId) {
        try {
            dialogSessionService.clearSession(sessionId);
            return Response.ok("会话已清除");
        } catch (Exception e) {
            log.error("清除会话失败", e);
            return Response.error("清除会话失败");
        }
    }
}