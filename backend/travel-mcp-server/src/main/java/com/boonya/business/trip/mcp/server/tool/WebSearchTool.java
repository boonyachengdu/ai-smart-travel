package com.boonya.business.trip.mcp.server.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSearchTool {

    private final RestTemplate restTemplate = new RestTemplate();

    @McpTool(
            name = "web-search",
            description = "执行网络搜索，返回搜索结果摘要"
    )
    public String search(
            @McpToolParam(description = "搜索关键词") String query,
            @McpToolParam(description = "返回结果数量，默认5条", required = false) Integer limit
    ) {
        int resultLimit = (limit != null && limit > 0) ? limit : 5;

        log.info("执行网络搜索: {}, 限制: {}", query, resultLimit);

        try {
            String url = String.format(
                    "https://api.example.com/search?q=%s&limit=%d",
                    query, resultLimit
            );

            String response = restTemplate.getForObject(url, String.class);
            return response != null ? response : "未找到相关结果";

        } catch (Exception e) {
            log.error("网络搜索失败", e);
            return "搜索失败: " + e.getMessage();
        }
    }

    @McpTool(
            name = "fetch-webpage",
            description = "获取指定URL的网页内容"
    )
    public String fetchWebpage(
            @McpToolParam(description = "网页URL地址") String url
    ) {
        log.info("获取网页内容: {}", url);

        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return "错误: URL必须以http://或https://开头";
            }

            String content = restTemplate.getForObject(url, String.class);
            return content != null ? content.substring(0, Math.min(content.length(), 5000)) : "内容为空";

        } catch (Exception e) {
            log.error("获取网页失败: {}", url, e);
            return "获取失败: " + e.getMessage();
        }
    }
}
