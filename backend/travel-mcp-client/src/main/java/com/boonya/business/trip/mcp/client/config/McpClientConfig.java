package com.boonya.business.trip.mcp.client.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("mcp-client")
public class McpClientConfig {

    @Bean
    public McpSyncClient mcpSyncClient() {
        ServerParameters serverParams = ServerParameters.builder("java")
                .args("-jar", "path/to/mcp-server.jar")
                .build();

        StdioClientTransport transport = new StdioClientTransport(serverParams);
        return McpClient.sync(transport).build();
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(McpSyncClient mcpClient) {
        return new SyncMcpToolCallbackProvider(mcpClient);
    }
}
