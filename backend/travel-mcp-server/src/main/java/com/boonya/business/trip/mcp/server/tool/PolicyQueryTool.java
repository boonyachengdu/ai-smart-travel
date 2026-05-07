package com.boonya.business.trip.mcp.server.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 差旅政策查询 MCP 工具 —— 供外部Agent调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyQueryTool {

    private final JdbcTemplate jdbcTemplate;

    @McpTool(name = "search-policy", description = "搜索企业差旅政策文档，支持关键词检索")
    public String searchPolicy(
            @McpToolParam(description = "搜索关键词") String query,
            @McpToolParam(description = "企业ID") Long companyId,
            @McpToolParam(description = "返回条数限制，默认5") Integer limit
    ) {
        int resultLimit = (limit != null && limit > 0) ? limit : 5;
        log.info("MCP政策搜索: query={}, companyId={}, limit={}", query, companyId, resultLimit);

        try {
            // 使用全文检索（如果有content_tsv列）或LIKE降级
            String sql;
            List<Map<String, Object>> rows;
            try {
                sql = """
                        SELECT content, ts_rank(content_tsv, plainto_tsquery('simple', ?)) AS rank
                        FROM policy_documents
                        WHERE enabled = TRUE AND company_id = ?
                          AND content_tsv @@ plainto_tsquery('simple', ?)
                        ORDER BY rank DESC LIMIT ?
                        """;
                rows = jdbcTemplate.queryForList(sql, query, companyId, query, resultLimit);
            } catch (Exception e) {
                log.warn("全文检索不可用，降级为LIKE搜索");
                sql = """
                        SELECT content FROM policy_documents
                        WHERE enabled = TRUE AND company_id = ?
                          AND content LIKE ?
                        LIMIT ?
                        """;
                rows = jdbcTemplate.queryForList(sql, companyId, "%" + query + "%", resultLimit);
            }

            if (rows.isEmpty()) {
                return "未找到相关政策文档";
            }

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < rows.size(); i++) {
                result.append("[").append(i + 1).append("] ")
                        .append(rows.get(i).get("content")).append("\n");
            }
            return result.toString();
        } catch (Exception e) {
            log.error("MCP政策搜索失败", e);
            return "搜索失败: " + e.getMessage();
        }
    }

    @McpTool(name = "query-dept-policy", description = "查询部门级别的专属差旅政策")
    public String queryDeptPolicy(
            @McpToolParam(description = "企业ID") Long companyId,
            @McpToolParam(description = "部门ID") Long deptId
    ) {
        log.info("MCP部门政策查询: companyId={}, deptId={}", companyId, deptId);

        try {
            String sql = """
                    SELECT content FROM policy_documents
                    WHERE company_id = ? AND dept_id = ? AND enabled = TRUE
                    ORDER BY create_time DESC LIMIT 1
                    """;
            String policy = jdbcTemplate.queryForObject(sql, String.class, companyId, deptId);
            return policy != null ? policy : "该部门无特殊差旅政策";
        } catch (Exception e) {
            log.error("MCP部门政策查询失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    @McpTool(name = "check-compliance", description = "检查差旅费用是否符合企业政策标准")
    public String checkCompliance(
            @McpToolParam(description = "订单类型: FLIGHT/HOTEL/TRAIN/CAR") String orderType,
            @McpToolParam(description = "金额") Double amount,
            @McpToolParam(description = "企业ID") Long companyId
    ) {
        log.info("MCP合规检查: type={}, amount={}, companyId={}", orderType, amount, companyId);

        try {
            // 查询政策中是否有该类型的金额限制
            String sql = """
                    SELECT content FROM policy_documents
                    WHERE enabled = TRUE AND company_id = ?
                      AND (content ILIKE ? OR metadata::text ILIKE ?)
                    ORDER BY create_time DESC LIMIT 3
                    """;
            String pattern = "%" + orderType + "%";
            List<String> policies = jdbcTemplate.queryForList(sql, String.class, companyId, pattern, pattern);

            if (policies.isEmpty()) {
                return "未找到" + orderType + "相关政策，无法判断合规性";
            }

            return "已找到" + policies.size() + "条相关政策，内容如下：\n" +
                    String.join("\n---\n", policies);
        } catch (Exception e) {
            log.error("MCP合规检查失败", e);
            return "检查失败: " + e.getMessage();
        }
    }
}
