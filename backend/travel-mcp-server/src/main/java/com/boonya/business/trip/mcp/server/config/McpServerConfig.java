package com.boonya.business.trip.mcp.server.config;

import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("mcp-server")  // 使用 profile 区分启动模式
public class McpServerConfig {

    @Bean
    public McpServerProperties mcpServerProperties() {
        var props = new McpServerProperties();
        props.setName("business-trip-mcp-server");
        props.setVersion("1.0.0");
        props.setEnabled(true);
        return props;
    }
}
