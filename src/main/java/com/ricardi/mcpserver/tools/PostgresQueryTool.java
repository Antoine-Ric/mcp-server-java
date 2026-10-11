package com.ricardi.mcpserver.tools;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;

/**
 * Lets an AI agent run read-only SQL against your Postgres database.
 *
 * <p>Plain English: instead of the AI guessing at your data or you
 * copy-pasting rows into chat, the agent can ask the database itself —
 * through a tool <em>you</em> control. The read-only guard is the important
 * part: never give an AI agent an open-ended write path to your database.</p>
 */
public final class PostgresQueryTool {

    private PostgresQueryTool() {
    }

    public static McpServerFeatures.SyncToolSpecification spec(DataSource dataSource) {
        var tool = new McpSchema.Tool(
                "query_expenses",
                "Runs a READ-ONLY SQL SELECT against the expenses database. "
                        + "Anything that is not a SELECT is refused. "
                        + "Example: SELECT * FROM expenses ORDER BY created_at DESC LIMIT 10",
                """
                {"type":"object",
                 "properties":{"sql":{"type":"string","description":"A SELECT query to run"}},
                 "required":["sql"]}
                """);
        return new McpServerFeatures.SyncToolSpecification(
                tool,
                (exchange, args) -> {
                    String sql = String.valueOf(args.get("sql"));
                    if (!sql.stripLeading().toUpperCase(Locale.ROOT).startsWith("SELECT")) {
                        return error("Refused: only SELECT queries are allowed.");
                    }
                    try (var conn = dataSource.getConnection();
                         var stmt = conn.createStatement();
                         var rs = stmt.executeQuery(sql)) {
                        return ok(render(rs));
                    } catch (Exception e) {
                        return error("Query failed: " + e.getMessage());
                    }
                });
    }

    private static String render(ResultSet rs) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= cols; i++) {
            if (i > 1) {
                sb.append(" | ");
            }
            sb.append(meta.getColumnLabel(i));
        }
        sb.append('\n');
        int rows = 0;
        while (rs.next() && rows < 50) {
            for (int i = 1; i <= cols; i++) {
                if (i > 1) {
                    sb.append(" | ");
                }
                sb.append(rs.getString(i));
            }
            sb.append('\n');
            rows++;
        }
        if (rows == 50) {
            sb.append("... (truncated at 50 rows)\n");
        }
        return sb.toString();
    }

    private static McpSchema.CallToolResult ok(String text) {
        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), false);
    }

    private static McpSchema.CallToolResult error(String text) {
        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), true);
    }
}
