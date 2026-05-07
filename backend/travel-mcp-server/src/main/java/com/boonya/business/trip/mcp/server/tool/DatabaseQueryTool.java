package com.boonya.business.trip.mcp.server.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseQueryTool {

    private final JdbcTemplate jdbcTemplate;

    // 1. @McpTool 直接用于方法，不再需要 @McpToolFunction
    @McpTool(name = "database-query", description = "根据 SQL 查询数据")
    public String query(@McpToolParam(description = "需要执行的 SQL 查询语句") String sql) {
        // 【警告】实际生产环境绝对不能这样直接拼接SQL，存在严重的SQL注入风险！
        // 这里仅作演示，建议对sql进行严格的校验和过滤。
        return jdbcTemplate.queryForList(sql).toString();
    }

    // 2. 另一个工具方法，用法相同
    @McpTool(name = "get-table-schema", description = "获取指定数据库表的结构信息")
    public List<String> getTableSchema(@McpToolParam(description = "数据库表名") String tableName) {
        // 同样，此方法也存在SQL注入风险，生产环境请使用参数化查询
        return jdbcTemplate.queryForList("DESC " + tableName, String.class);
    }
}