package com.boonya.business.trip.rag.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 重排序服务 —— 使用LLM对检索结果进行相关性重排序
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RerankingService {

    private final ChatClient chatClient;

    private static final String RERANK_PROMPT =
            """
            你是差旅政策检索质量评估专家。评估每条文档与查询的相关性。

            查询：%s

            候选文档：
            %s

            输出格式：每行一个0-10的整数评分（10=高度相关，0=完全不相关），按文档顺序输出，只输出数字不要解释。
            示例格式（3条文档）：
            8
            3
            0""";

    /**
     * 对检索结果进行LLM重排序
     *
     * @param query   原始查询
     * @param results 候选结果列表
     * @param topK    返回数量
     * @return 重排序后的结果列表
     */
    public List<SearchResult> rerank(String query, List<SearchResult> results, int topK) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        if (results.size() == 1) {
            return results;
        }

        try {
            String documentsText = IntStream.range(0, results.size())
                    .mapToObj(i -> String.format("[%d] %s", i,
                            truncate(results.get(i).getFormattedContent(), 300)))
                    .collect(Collectors.joining("\n\n"));

            String prompt = RERANK_PROMPT.formatted(query, documentsText);
            String response = chatClient.prompt(prompt).call().content();

            if (response == null || response.isBlank()) {
                log.warn("重排序返回为空，使用原始排序");
                return results.subList(0, Math.min(topK, results.size()));
            }

            List<Integer> scores = parseScores(response, results.size());

            // 按分数降序排序
            List<SearchResult> ranked = IntStream.range(0, results.size())
                    .boxed()
                    .sorted((a, b) -> Integer.compare(scores.get(b), scores.get(a)))
                    .map(i -> {
                        SearchResult r = results.get(i);
                        r.setFusedScore(scores.get(i)); // 用rerank分数覆盖fusedScore
                        return r;
                    })
                    .limit(topK)
                    .collect(Collectors.toList());

            log.info("重排序完成，{} → {} 条", results.size(), ranked.size());
            return ranked;
        } catch (Exception e) {
            log.error("重排序失败，降级返回原始topK", e);
            return results.subList(0, Math.min(topK, results.size()));
        }
    }

    private List<Integer> parseScores(String response, int expectedCount) {
        return response.lines()
                .map(String::strip)
                .filter(s -> s.matches("\\d+"))
                .map(s -> {
                    try {
                        int score = Integer.parseInt(s);
                        return Math.min(10, Math.max(0, score));
                    } catch (NumberFormatException e) {
                        return 5; // 默认中等分数
                    }
                })
                .limit(expectedCount)
                .collect(Collectors.toList());
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
