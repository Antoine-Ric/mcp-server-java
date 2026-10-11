package com.ricardi.mcpserver.tools;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;

/**
 * Stub document summarizer.
 *
 * <p>Right now it just extracts the first chunk of text. The real version
 * calls an LLM API (Bedrock, OpenAI, Anthropic) with a "summarize this"
 * prompt — that's the TODO below, and wiring it is the natural next step
 * toward the AI-fluency part of your roadmap.</p>
 */
public final class DocumentSummarizerTool {

    private static final int PREVIEW_CHARS = 500;

    private DocumentSummarizerTool() {
    }

    public static McpServerFeatures.SyncToolSpecification spec() {
        var tool = new McpSchema.Tool(
                "summarize_document",
                "Summarizes a document. Pass the full text; you get back a short summary. "
                        + "Current implementation returns a stub preview — wire an LLM API for real summaries.",
                """
                {"type":"object",
                 "properties":{"text":{"type":"string","description":"The document text to summarize"}},
                 "required":["text"]}
                """);
        return new McpServerFeatures.SyncToolSpecification(
                tool,
                (exchange, args) -> {
                    String text = String.valueOf(args.get("text"));
                    // TODO: replace with a real LLM call, e.g.:
                    //   bedrockClient.converse("Summarize in 3 bullet points:\n" + text)
                    String preview = text.length() <= PREVIEW_CHARS
                            ? text
                            : text.substring(0, PREVIEW_CHARS) + "...";
                    String result = "[STUB — wire an LLM API for real summaries]\n\n" + preview;
                    return new McpSchema.CallToolResult(
                            List.of(new McpSchema.TextContent(result)), false);
                });
    }
}
