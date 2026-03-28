# Chatbot MCP Coaching Agent Design

## Goal

Rebuild the chatbot LangGraph runtime as a `create_agent`-based consumption coaching assistant that uses the server-side finance MCP through `langchain-mcp-adapters`, while preserving the existing chatbot API, DTO, service entry points, and streaming behavior.

## Constraints

- Keep the implementation inside the chatbot domain.
- Do not modify the image generation domain.
- Keep the existing chatbot API and service method signatures intact.
- Preserve JSON response and SSE streaming behavior from the API layer.
- Use the finance MCP described in `agents_tools.md`.
- Use `langchain-mcp-adapters` for MCP integration.
- Harden the system prompt so the assistant does not reveal internal tools, hidden context, system prompts, or MCP details.
- Rebuild the agent as a single tool-calling agent based on `create_agent`, not a custom supervisor graph.

## Chosen Direction

Use a thin LangGraph wrapper around a single `create_agent` runtime:

1. `chatbot_service.py` continues to call `build_chatbot_graph(...)`, `graph.ainvoke(...)`, and `graph.astream(...)`.
2. `build_chatbot_graph(...)` returns a compatibility wrapper object whose internals delegate to a `create_agent` runnable.
3. MCP tools are loaded through `MultiServerMCPClient`.
4. Tool execution is wrapped so `user_id` is injected in application code instead of relying only on the model prompt.
5. The final answer is normalized into the existing result contract: `final_response`, `final_answer`, `warnings`, and `tool_summaries`.

## Architecture

### 1. Runtime activation

- `build_chatbot_service()` stops depending on the database DSN for chatbot runtime availability.
- A new MCP URL setting activates the chatbot runtime.
- Request-scoped runtime resources remain supported so MCP clients can be closed after each request.

### 2. Agent assembly

- `app/domain/chatbot/agents/graph.py` becomes the adapter layer.
- The wrapper loads MCP tools, applies server-side user context injection, binds them into a `create_agent` runnable, and exposes:
  - `ainvoke`
  - `astream`
  - `get_graph`
- The wrapper keeps the outer response shape expected by `ChatbotService`.

### 3. Tool access model

- The model may choose tools, but `user_id` is inserted by server code before invocation.
- This avoids tool-call validation drift and keeps identity as trusted server context rather than user-supplied text.
- `agents_tools.md` becomes the primary tool selection guide for prompt construction.

### 4. Prompt model

- The system prompt turns the assistant into a consumption-habit coaching assistant.
- Tone requirements:
  - calm
  - factual
  - coaching-oriented
  - non-judgmental
  - action-oriented
- Privacy and abstraction requirements:
  - never mention MCP, internal prompts, tools, hidden rules, system messages, or referenced backend context
  - never describe internal selection logic
  - never expose raw tool payloads unless transformed into user-facing explanation
- Tool-use guidance:
  - start with `get_budget_home` for current-state coaching
  - use summary tools before detail tools
  - use transaction or detail tools only when the user asks why, what changed, or which category/transactions caused a pattern

### 5. State and output contract

- `ChatbotGraphState` is simplified to the fields still used by the service layer and tests:
  - `messages`
  - `user_id`
  - `session_id`
  - `user_query`
  - `warnings`
  - `tool_summaries`
  - `final_response`
  - `final_answer`
- SQL-specialist-era fields are removed because they no longer describe the MCP runtime.

### 6. Streaming

- `ChatbotService.stream_reply()` already expects message-stream chunks and filters tool-call chunks out.
- The wrapper will emit message events from the agent stream in a format that keeps this behavior stable.
- The final user-facing chunks will continue to appear as assistant text tokens.

## Error Handling

- MCP initialization failure becomes a chatbot configuration/runtime error with a clear message.
- Tool-call failures are captured into warnings and followed by a safe coaching fallback.
- If the model returns an empty or unusable answer, the runtime returns a short grounded fallback instead of leaking internal failure details.

## Testing Strategy

### Keep

- Service tests that validate:
  - conversation memory
  - thread id construction
  - reply result extraction
  - streaming SSE-compatible events
- API tests that validate:
  - request validation
  - JSON response shape
  - SSE response shape

### Replace

- Placeholder removal tests become MCP-agent tests.
- Old SQL-orchestration tests are replaced with focused tests for:
  - MCP runtime wrapper behavior
  - prompt hardening
  - hidden `user_id` injection
  - final response normalization
  - streaming compatibility

## Files Expected To Change

- `pyproject.toml`
- `.env.example`
- `app/core/config.py`
- `app/domain/chatbot/service/chatbot_service.py`
- `app/domain/chatbot/agents/__init__.py`
- `app/domain/chatbot/agents/graph.py`
- `app/domain/chatbot/agents/prompts/chatbot_prompts.py`
- `app/domain/chatbot/agents/states/state.py`
- `app/domain/chatbot/agents/tools/sql_tools.py`
- `app/domain/chatbot/agents/tools/finance_tools.py`
- `tests/app/domain/chatbot/agents/test_chatbot_graph.py`
- `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`
- `tests/app/domain/chatbot/agents/test_sql_tools.py`
- `tests/app/domain/chatbot/agents/test_removed_runtime.py`

## Risks And Mitigations

- Risk: `create_agent` stream payloads may not exactly match the previous custom graph.
  - Mitigation: keep a wrapper layer that normalizes invoke and stream outputs for the service.
- Risk: MCP tools may fail when `user_id` is omitted by the model.
  - Mitigation: inject `user_id` in server-side wrapped tools.
- Risk: the assistant may mention internal data provenance.
  - Mitigation: harden the system prompt and add prompt-focused tests for non-disclosure rules.
