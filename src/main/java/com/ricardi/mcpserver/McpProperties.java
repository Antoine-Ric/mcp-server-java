package com.ricardi.mcpserver;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Simple typed config for the MCP server.
 */
@ConfigurationProperties(prefix = "mcp")
public record McpProperties(
        /**
         * Base URL used by the internal-api tool, e.g. http://localhost:8081.
         * Only used for demo calls to your own services.
         */
        String internalApiBaseUrl) {
}
