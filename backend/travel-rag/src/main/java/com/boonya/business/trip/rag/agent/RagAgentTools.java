package com.boonya.business.trip.rag.agent;

import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.rag.search.HybridSearchService;
import com.boonya.business.trip.rag.search.QueryRewriter;
import com.boonya.business.trip.rag.search.RerankingService;
import com.boonya.business.trip.rag.search.SearchResult;
import com.boonya.business.trip.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agent可用工具注册 —— 遵循Spring AI Function Calling模式
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RagAgentTools {

    private final RagService ragService;
    private final QueryRewriter queryRewriter;
    private final HybridSearchService hybridSearchService;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    @Description("搜索企业差旅政策知识库，返回相关政策文档内容。参数：query-搜索查询词；companyId-企业ID；deptId-部门ID(可选)")
    public Function<PolicySearchInput, String> searchPolicyKnowledge() {
        return input -> {
            log.info("Agent调用searchPolicyKnowledge: query={}, companyId={}, deptId={}",
                    input.query, input.companyId, input.deptId);
            try {
                List<SearchResult> results = ragService.searchWithEnhancedPipeline(
                        input.query, input.companyId, input.deptId, 5);
                if (results.isEmpty()) {
                    return "未找到相关政策文档";
                }
                return results.stream()
                        .map(r -> r.getFormattedContent())
                        .collect(Collectors.joining("\n---\n"));
            } catch (Exception e) {
                log.error("Agent政策搜索失败", e);
                return "搜索失败: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("精确查询部门级别的差旅政策。参数：companyId-企业ID；deptId-部门ID")
    public Function<DeptPolicyInput, String> queryDepartmentPolicy() {
        return input -> {
            log.info("Agent调用queryDepartmentPolicy: companyId={}, deptId={}", input.companyId, input.deptId);
            try {
                String policy = jdbcTemplate.queryForObject(
                        """
                        SELECT content FROM policy_documents
                        WHERE company_id = ? AND dept_id = ? AND enabled = TRUE
                        ORDER BY create_time DESC LIMIT 1
                        """,
                        String.class,
                        input.companyId,
                        input.deptId
                );
                return policy != null ? policy : "该部门无特殊差旅政策，使用公司通用标准";
            } catch (Exception e) {
                log.error("部门政策查询失败", e);
                return "查询失败: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("检查差旅费用是否合规。参数：orderType-订单类型(FLIGHT/HOTEL/TRAIN/CAR)；amount-金额；companyId-企业ID")
    public Function<ComplianceCheckInput, String> checkCompliance() {
        return input -> {
            log.info("Agent调用checkCompliance: type={}, amount={}, companyId={}",
                    input.orderType, input.amount, input.companyId);
            boolean compliant = ragService.isCompliant(input.orderType, input.amount, input.companyId);
            return compliant ? "合规" : "不合规";
        };
    }

    @Bean
    @Description("改写用户查询，生成更适合检索的关键词。参数：query-原始查询")
    public Function<RewriteInput, List<String>> rewriteQuery() {
        return input -> {
            log.info("Agent调用rewriteQuery: {}", input.query);
            return queryRewriter.rewrite(input.query);
        };
    }

    // ---- Input DTOs ----

    public record PolicySearchInput(String query, Long companyId, Long deptId) {}
    public record DeptPolicyInput(Long companyId, Long deptId) {}
    public record ComplianceCheckInput(String orderType, Double amount, Long companyId) {}
    public record RewriteInput(String query) {}
}
