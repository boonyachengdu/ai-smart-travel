package com.boonya.business.trip.rag.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务 —— 向量检索 + PostgreSQL全文检索，使用RRF（Reciprocal Rank Fusion）融合
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    private static final int VECTOR_TOP_K = 10;
    private static final int KEYWORD_TOP_K = 10;
    private static final int FUSION_TOP_K = 5;
    private static final double RRF_K = 60.0;

    /**
     * 执行混合检索
     *
     * @param query           查询文本
     * @param filterExpression pgvector过滤表达式
     * @param companyId        企业ID（用于全文检索过滤）
     * @param deptId           部门ID（可选）
     * @return 融合排序后的结果列表
     */
    public List<SearchResult> search(String query, String filterExpression, Long companyId, Long deptId) {
        // 1. 向量检索
        List<SearchResult> vectorResults = searchVector(query, filterExpression);
        log.debug("向量检索返回 {} 条", vectorResults.size());

        // 2. 全文检索
        List<SearchResult> keywordResults = searchKeyword(query, companyId, deptId);
        log.debug("全文检索返回 {} 条", keywordResults.size());

        // 3. RRF 融合
        List<SearchResult> fused = reciprocalRankFusion(vectorResults, keywordResults, FUSION_TOP_K);
        log.info("混合检索融合结果 {} 条", fused.size());

        return fused;
    }

    private List<SearchResult> searchVector(String query, String filterExpression) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(VECTOR_TOP_K)
                    .filterExpression(filterExpression)
                    .build();

            List<Document> docs = vectorStore.similaritySearch(request);

            List<SearchResult> results = new ArrayList<>();
            for (int i = 0; i < docs.size(); i++) {
                Document doc = docs.get(i);
                results.add(SearchResult.builder()
                        .content(doc.getFormattedContent())
                        .vectorScore(1.0 / (i + 1))
                        .metadata(new HashMap<>(doc.getMetadata()))
                        .build());
            }
            return results;
        } catch (Exception e) {
            log.error("向量检索失败", e);
            return Collections.emptyList();
        }
    }

    private List<SearchResult> searchKeyword(String query, Long companyId, Long deptId) {
        try {
            String sql;
            List<Object> params = new ArrayList<>();

            if (deptId != null && deptId > 0) {
                sql = """
                        SELECT content, metadata,
                               ts_rank(content_tsv, plainto_tsquery('simple', ?)) AS rank
                        FROM policy_documents
                        WHERE enabled = TRUE
                          AND company_id = ?
                          AND (dept_id = ? OR dept_id IS NULL)
                          AND content_tsv @@ plainto_tsquery('simple', ?)
                        ORDER BY rank DESC
                        LIMIT ?
                        """;
                params.add(query);
                params.add(companyId);
                params.add(deptId);
                params.add(query);
                params.add(KEYWORD_TOP_K);
            } else {
                sql = """
                        SELECT content, metadata,
                               ts_rank(content_tsv, plainto_tsquery('simple', ?)) AS rank
                        FROM policy_documents
                        WHERE enabled = TRUE
                          AND company_id = ?
                          AND content_tsv @@ plainto_tsquery('simple', ?)
                        ORDER BY rank DESC
                        LIMIT ?
                        """;
                params.add(query);
                params.add(companyId);
                params.add(query);
                params.add(KEYWORD_TOP_K);
            }

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

            List<SearchResult> results = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                Double rank = (Double) row.getOrDefault("rank", 0.0);
                String metadataStr = (String) row.get("metadata");

                Map<String, Object> metadata = new HashMap<>();
                if (metadataStr != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsed = com.alibaba.fastjson.JSONObject.parseObject(metadataStr, Map.class);
                        if (parsed != null) {
                            metadata.putAll(parsed);
                        }
                    } catch (Exception ignored) {
                    }
                }

                results.add(SearchResult.builder()
                        .content((String) row.get("content"))
                        .keywordScore(rank)
                        .metadata(metadata)
                        .build());
            }
            return results;
        } catch (Exception e) {
            log.warn("全文检索失败，可能是content_tsv列未创建: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Reciprocal Rank Fusion (RRF) - 将多个排序列表融合为单一排序
     */
    List<SearchResult> reciprocalRankFusion(List<SearchResult> vectorResults,
                                             List<SearchResult> keywordResults,
                                             int topK) {
        Map<String, SearchResult> contentMap = new LinkedHashMap<>();

        // 计算向量检索的 RRF 分数
        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResult sr = vectorResults.get(i);
            String key = contentKey(sr.getContent());
            double rrfScore = 1.0 / (RRF_K + i + 1);
            sr.setFusedScore(rrfScore);
            contentMap.put(key, sr);
        }

        // 累加全文检索的 RRF 分数
        for (int i = 0; i < keywordResults.size(); i++) {
            SearchResult kr = keywordResults.get(i);
            String key = contentKey(kr.getContent());
            double rrfScore = 1.0 / (RRF_K + i + 1);

            if (contentMap.containsKey(key)) {
                SearchResult existing = contentMap.get(key);
                existing.setFusedScore(existing.getFusedScore() + rrfScore);
                if (existing.getKeywordScore() == 0) {
                    existing.setKeywordScore(kr.getKeywordScore());
                }
            } else {
                kr.setFusedScore(rrfScore);
                contentMap.put(key, kr);
            }
        }

        // 按融合分数降序排序并截取 topK
        return contentMap.values().stream()
                .sorted(Comparator.comparingDouble(SearchResult::getFusedScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    private String contentKey(String content) {
        if (content == null || content.length() <= 100) {
            return content;
        }
        return content.substring(0, 100);
    }
}
