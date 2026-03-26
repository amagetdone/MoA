# MOA Chatbot Agent Design

## Context

The current AI server exposes merchant inference and image generation through FastAPI. It already uses LangGraph in one domain and has Langfuse tracing support, but it does not yet provide:

- a conversational API
- PostgreSQL access
- multi-turn conversation memory
- supervisor-based multi-agent routing

MOA itself is a personal finance service. Based on the approved `에이전트설계.md` and the current backend API surface, the chatbot MVP should answer questions about:

- weekly budget status
- goal progress
- spending summaries
- category-level spending
- unusual recent spending

The chatbot must use PostgreSQL directly instead of calling the Spring backend REST API. Because of that, the highest-priority requirement is hard user isolation: one request must never be able to read another user's data.

## Goals

- Expose a FastAPI chatbot API for single-response and streaming-response conversations.
- Build the chatbot with `langgraph` and `langgraph-supervisor`.
- Support multi-turn conversations with LangGraph memory/checkpointing.
- Use the existing `.env` values `AI_API_KEY` and `OPENAI_BASE_URL` to construct the chat model through an OpenAI-compatible client.
- Query PostgreSQL directly through read-only, user-scoped tools.
- Attach Langfuse tracing to the LangGraph and LLM execution path.
- Keep the first release read-only and focused on the 5 approved MVP tools.

## Non-Goals

- No write actions such as budget adjustment, plan edits, transfers, or transaction synchronization.
- No generic SQL execution API.
- No cross-user search or admin-style retrieval.
- No durable cross-restart memory in the first version.
- No attempt to proxy Spring authentication inside the AI service.

## Chosen Approach

Use a dedicated `chatbot` domain under `app/domain/chatbot/`, exposed through FastAPI, backed by:

- upgraded LangChain/LangGraph dependencies that are compatible with `langgraph-supervisor`
- `ChatOpenAI` from `langchain-openai` configured with `AI_API_KEY` and `OPENAI_BASE_URL`
- a compiled supervisor graph with a LangGraph in-memory checkpointer for multi-turn state
- PostgreSQL read-only repository methods that always require a trusted upstream `user_id`
- SSE streaming for progressive UI updates and token streaming
- Langfuse tracing at both the request/service layer and the LLM callback layer

This keeps the architecture close to the current FastAPI code style while adding a clean new domain for conversational behavior.

## Public API

### 1. JSON response endpoint

`POST /v1/chatbot/messages`

Request body:

```json
{
  "message": "이번 주 예산 괜찮아?",
  "conversation_id": "optional-client-conversation-id"
}
```

Required header from the trusted caller:

```text
X-User-Id: 123
```

Response shape:

```json
{
  "conversation_id": "generated-or-reused-id",
  "answer": "이번 주 자유예산은 12,000원 남아 있어요...",
  "warnings": [],
  "tool_summaries": [
    {
      "tool_name": "get_weekly_budget_status",
      "as_of": "2026-03-26T20:00:00+09:00"
    }
  ]
}
```

### 2. Streaming endpoint

`POST /v1/chatbot/messages/stream`

Request body and headers are the same as the JSON endpoint.

Response is `text/event-stream` with:

- `status` events for graph progress
- `tool` events for tool execution boundaries
- `token` events for streamed answer text
- `done` event with the final `conversation_id`
- `error` event for recoverable failure reporting

## Agent Topology

### Supervisor

The top-level supervisor decides which specialist to use based on the current turn and prior conversation state.

Responsibilities:

- inspect the user request
- choose one or more specialist agents
- merge their outputs
- produce the final Korean answer
- explicitly mention uncertainty or missing data

### Specialist agents

1. `budget_agent`
   - uses `get_weekly_budget_status`

2. `goal_agent`
   - uses `get_goal_progress_overview`

3. `spending_agent`
   - uses `get_spending_summary`
   - uses `get_spending_by_category`

4. `anomaly_agent`
   - uses `get_recent_spending_anomalies`

Each specialist returns structured evidence rather than user-facing prose only. The supervisor turns that evidence into the final answer.

## Multi-Turn Memory

Use LangGraph's in-memory checkpointer for the first version.

Key rule:

- the graph `thread_id` must be derived as `moa:{user_id}:{conversation_id}`

That means:

- two different users can never share the same memory thread
- one user can keep a stable conversation across turns by reusing `conversation_id`
- if no `conversation_id` is provided, the service generates one and returns it

This gives multi-turn continuity without allowing conversation collision across users.

Known limitation:

- in-memory checkpointing is process-local, so conversation history is not guaranteed across restarts or multiple app instances

That limitation is acceptable for the first release because the user explicitly requested LangGraph memory support, not long-term durable persistence.

## PostgreSQL Access Strategy

The chatbot will query PostgreSQL directly through a repository layer. It will not use ad-hoc SQL built from LLM output.

### Repository boundaries

All database access lives in `app/domain/chatbot/repository/`.

The repository exposes fixed methods only:

- `get_weekly_budget_status(user_id, base_date)`
- `get_goal_progress_overview(user_id, limit, status_filter)`
- `get_spending_summary(user_id, date_from, date_to)`
- `get_spending_by_category(user_id, date_from, date_to, include_comparison)`
- `get_recent_spending_anomalies(user_id, lookback_days, limit)`

### Guardrails

The user requested strong restrictions, so the design includes multiple layers:

1. Trusted user identity only
   - the service accepts `user_id` from the trusted upstream caller through `X-User-Id`
   - the LLM never chooses or overrides `user_id`

2. Fixed repository methods only
   - no raw SQL tool
   - no dynamic table names
   - no arbitrary `WHERE` fragments

3. Parameterized SQL only
   - every query uses bind parameters
   - every query enforces `users_id = :user_id` directly or through owner joins

4. Read-only transaction mode
   - each database request starts a read-only transaction
   - the connection is identified with an `application_name` such as `moa-ai-chatbot`

5. Select-only safety checks
   - repository helpers reject non-`SELECT` statements
   - multi-statement SQL is disallowed

6. Output shape constraints
   - tool outputs must include `summary`, `evidence`, and `warnings`
   - when data is insufficient, the tool returns warnings instead of forcing the model to infer

7. Prompt-level restrictions
   - system prompts instruct the agents to never speculate about another user's data
   - the assistant must say when information is missing or stale

## Tool Response Contract

Each tool returns a shape like:

```json
{
  "as_of": "2026-03-26T20:00:00+09:00",
  "window": {
    "date_from": "2026-03-01",
    "date_to": "2026-03-26"
  },
  "summary": {},
  "evidence": [],
  "warnings": []
}
```

This keeps the supervisor grounded in evidence instead of free-form hidden reasoning.

## LLM Construction

Use an OpenAI-compatible chat client backed by the existing environment variables:

- `AI_API_KEY`
- `OPENAI_BASE_URL`

The service should construct a `ChatOpenAI` instance with:

- explicit `api_key`
- explicit `base_url`
- streaming enabled for the streaming endpoint
- callbacks that connect to Langfuse

The exact chat model name should be configurable through a new setting such as `CHAT_MODEL`, with a safe default that can be overridden in `.env`.

## Langfuse Tracing

The service already has a `LangfuseTraceLogger`. Reuse that and add LangChain/LangGraph callbacks so execution is visible at both levels.

### Tracing layers

1. Request/service span
   - one parent span per chatbot request
   - includes `conversation_id`, route type, and selected thread id metadata

2. Agent/LLM callbacks
   - supervisor calls
   - specialist agent calls
   - tool execution boundaries
   - token generation when available

### Sensitive data handling

Tracing must preserve the current sanitization behavior and extend it for chatbot usage:

- do not log raw SQL text with bound values expanded
- do not log account numbers or card numbers
- avoid logging full evidence payloads when they contain sensitive merchant memo text
- allow high-level metadata such as tool name, row counts, date windows, and warning counts

## Error Handling

### API-level errors

- invalid or missing `X-User-Id` -> `422`
- missing chat model configuration -> `503`
- database connectivity failure -> `503`
- upstream LLM failure -> `502`

### Tool-level errors

If one specialist tool fails:

- emit a warning
- let the supervisor answer with the successful tools when possible
- mention that part of the requested information could not be retrieved

### Streaming errors

For the stream endpoint:

- emit an `error` SSE event
- close the stream cleanly

## File Layout

New domain layout:

```text
app/domain/chatbot/
├─ api/
│  ├─ controller/
│  │  └─ chatbot_controller.py
│  └─ dto/
│     └─ detail_dto.py
├─ agents/
│  ├─ graph.py
│  ├─ prompts/
│  │  └─ chatbot_prompts.py
│  ├─ states/
│  │  └─ state.py
│  └─ tools/
│     └─ finance_tools.py
├─ repository/
│  └─ chatbot_repository.py
└─ service/
   └─ chatbot_service.py
```

Supporting additions:

- `app/core/database.py`
- `app/api/dependencies.py` updates
- `app/main.py` router and lifespan updates
- test modules under `tests/app/domain/chatbot/` and `tests/app/`

## Dependency Changes

The current repository pins `langchain` and `langgraph` in an older range that does not support the requested `langgraph-supervisor` package. The chatbot feature therefore requires a coordinated dependency upgrade.

Expected additions/upgrades:

- `langchain` -> `1.x`
- `langgraph` -> `1.x`
- `langgraph-supervisor`
- `langchain-openai`
- `sqlalchemy`
- `psycopg[binary]`

This should be done once before chatbot implementation begins.

## Risks

1. Dependency upgrade risk
   - `langgraph-supervisor` requires newer `langgraph` versions than the project currently uses

2. Schema mismatch risk
   - table and column names inferred from the design doc may differ from the live database

3. In-memory memory limitation
   - conversations will not survive process restarts

4. Data sensitivity risk
   - direct DB access increases the need for strict logging and query controls

## Validation

The implementation is complete when the following pass:

- configuration tests for chatbot and database settings
- repository tests for all 5 MVP tools
- graph tests for supervisor routing and thread scoping
- service tests for standard and streaming execution
- API tests for both chatbot endpoints
- explicit tests proving user scoping and cross-user conversation isolation

## Rollout Notes

Ship the chatbot as a read-only MVP first. Once the retrieval path is reliable and observable, later iterations can add:

- durable checkpoint persistence
- richer tool coverage
- personalized coaching refinements
- cautious write flows behind separate confirmation protocols
