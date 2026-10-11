package com.ricardi.mcpserver.tools;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Instant;
import java.util.List;

/**
 * Minimal "is it alive?" tool. Useful as the first thing to call when
 * testing with the MCP inspector.
 */
public final class HealthTool {

    private HealthTool() {
    }

    public static McpServerFeatures.SyncToolSpecification spec() {
        var tool = new McpSchema.Tool(
                "health_check",
                "Returns the service health status and current time. Takes no arguments.",
                """
                {"type":"object","properties":{}}
                """);
        return new McpServerFeatures.SyncToolSpecification(
                tool,
                (exchange, args) -> new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent("OK - " + Instant.now())),
                        false));
    }
}
