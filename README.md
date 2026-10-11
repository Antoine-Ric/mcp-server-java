# MCP Server (Java)

A **Model Context Protocol (MCP) server** written in Java with Spring Boot.
It exposes four tools an AI assistant can call: health check, read-only
Postgres queries, document summarization (stub), and internal HTTP API calls.

## What is MCP? (plain English)

AI assistants are smart but blind — they can't see your database, your docs,
or your internal APIs. The **Model Context Protocol** is an open standard
(think "USB-C for AI") that lets any AI app plug into tools *you* build.
You write a small server that says "here are my tools and what they do";
the AI discovers them and calls them when it needs live data instead of
guessing.

Concretely: with this server running, an agent can answer "what were my
biggest expenses last month?" by calling your `query_expenses` tool — no
copy-pasting rows into chat, no hallucinating numbers.

## Why this matters for backend devs (career-wise)

- **AI fluency, the builder kind** — using Copilot makes you an AI *user*;
  shipping MCP tools makes you an AI *builder*. That's the differentiator
  $150K+ postings are starting to ask for ("agentic AI", "AI enablement").
- **It compounds your backend skills** — MCP servers are just APIs with a
  different client. Your Spring Boot, Postgres, and HTTP knowledge transfers
  directly.
- **Interview story** — "I built the tool layer our agents use to query
  production data safely" is a senior-level narrative.

## The four tools

| Tool | What it does | Safety note |
|---|---|---|
| `health_check` | Returns OK + timestamp | — |
| `query_expenses` | Runs SQL against Postgres | **Read-only guard:** refuses anything that isn't `SELECT` |
| `summarize_document` | Summarizes text (stub) | Wire an LLM API for real summaries |
| `call_internal_api` | GETs your internal endpoints | Relative paths only; responses truncated at 4k chars |

## How to run locally

Requirements: Java 21, Maven 3.9+, Docker, Node.js (for the inspector).

```bash
# 1. Start Postgres
docker compose up -d

# 2. Create a demo table (optional)
docker exec -it mcp-postgres psql -U mcpdemo -c \
  "CREATE TABLE IF NOT EXISTS expenses(id SERIAL PRIMARY KEY, description TEXT, amount NUMERIC(12,2), category TEXT, created_at TIMESTAMPTZ DEFAULT now());"

# 3. Build and run
mvn package -DskipTests
java -jar target/mcp-server-java-0.1.0-SNAPSHOT.jar
```

The server speaks MCP over **stdin/stdout** — you won't see a web page.
That's expected.

## Test with the MCP inspector

The inspector is a visual playground for MCP servers:

```bash
npx @modelcontextprotocol/inspector \
  java -jar target/mcp-server-java-0.1.0-SNAPSHOT.jar
```

Then in the inspector UI: connect → "List Tools" → try `health_check`,
then `query_expenses` with `{"sql":"SELECT * FROM expenses LIMIT 5"}`.

## Deploy notes (ECS/Fargate)

MCP over stdio is for local/dev and Claude Desktop. For production you want
**streamable HTTP transport** instead:

1. Swap `StdioServerTransportProvider` for the SDK's HTTP transport
   (check the SDK README for the current class name at your pinned version).
2. Expose it behind Spring MVC, add auth (API key / OAuth) — never expose
   an unauthenticated tool server.
3. Containerize (`mvn spring-boot:build-image` gives you a Paketo image),
   push to ECR, run on Fargate behind an ALB — same pattern as the
   expense-tracker `terraform/` skeleton.

## Project layout

```
src/main/java/com/ricardi/mcpserver/
├── McpServerApplication.java   # boots Spring, registers tools, stays alive
├── McpProperties.java           # typed config (mcp.*)
└── tools/
    ├── HealthTool.java
    ├── PostgresQueryTool.java      # read-only SQL, SELECT guard
    ├── DocumentSummarizerTool.java # stub → wire an LLM API
    └── InternalApiTool.java        # safe GET passthrough
```

## Stretch-goal checklist

- [ ] **pgvector retrieval tool** — swap the Postgres image to
      `pgvector/pgvector:pg16`, store document embeddings, add a
      `search_docs` tool that does similarity search. That's a real RAG
      primitive and a huge resume line.
- [ ] **Real summarizer** — call Bedrock/Anthropic/OpenAI from
      `DocumentSummarizerTool`; stream the tokens back.
- [ ] **HTTP transport + auth** — move off stdio so the server can run on
      Fargate behind an ALB with API-key auth.
- [ ] **Tool annotations** — mark read-only tools as such in their schemas;
      clients use this to decide when to ask the user for confirmation.
- [ ] **Evals** — write 10 "golden" questions and assert the agent picks the
      right tool with the right arguments. Evals are what separate demos
      from production AI features.
