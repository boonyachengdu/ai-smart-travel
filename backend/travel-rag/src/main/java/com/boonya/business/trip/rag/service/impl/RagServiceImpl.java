package com.boonya.business.trip.rag.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.PolicyDocument;
import com.boonya.business.trip.common.models.rag.PolicyDocumentQueryRequest;
import com.boonya.business.trip.rag.search.HybridSearchService;
import com.boonya.business.trip.rag.search.QueryRewriter;
import com.boonya.business.trip.rag.search.RerankingService;
import com.boonya.business.trip.rag.search.SearchResult;
import com.boonya.business.trip.rag.service.EmbeddingService;
import com.boonya.business.trip.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final ChatClient chatClient;
    private final JdbcTemplate jdbcTemplate;
    private final QueryRewriter queryRewriter;
    private final HybridSearchService hybridSearchService;
    private final RerankingService rerankingService;

    // ==================== 增强检索管道核心方法 ====================

    @Override
    public List<SearchResult> searchWithEnhancedPipeline(String query, Long companyId, Long deptId, int topK) {
        UserHolder userHolder = UserHolder.get();
        if (userHolder == null) {
            throw new IllegalStateException("用户未登录");
        }
        if (!userHolder.isSuperAdmin() && !companyId.equals(userHolder.getCompanyId())) {
            throw new SecurityException("无权访问该企业的政策数据");
        }

        // Step 1: 查询改写
        List<String> rewrittenQueries = queryRewriter.rewrite(query);

        // Step 2: 对每个改写后的查询执行混合检索，收集所有结果
        String filterExpression = buildFilterExpression(companyId, deptId);
        Map<String, SearchResult> allResults = new LinkedHashMap<>();

        for (String rewrittenQuery : rewrittenQueries) {
            List<SearchResult> hybridResults = hybridSearchService.search(
                    rewrittenQuery, filterExpression, companyId, deptId);
            for (SearchResult sr : hybridResults) {
                String key = contentKey(sr.getContent());
                if (!allResults.containsKey(key) ||
                        sr.getFusedScore() > allResults.get(key).getFusedScore()) {
                    allResults.put(key, sr);
                }
            }
        }

        if (allResults.isEmpty()) {
            log.warn("增强检索未找到相关政策文档，query={}", query);
            return Collections.emptyList();
        }

        List<SearchResult> candidates = new ArrayList<>(allResults.values());

        // Step 3: 重排序
        candidates = rerankingService.rerank(query, candidates,
                Math.min(topK, candidates.size()));

        log.info("增强检索管道完成: query={}, 改写={}个, 候选={}条, 最终={}条",
                query, rewrittenQueries.size(), allResults.size(), candidates.size());

        return candidates;
    }

    // ==================== 接口实现 ====================

    @Override
    public String enhanceWithRag(String query, Long companyId) {
        return enhanceWithRag(query, companyId, null);
    }

    @Override
    public String enhanceWithRag(String query, Long companyId, Long deptId) {
        try {
            List<SearchResult> results = searchWithEnhancedPipeline(query, companyId, deptId, 3);
            if (results.isEmpty()) {
                return null;
            }
            return buildContextFromResults(results);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("增强检索失败", e);
            return fallbackVectorSearch(query, companyId);
        }
    }

    @Override
    public String generateAnswerWithRag(String query, Long companyId, Long deptId) {
        try {
            UserHolder userHolder = UserHolder.get();
            if (userHolder == null) {
                throw new IllegalStateException("用户未登录");
            }
            if (!userHolder.isSuperAdmin() && !companyId.equals(userHolder.getCompanyId())) {
                throw new SecurityException("无权访问该企业的政策数据");
            }

            log.info("开始增强RAG问答，companyId={}, deptId={}, query={}", companyId, deptId, query);

            List<SearchResult> results = searchWithEnhancedPipeline(query, companyId, deptId, 3);
            if (results.isEmpty()) {
                return "抱歉，未找到相关政策文档，无法回答该问题。";
            }

            String context = buildContextFromResults(results);
            String prompt = buildEnhancedPrompt(context, query);

            String answer = chatClient.prompt(prompt).call().content();
            log.info("增强RAG问答完成");
            return answer;

        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("增强RAG问答失败", e);
            return "抱歉，知识库查询暂时不可用。";
        }
    }

    @Override
    public String queryWithPolicyCheck(String question, Long companyId, Long deptId) {
        log.info("差标校验问答，companyId: {}, deptId: {}, question: {}", companyId, deptId, question);

        try {
            String deptPolicy = jdbcTemplate.queryForObject(
                    """
                    SELECT content FROM policy_documents
                    WHERE company_id = ? AND dept_id = ? AND enabled = TRUE
                    ORDER BY create_time DESC LIMIT 1
                    """,
                    String.class,
                    companyId,
                    deptId
            );

            String enhancedPrompt = buildEnhancedPrompt(
                    deptPolicy != null ? deptPolicy : "无特殊部门政策（使用公司通用政策）",
                    question
            );

            String answer = chatClient.prompt(enhancedPrompt).call().content();
            log.info("差标校验问答完成");
            return answer;

        } catch (Exception e) {
            log.error("差标校验问答失败，deptId: {}, question: {}", deptId, question, e);
            return generateAnswerWithRag(question + "\n（部门政策查询失败，使用通用政策）", companyId, deptId);
        }
    }

    @Override
    public String queryWithStandardCheck(String question, Long companyId, Long deptId) {
        try {
            log.info("开始差标校验问答，companyId={}, deptId={}, question={}", companyId, deptId, question);

            String deptPolicy = null;
            if (deptId != null) {
                deptPolicy = jdbcTemplate.queryForObject(
                        """
                        SELECT content FROM policy_documents
                        WHERE company_id = ? AND dept_id = ? AND enabled = TRUE
                        ORDER BY create_time DESC LIMIT 1
                        """,
                        String.class,
                        companyId,
                        deptId
                );
            }

            String fullQuestion;
            if (deptPolicy != null && !deptPolicy.isBlank()) {
                fullQuestion = String.format(
                        "【员工部门差标政策】%s\n\n【用户问题】%s\n\n请根据上述政策判断是否合规，并给出详细理由。",
                        deptPolicy,
                        question
                );
            } else {
                fullQuestion = String.format(
                        "【公司通用差标政策】无特殊部门政策\n\n【用户问题】%s\n\n请根据公司通用政策回答。",
                        question
                );
            }

            return generateAnswerWithRag(fullQuestion, companyId, deptId);

        } catch (Exception e) {
            log.error("差标校验问答失败", e);
            return generateAnswerWithRag(question + "\n（部门政策查询失败，使用通用政策）",
                    companyId, deptId);
        }
    }

    @Override
    public List<PolicyDocument> searchPolicies(PolicyDocumentQueryRequest request) {
        Long companyId = request.getCompanyId();
        Long deptId = request.getDeptId();
        String query = request.getQuery();

        log.info("增强搜索政策: {}", JSONObject.toJSONString(request));

        List<SearchResult> results = searchWithEnhancedPipeline(query, companyId, deptId, request.getLimit());
        return results.stream().map(this::toPolicyDocument).collect(Collectors.toList());
    }

    @Override
    public List<PolicyDocument> searchPolicies(String query, Long companyId, Long deptId, int limit) {
        List<SearchResult> results = searchWithEnhancedPipeline(query, companyId, deptId, limit);
        return results.stream().map(this::toPolicyDocument).collect(Collectors.toList());
    }

    @Override
    public boolean isCompliant(String orderType, Double amount, Long companyId) {
        try {
            String query = String.format("%s 价格 %.2f 元是否符合差旅标准", orderType, amount);

            List<SearchResult> results = searchWithEnhancedPipeline(query, companyId, null, 3);
            String context;
            if (results.isEmpty()) {
                context = "未找到相关政策文档，无法判断";
            } else {
                context = buildContextFromResults(results);
            }

            String prompt = String.format(
                    """
                    请判断以下差旅费用是否合规。

                    政策上下文：
                    %s

                    问题：%s

                    请仅回答'合规'或'不合规'，不要给出其他内容。""",
                    context,
                    String.format("%s 价格 %.2f 元是否符合差旅标准", orderType, amount)
            );

            String answer = chatClient.prompt(prompt).call().content();

            if (answer == null || answer.isBlank()) {
                log.warn("合规判断返回为空，默认认为不合规");
                return false;
            }

            boolean compliant = answer.contains("合规") && !answer.contains("不合规");
            log.info("合规判断结果：{} - {} {:.2f} - LLM回答：{}",
                    compliant ? "合规" : "不合规", orderType, amount, answer.trim());
            return compliant;

        } catch (Exception e) {
            log.error("合规检查失败", e);
            return false;
        }
    }

    // ==================== 私有方法 ====================

    private String buildContextFromResults(List<SearchResult> results) {
        StringBuilder context = new StringBuilder("根据企业差旅政策：\n\n");
        for (int i = 0; i < results.size(); i++) {
            context.append(String.format("[%d] %s\n", i + 1, results.get(i).getFormattedContent()));
            if (i < results.size() - 1) {
                context.append("\n---\n\n");
            }
        }
        return context.toString();
    }

    /**
     * 降级方案：当增强管道出错时，回退到纯向量检索
     */
    private String fallbackVectorSearch(String query, Long companyId) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(3)
                    .filterExpression("company_id == '" + companyId + "'")
                    .build();

            List<org.springframework.ai.document.Document> documents = vectorStore.similaritySearch(searchRequest);
            if (documents.isEmpty()) {
                return null;
            }

            StringBuilder context = new StringBuilder("根据企业差旅政策：\n\n");
            for (int i = 0; i < documents.size(); i++) {
                context.append(String.format("[%d] %s\n", i + 1, documents.get(i).getFormattedContent()));
                if (i < documents.size() - 1) {
                    context.append("\n---\n\n");
                }
            }
            return context.toString();
        } catch (Exception ex) {
            log.error("降级向量检索也失败", ex);
            return null;
        }
    }

    private PolicyDocument toPolicyDocument(SearchResult sr) {
        PolicyDocument doc = new PolicyDocument();
        Map<String, Object> metadata = sr.getMetadata();
        if (metadata != null) {
            if (metadata.containsKey("id")) {
                doc.setId(Long.valueOf(metadata.get("id").toString()));
            }
            if (metadata.containsKey("company_id")) {
                doc.setCompanyId(Long.valueOf(metadata.get("company_id").toString()));
            }
            if (metadata.get("dept_id") != null) {
                doc.setDeptId(Long.valueOf(metadata.get("dept_id").toString()));
            }
            if (metadata.containsKey("scene")) {
                doc.setScene(Scene.valueOf(metadata.get("scene").toString()));
            }
        }
        doc.setContent(sr.getFormattedContent());
        doc.setEnabled(true);
        return doc;
    }

    private String buildEnhancedPrompt(String context, String question) {
        return """
            你是一个专业的差旅政策顾问，请基于以下提供的公司差旅政策上下文，回答员工的问题。

            回答要求：
            1. 严格基于给定的政策上下文，不要编造信息
            2. 如果上下文中没有相关信息，请明确说明"根据现有政策文档，无法回答该问题"
            3. 回答要简洁、准确、友好
            4. 涉及金额时请明确货币单位
            5. 如果问题涉及政策例外情况，请提醒员工联系财务部门确认

            政策上下文：
            %s

            员工问题：
            %s

            你的回答：
            """.formatted(context, question);
    }

    private String buildFilterExpression(Long companyId, Long deptId) {
        UserHolder userHolder = UserHolder.get();
        if (userHolder == null) {
            throw new IllegalStateException("用户未登录");
        }
        if (companyId == null || companyId <= 0) {
            throw new IllegalArgumentException("无效的企业ID");
        }
        if (!userHolder.isSuperAdmin() && !companyId.equals(userHolder.getCompanyId())) {
            throw new SecurityException("无权访问该企业的政策数据");
        }

        StringBuilder filter = new StringBuilder();
        filter.append("company_id == '").append(companyId).append("'");
        if (deptId != null && deptId > 0 && !userHolder.isSuperAdmin()) {
            filter.append(" AND (dept_id == '").append(deptId).append("' OR dept_id IS NULL)");
        }
        return filter.toString();
    }

    private String contentKey(String content) {
        if (content == null || content.length() <= 100) {
            return content;
        }
        return content.substring(0, 100);
    }
}
