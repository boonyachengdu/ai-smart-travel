package com.boonya.business.trip.rag.controller;

import com.alibaba.fastjson.JSONObject;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
    private final StringRedisTemplate stringRedisTemplate;

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

        if (positive) {
            feedbackCollector.recordPositive(query, List.of(), sessionId);
        } else {
            feedbackCollector.recordNegative(query, List.of(), sessionId);
        }

        UserHolder user = UserHolder.get();
        if (user != null) {
            memoryService.recordSatisfaction(user.getUserId().toString(), positive ? 1.0 : 0.0);
        }

        return Response.ok("反馈已记录");
    }

    /**
     * 分页查询反馈记录（供后台管理页使用）
     */
    @PostMapping("/feedback/page")
    public Response<Map<String, Object>> feedbackPage(@RequestBody Map<String, Object> params) {
        int pageNum = params.containsKey("pageNum") ? ((Number) params.get("pageNum")).intValue() : 1;
        int pageSize = params.containsKey("pageSize") ? ((Number) params.get("pageSize")).intValue() : 10;

        Set<String> keys = stringRedisTemplate.keys("rag:feedback:*");
        if (keys == null) keys = Collections.emptySet();

        List<JSONObject> all = new ArrayList<>();
        for (String key : keys) {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                all.add(JSONObject.parseObject(json));
            }
        }

        all.sort((a, b) -> {
            String ta = a.getString("timestamp");
            String tb = b.getString("timestamp");
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });

        int total = all.size();
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<JSONObject> page = from < total ? all.subList(from, to) : Collections.emptyList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", page);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return Response.ok(result);
    }

    /**
     * 获取反馈统计
     */
    @GetMapping("/feedback/stats")
    public Response<Map<String, Object>> feedbackStats() {
        Set<String> keys = stringRedisTemplate.keys("rag:feedback:*");
        if (keys == null) keys = Collections.emptySet();

        long positive = 0;
        long negative = 0;
        for (String key : keys) {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                JSONObject fb = JSONObject.parseObject(json);
                if (fb.getBoolean("positive") == Boolean.TRUE) {
                    positive++;
                } else {
                    negative++;
                }
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", keys.size());
        stats.put("positive", positive);
        stats.put("negative", negative);
        stats.put("positiveRate", keys.isEmpty() ? 0 : (double) positive / keys.size());
        return Response.ok(stats);
    }
}
