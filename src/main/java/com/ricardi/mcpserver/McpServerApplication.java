package com.ricardi.mcpserver;

import com.ricardi.mcpserver.tools.DocumentSummarizerTool;
import com.ricardi.mcpserver.tools.HealthTool;
import com.ricardi.mcpserver.tools.InternalApiTool;
import com.ricardi.mcpserver.tools.PostgresQueryTool;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * MCP server entry point.
 *
 * <p>Plain English: this app doesn't serve web pages — it exposes <em>tools</em>
 * that AI assistants can call. An AI client (Claude Desktop, the MCP
 * inspector, an agent you build) launches this program and talks to it over
 * stdin/stdout using the Model Context Protocol. When the AI needs live data,
 * it calls one of your tools instead of guessing.</p>
 *
 * <p>NOTE on SDK versions: the registration API below matches
 * io.modelcontextprotocol.sdk:mcp ${mcp.version} (see pom.xml). If you bump
 * the version, check the SDK's README — tool/schema builder names move
 * between releases.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(McpProperties.class)
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean(destroyMethod = "close")
    public McpSyncServer mcpServer(DataSource dataSource,
                                    RestClient.Builder restClientBuilder,
                                    McpProperties props) {
        var transport = new StdioServerTransportProvider();
        return McpServer.sync(transport)
                .serverInfo("ricardi-mcp-server", "0.1.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(List.of(
                        HealthTool.spec(),
                        PostgresQueryTool.spec(dataSource),
                        DocumentSummarizerTool.spec(),
                        InternalApiTool.spec(restClientBuilder.build(), props.internalApiBaseUrl())))
                .build();
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * The MCP server runs on background threads; without this the JVM would
     * exit immediately after startup.
     */
    @Bean
    public ApplicationRunner keepAlive() {
        return args -> new CountDownLatch(1).await();
    }
}
