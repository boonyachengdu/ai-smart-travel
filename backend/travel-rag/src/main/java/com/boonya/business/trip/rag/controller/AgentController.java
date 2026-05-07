package com.boonya.business.trip.rag.controller;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.rag.agent.AgentMemoryService;
import com.boonya.business.trip.rag.agent.AgentResponse;
import com.boonya.business.trip.rag.agent.ReActAgent;
import com.boonya.business.trip.rag.evaluation.FeedbackCollector;
import com.boonya.business.trip.rag.search.SearchResult;
import com.boonya.business.trip.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent API —— ReAct循环+增强检索+反馈收集
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final ReActAgent reActAgent;
    private final RagService ragService;
    private final FeedbackCollector feedbackCollector;
    private final AgentMemoryService memoryService;

    /**
     * Agent问答（ReAct自主推理+工具调用）
     */
    @PostMapping("/ask")
    public Response<Map<String, Object>> ask(
            @RequestParam String question,
            @RequestParam Long companyId,
            @RequestParam(required = false) Long deptId) {

        UserHolder user = UserHolder.get();
        String userId = user != null ? user.getUserId().toString() : "anonymous";

        // 注入跨会话记忆上下文
        String memoryContext = memoryService.getMemoryContext(userId);
        String fullQuestion = memoryContext.isEmpty() ? question :
                memoryContext + "\n用户当前问题：" + question;

        AgentResponse agentResponse = reActAgent.execute(fullQuestion, companyId, deptId);

        // 记录主题到记忆
        memoryService.recordTopic(userId, question);
        memoryService.recordComplianceResult(userId, agentResponse.getAnswer());

        return Response.ok(Map.of(
                "answer", agentResponse.getAnswer(),
                "iterations", agentResponse.getTotalIterations(),
                "steps", agentResponse.getSteps(),
                "contextUsed", agentResponse.getContextUsed(),
                "forcedStop", agentResponse.isForcedStop()
        ));
    }

    /**
     * 增强检索（查询改写+混合检索+重排序）
     */
    @GetMapping("/search")
    public Response<List<SearchResult>> search(
            @RequestParam String query,
            @RequestParam Long companyId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(defaultValue = "5") int topK) {

        List<SearchResult> results = ragService.searchWithEnhancedPipeline(query, companyId, deptId, topK);
        return Response.ok(results);
    }

    /**
     * 记录用户反馈
     */
    @PostMapping("/feedback")
    public Response<String> feedback(
            @RequestParam String query,
            @RequestParam boolean positive,
            @RequestParam String sessionId) {

        feedbackCollector.recordPositive(query, List.of(), sessionId);

        UserHolder user = UserHolder.get();
        if (user != null) {
            memoryService.recordSatisfaction(user.getUserId().toString(), positive ? 1.0 : 0.0);
        }

        return Response.ok("反馈已记录");
    }
}
