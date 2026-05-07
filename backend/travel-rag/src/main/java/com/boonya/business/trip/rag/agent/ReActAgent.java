package com.boonya.business.trip.rag.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.boonya.business.trip.rag.search.SearchResult;
import com.boonya.business.trip.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ReAct Agent —— Reasoning + Acting 循环
 *
 * 核心流程：
 *   Thought → Action → Observation → Thought → Action → ... → Final Answer
 *
 * Agent自主决定：
 *   1. 是否需要改写查询再搜
 *   2. 检索结果是否足够回答
 *   3. 是否需要查部门特殊政策
 *   4. 何时停止检索、生成最终答案
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReActAgent {

    private final ChatClient chatClient;
    private final RagService ragService;

    private static final int MAX_ITERATIONS = 5;
    private static final int MIN_CONFIDENCE = 7; // 0-10, 低于此值继续检索

    private static final String REACT_SYSTEM_PROMPT =
            """
            你是智能差旅政策Agent，使用ReAct模式回答用户问题。

            可用工具：
            - search_policy(query, companyId, deptId): 搜索政策知识库
            - query_dept_policy(companyId, deptId): 查询部门专属政策
            - check_compliance(orderType, amount, companyId): 合规校验
            - rewrite_query(query): 改写查询

            工作流程：
            1. Thought: 分析用户问题，决定下一步行动
            2. Action: 选择并调用工具
            3. Observation: 观察工具返回结果
            4. 重复1-3直到信息充足
            5. 生成Final Answer（含引用来源）

            输出严格的JSON格式，不要任何额外文字：
            {
              "thought": "分析当前状态和下一步推理",
              "action": "工具名或final_answer",
              "action_input": "工具参数或最终答案内容",
              "confidence": 0-10的整数（仅在action=final_answer时需要）
            }

            当信息充足可以直接回答时，action设为"final_answer"，action_input为完整答案。
            当需要更多信息时，action设为具体工具名，action_input为JSON参数。""";

    /**
     * 执行ReAct循环，回答用户问题
     */
    public AgentResponse execute(String userQuestion, Long companyId, Long deptId) {
        List<AgentStep> steps = new ArrayList<>();
        StringBuilder contextAccumulator = new StringBuilder();

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            log.info("ReAct iteration {}/{}", i + 1, MAX_ITERATIONS);

            // 1. REASONING: LLM决定下一步
            String prompt = buildReActPrompt(userQuestion, companyId, deptId, steps, contextAccumulator.toString());
            String llmResponse = chatClient.prompt(prompt).system(REACT_SYSTEM_PROMPT).call().content();

            AgentAction action = parseAction(llmResponse);
            if (action == null) {
                log.warn("无法解析Agent响应: {}", llmResponse);
                continue;
            }

            log.info("Thought: {}", action.getThought());
            log.info("Action: {} → {}", action.getAction(), action.getActionInput());

            // 2. 检查是否到达终点
            if ("final_answer".equals(action.getAction())) {
                AgentStep finalStep = AgentStep.builder()
                        .iteration(i + 1)
                        .thought(action.getThought())
                        .action("final_answer")
                        .actionInput(action.getActionInput())
                        .observation("答案已生成")
                        .timestamp(LocalDateTime.now())
                        .build();
                steps.add(finalStep);

                return AgentResponse.builder()
                        .answer(action.getActionInput())
                        .steps(steps)
                        .totalIterations(i + 1)
                        .contextUsed(contextAccumulator.toString())
                        .build();
            }

            // 3. ACTING: 执行工具
            String observation = executeTool(action, companyId, deptId);

            AgentStep step = AgentStep.builder()
                    .iteration(i + 1)
                    .thought(action.getThought())
                    .action(action.getAction())
                    .actionInput(action.getActionInput())
                    .observation(observation)
                    .timestamp(LocalDateTime.now())
                    .build();
            steps.add(step);

            // 4. OBSERVATION: 累积上下文
            if (observation != null && !observation.isBlank()) {
                contextAccumulator.append("\n[检索结果").append(i + 1).append("]: ")
                        .append(observation).append("\n");
            }
        }

        // 达到最大迭代次数，强制生成答案
        log.warn("达到最大迭代次数，强制生成答案");
        String forcedAnswer = generateForcedAnswer(userQuestion, contextAccumulator.toString(), companyId, deptId);

        return AgentResponse.builder()
                .answer(forcedAnswer)
                .steps(steps)
                .totalIterations(MAX_ITERATIONS)
                .contextUsed(contextAccumulator.toString())
                .forcedStop(true)
                .build();
    }

    private String buildReActPrompt(String question, Long companyId, Long deptId,
                                     List<AgentStep> previousSteps, String accumulatedContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户问题：").append(question).append("\n");
        sb.append("企业ID：").append(companyId).append("\n");
        if (deptId != null) {
            sb.append("部门ID：").append(deptId).append("\n");
        }

        if (!previousSteps.isEmpty()) {
            sb.append("\n已执行步骤：\n");
            for (AgentStep step : previousSteps) {
                sb.append("- Thought: ").append(step.getThought()).append("\n");
                sb.append("  Action: ").append(step.getAction()).append("\n");
                sb.append("  Observation: ")
                        .append(truncate(step.getObservation(), 200))
                        .append("\n");
            }
        }

        if (!accumulatedContext.isEmpty()) {
            sb.append("\n已检索到的政策信息：").append(accumulatedContext).append("\n");
        }

        sb.append("\n请决定下一步：thought + action + action_input（JSON格式）");
        return sb.toString();
    }

    private AgentAction parseAction(String llmResponse) {
        try {
            String json = extractJson(llmResponse);
            JSONObject obj = JSON.parseObject(json);
            return AgentAction.builder()
                    .thought(obj.getString("thought"))
                    .action(obj.getString("action"))
                    .actionInput(obj.getString("action_input"))
                    .confidence(obj.getInteger("confidence"))
                    .build();
        } catch (Exception e) {
            log.error("解析Agent响应失败: {}", llmResponse, e);
            return null;
        }
    }

    private String executeTool(AgentAction action, Long companyId, Long deptId) {
        try {
            return switch (action.getAction()) {
                case "search_policy" -> {
                    JSONObject params = JSON.parseObject(action.getActionInput());
                    String query = params.getString("query");
                    List<SearchResult> results = ragService.searchWithEnhancedPipeline(
                            query, companyId, deptId, 3);
                    yield results.isEmpty() ? "未找到相关政策" :
                            results.stream()
                                    .map(SearchResult::getFormattedContent)
                                    .collect(Collectors.joining("\n---\n"));
                }
                case "query_dept_policy" -> {
                    // 直接用 RAG 服务的方法
                    String result = ragService.queryWithPolicyCheck(
                            "请提供该部门的完整差旅政策", companyId, deptId);
                    yield result;
                }
                case "check_compliance" -> {
                    JSONObject params = JSON.parseObject(action.getActionInput());
                    boolean compliant = ragService.isCompliant(
                            params.getString("orderType"),
                            params.getDouble("amount"),
                            companyId);
                    yield compliant ? "合规" : "不合规";
                }
                case "rewrite_query" -> {
                    JSONObject params = JSON.parseObject(action.getActionInput());
                    String query = params.getString("query");
                    List<SearchResult> results = ragService.searchWithEnhancedPipeline(
                            query, companyId, deptId, 3);
                    yield results.isEmpty() ? "改写后未找到结果" :
                            results.stream()
                                    .map(SearchResult::getFormattedContent)
                                    .collect(Collectors.joining("\n---\n"));
                }
                default -> {
                    log.warn("未知工具: {}, 降级使用增强检索", action.getAction());
                    List<SearchResult> results = ragService.searchWithEnhancedPipeline(
                            action.getActionInput(), companyId, deptId, 3);
                    yield results.isEmpty() ? "未找到相关政策" :
                            results.stream()
                                    .map(SearchResult::getFormattedContent)
                                    .collect(Collectors.joining("\n---\n"));
                }
            };
        } catch (Exception e) {
            log.error("执行工具 {} 失败", action.getAction(), e);
            return "工具执行失败: " + e.getMessage();
        }
    }

    private String generateForcedAnswer(String question, String context, Long companyId, Long deptId) {
        if (context.isEmpty()) {
            return "抱歉，经过多次检索仍未能找到相关政策信息。建议您联系财务部门确认。";
        }
        return ragService.generateAnswerWithRag(question, companyId, deptId);
    }

    private String extractJson(String response) {
        if (response == null) return "{}";
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
