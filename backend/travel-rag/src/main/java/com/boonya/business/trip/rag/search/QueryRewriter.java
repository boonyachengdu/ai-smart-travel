package com.boonya.business.trip.rag.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询改写器 —— 将用户口语化/模糊的查询改写为更适合检索的查询列表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryRewriter {

    private final ChatClient chatClient;

    private static final String REWRITE_PROMPT =
            """
            你是差旅政策搜索专家。将用户的原始查询改写为3个适合向量检索的查询变体。

            改写规则：
            1. 消除口语化表达，保留核心政策术语（如"差标""报销标准""审批流程"）
            2. 从不同角度拆分复合问题（如"机票和酒店的标准"→ 拆为机票标准 + 酒店标准）
            3. 补充可能的同义词和相关术语
            4. 不添加原始查询中不存在的新概念

            原始查询：%s

            输出格式：每行一个查询变体，最多3行，不要编号。""";

    /**
     * 改写查询，生成多个变体用于提升召回率
     */
    public List<String> rewrite(String query) {
        if (query == null || query.isBlank() || query.length() < 4) {
            log.debug("查询过短，跳过改写: {}", query);
            return List.of(query);
        }

        try {
            String prompt = REWRITE_PROMPT.formatted(query);
            String result = chatClient.prompt(prompt).call().content();

            if (result == null || result.isBlank()) {
                log.warn("查询改写返回为空，使用原始查询");
                return List.of(query);
            }

            List<String> queries = new java.util.ArrayList<>();
            queries.add(query); // 原始查询始终保留

            for (String line : result.lines().toList()) {
                String trimmed = line.strip();
                if (!trimmed.isEmpty() && !queries.contains(trimmed)) {
                    queries.add(trimmed);
                }
            }

            log.info("查询改写: {} → {} 个变体", query, queries.size());
            return queries;
        } catch (Exception e) {
            log.error("查询改写失败，降级使用原始查询", e);
            return List.of(query);
        }
    }
}
