package com.ricardi.mcpserver.tools;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import org.springframework.web.client.RestClient;

/**
 * Lets an AI agent call one of your own internal HTTP endpoints and read
 * the response.
 *
 * <p>Plain English: your backend already has useful APIs. This tool hands
 * the AI a safe, read-only way to call them — e.g. "check the status page"
 * or "fetch the latest report" — instead of the AI making things up.</p>
 */
public final class InternalApiTool {

    private InternalApiTool() {
    }

    public static McpServerFeatures.SyncToolSpecification spec(RestClient restClient, String baseUrl) {
        var tool = new McpSchema.Tool(
                "call_internal_api",
                "Calls a GET endpoint on the configured internal API and returns the response body. "
                        + "Only relative paths are accepted (e.g. /api/status).",
                """
                {"type":"object",
                 "properties":{"path":{"type":"string","description":"Relative path, must start with /"}},
                 "required":["path"]}
                """);
        return new McpServerFeatures.SyncToolSpecification(
                tool,
                (exchange, args) -> {
                    String path = String.valueOf(args.get("path"));
                    if (!path.startsWith("/")) {
                        return error("Refused: path must start with /");
                    }
                    try {
                        String body = restClient.get()
                                .uri(baseUrl + path)
                                .retrieve()
                                .body(String.class);
                        String out = body == null ? "(empty response)"
                                : (body.length() > 4000 ? body.substring(0, 4000) + "..." : body);
                        return ok(out);
                    } catch (Exception e) {
                        return error("Call failed: " + e.getMessage());
                    }
                });
    }

    private static McpSchema.CallToolResult ok(String text) {
        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), false);
    }

    private static McpSchema.CallToolResult error(String text) {
        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), true);
    }
}
