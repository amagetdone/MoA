# MOA Chatbot Agent Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a FastAPI-exposed, multi-turn, streaming MOA chatbot that uses LangGraph supervisor agents, PostgreSQL read-only tools, strict user scoping, and Langfuse tracing.

**Architecture:** The chatbot will live in a new `app/domain/chatbot/` domain. A supervisor graph will route requests to user-scoped financial specialist agents backed by fixed read-only repository methods. FastAPI will expose JSON and SSE endpoints, and LangGraph memory will preserve multi-turn state using a `thread_id` namespaced by both `user_id` and `conversation_id`.

**Tech Stack:** FastAPI, pytest, LangChain 1.x, LangGraph 1.x, `langgraph-supervisor`, `langchain-openai`, SQLAlchemy, psycopg, Langfuse

---

### Task 1: Upgrade dependencies and lock chatbot configuration

**Files:**
- Modify: `pyproject.toml`
- Modify: `uv.lock`
- Modify: `app/core/config.py`
- Create: `tests/app/core/test_config_chatbot.py`

**Step 1: Write the failing test**

Create `tests/app/core/test_config_chatbot.py` with tests for:

```python
from app.core.config import Settings


def test_settings_load_chatbot_values_from_env() -> None:
    settings = Settings(
        _env_file=None,
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gateway.example.com/v1",
        CHAT_MODEL="gpt-4.1-mini",
        DB_URL="jdbc:postgresql://postgres:5432/amagetdone",
        DB_USERNAME="d101",
        DB_PASSWORD="1234",
    )

    assert settings.chat_model == "gpt-4.1-mini"
    assert settings.db_url == "jdbc:postgresql://postgres:5432/amagetdone"
    assert settings.database_dsn == "postgresql+psycopg://d101:1234@postgres:5432/amagetdone"
```

Add a second test for optional defaults such as:

```python
assert settings.chatbot_stream_status_events_enabled is True
assert settings.chatbot_default_goal_limit == 5
```

**Step 2: Run test to verify it fails**

Run: `pytest tests/app/core/test_config_chatbot.py -v`

Expected: FAIL because the settings fields and DSN helper do not exist yet.

**Step 3: Write minimal implementation**

- Add chatbot settings to `app/core/config.py`
- Add a helper that converts the JDBC `DB_URL` into a SQLAlchemy/psycopg DSN using `DB_USERNAME` and `DB_PASSWORD`
- Update `pyproject.toml` to add:

```toml
"langchain>=1.0.0,<2.0.0",
"langgraph>=1.0.2,<2.0.0",
"langgraph-supervisor>=0.0.31,<0.1.0",
"langchain-openai>=1.0.0,<2.0.0",
"sqlalchemy>=2.0.0,<3.0.0",
"psycopg[binary]>=3.2.0,<4.0.0",
```

- Refresh `uv.lock`

**Step 4: Run test to verify it passes**

Run: `pytest tests/app/core/test_config_chatbot.py -v`

Expected: PASS

**Step 5: Commit**

```bash
git add pyproject.toml uv.lock app/core/config.py tests/app/core/test_config_chatbot.py
git commit -m "feat: add chatbot configuration and dependencies"
```

### Task 2: Add PostgreSQL read-only infrastructure and request-scoped user guardrails

**Files:**
- Create: `app/core/database.py`
- Create: `tests/app/domain/chatbot/repository/test_database_guardrails.py`

**Step 1: Write the failing test**

Create `tests/app/domain/chatbot/repository/test_database_guardrails.py` with focused tests like:

```python
from app.core.database import build_thread_id, jdbc_to_sqlalchemy_dsn, validate_user_id


def test_build_thread_id_namespaces_conversation_by_user() -> None:
    assert build_thread_id(user_id=7, conversation_id="abc") == "moa:7:abc"


def test_validate_user_id_rejects_non_positive_values() -> None:
    try:
        validate_user_id(0)
    except ValueError as exc:
        assert "user_id" in str(exc)
    else:
        raise AssertionError("validate_user_id should reject zero")
```

Add one guardrail test for a select-only helper:

```python
from app.core.database import assert_select_only_sql


def test_assert_select_only_sql_rejects_non_select_statements() -> None:
    try:
        assert_select_only_sql("DELETE FROM plan")
    except ValueError as exc:
        assert "SELECT" in str(exc)
    else:
        raise AssertionError("non-select SQL must be rejected")
```

**Step 2: Run test to verify it fails**

Run: `pytest tests/app/domain/chatbot/repository/test_database_guardrails.py -v`

Expected: FAIL because the module and helpers do not exist.

**Step 3: Write minimal implementation**

Create `app/core/database.py` with:

- `jdbc_to_sqlalchemy_dsn(...)`
- `validate_user_id(...)`
- `build_thread_id(...)`
- `assert_select_only_sql(...)`
- a small database helper that creates a SQLAlchemy engine and opens read-only connections

The read-only helper should enforce:

```python
connection.execute(text("SET TRANSACTION READ ONLY"))
```

and expose only a method suitable for parameterized `SELECT` execution.

**Step 4: Run test to verify it passes**

Run: `pytest tests/app/domain/chatbot/repository/test_database_guardrails.py -v`

Expected: PASS

**Step 5: Commit**

```bash
git add app/core/database.py tests/app/domain/chatbot/repository/test_database_guardrails.py
git commit -m "feat: add chatbot database guardrails"
```

### Task 3: Lock the public FastAPI chatbot API contract

**Files:**
- Create: `app/domain/chatbot/api/dto/detail_dto.py`
- Create: `tests/app/test_chatbot_api.py`

**Step 1: Write the failing test**

Create `tests/app/test_chatbot_api.py` with fake service-driven tests:

```python
from fastapi.testclient import TestClient

from app.main import create_app


class _FakeChatbotService:
    async def reply(self, *, user_id: int, message: str, conversation_id: str | None):
        return {
            "conversation_id": conversation_id or "generated-id",
            "answer": "이번 주 자유예산은 12000원 남아 있어요.",
            "warnings": [],
            "tool_summaries": [],
        }
```

Add tests for:

- `POST /v1/chatbot/messages` returns `200`
- missing `X-User-Id` returns `422`
- blank `message` returns `422`
- generated `conversation_id` is returned when omitted

**Step 2: Run test to verify it fails**

Run: `pytest tests/app/test_chatbot_api.py -v`

Expected: FAIL because the route and DTOs do not exist.

**Step 3: Write minimal implementation**

Create DTOs:

```python
class ChatbotMessageRequest(BaseModel):
    message: str
    conversation_id: str | None = None
```

Create response DTOs that include:

- `conversation_id`
- `answer`
- `warnings`
- `tool_summaries`

Do not add the route yet beyond the minimum shape needed by the tests if dependency injection is still pending.

**Step 4: Run test to verify it passes**

Run: `pytest tests/app/test_chatbot_api.py -v`

Expected: PASS once the route contract and validation are wired.

**Step 5: Commit**

```bash
git add app/domain/chatbot/api/dto/detail_dto.py tests/app/test_chatbot_api.py
git commit -m "test: lock chatbot API contract"
```

### Task 4: Lock the SSE streaming contract

**Files:**
- Modify: `tests/app/test_chatbot_api.py`
- Create: `app/domain/chatbot/api/controller/chatbot_controller.py`

**Step 1: Write the failing test**

Extend `tests/app/test_chatbot_api.py` with a streaming fake service:

```python
class _FakeStreamingChatbotService:
    async def stream_reply(self, *, user_id: int, message: str, conversation_id: str | None):
        yield {"event": "status", "data": {"message": "routing"}}
        yield {"event": "token", "data": {"text": "이번 주 "}}
        yield {"event": "token", "data": {"text": "예산은 안정적이에요."}}
        yield {"event": "done", "data": {"conversation_id": conversation_id or "generated-id"}}
```

Add a test that calls `POST /v1/chatbot/messages/stream` and asserts:

- status code is `200`
- content type starts with `text/event-stream`
- response body contains `event: status`
- response body contains `event: token`
- response body contains `event: done`

**Step 2: Run test to verify it fails**

Run: `pytest tests/app/test_chatbot_api.py -v`

Expected: FAIL because the stream route is not implemented.

**Step 3: Write minimal implementation**

Create controller code that:

- reads `X-User-Id`
- validates input DTOs
- returns `StreamingResponse`
- serializes yielded events in SSE format

Keep the event serializer small and deterministic.

**Step 4: Run test to verify it passes**

Run: `pytest tests/app/test_chatbot_api.py -v`

Expected: PASS

**Step 5: Commit**

```bash
git add app/domain/chatbot/api/controller/chatbot_controller.py tests/app/test_chatbot_api.py
git commit -m "feat: add chatbot streaming API"
```

### Task 5: Implement budget and goal repository methods and tools

**Files:**
- Create: `app/domain/chatbot/repository/chatbot_repository.py`
- Create: `app/domain/chatbot/agents/tools/finance_tools.py`
- Create: `tests/app/domain/chatbot/repository/test_chatbot_repository.py`
- Create: `tests/app/domain/chatbot/tools/test_finance_tools.py`

**Step 1: Write the failing test**

Create repository tests using a fake SQL runner:

```python
def test_get_weekly_budget_status_returns_summary_evidence_and_warnings() -> None:
    repo = ChatbotRepository(sql_runner=_FakeRunner([...]))

    result = repo.get_weekly_budget_status(user_id=7, base_date=date(2026, 3, 26))

    assert result["summary"]["final_free_amount"] == 12000
    assert "warnings" in result
    assert "evidence" in result
```

Add a second test for:

```python
def test_get_goal_progress_overview_limits_results_to_requested_user() -> None:
    ...
    assert all(item["user_id"] == 7 for item in runner.seen_user_rows)
```

Then add tool tests like:

```python
def test_budget_tool_binds_user_id_from_context() -> None:
    tool = build_budget_tool(repository=repo, user_id=7)
    result = tool.invoke({"base_date": "2026-03-26"})
    assert repo.calls == [{"method": "get_weekly_budget_status", "user_id": 7}]
```

**Step 2: Run test to verify it fails**

Run:

```bash
pytest tests/app/domain/chatbot/repository/test_chatbot_repository.py -v
pytest tests/app/domain/chatbot/tools/test_finance_tools.py -v
```

Expected: FAIL because the repository and tools do not exist.

**Step 3: Write minimal implementation**

Implement:

- `get_weekly_budget_status`
- `get_goal_progress_overview`
- user-bound tool wrappers for those methods

Return the standard contract:

```python
{
    "as_of": "...",
    "window": {...},
    "summary": {...},
    "evidence": [...],
    "warnings": [],
}
```

Ensure SQL always binds `:user_id` and never accepts user id from tool input.

**Step 4: Run test to verify it passes**

Run:

```bash
pytest tests/app/domain/chatbot/repository/test_chatbot_repository.py -v
pytest tests/app/domain/chatbot/tools/test_finance_tools.py -v
```

Expected: PASS

**Step 5: Commit**

```bash
git add app/domain/chatbot/repository/chatbot_repository.py app/domain/chatbot/agents/tools/finance_tools.py tests/app/domain/chatbot/repository/test_chatbot_repository.py tests/app/domain/chatbot/tools/test_finance_tools.py
git commit -m "feat: add chatbot budget and goal tools"
```

### Task 6: Implement spending and anomaly repository methods and tools

**Files:**
- Modify: `app/domain/chatbot/repository/chatbot_repository.py`
- Modify: `app/domain/chatbot/agents/tools/finance_tools.py`
- Modify: `tests/app/domain/chatbot/repository/test_chatbot_repository.py`
- Modify: `tests/app/domain/chatbot/tools/test_finance_tools.py`

**Step 1: Write the failing test**

Add repository tests for:

- `get_spending_summary`
- `get_spending_by_category`
- `get_recent_spending_anomalies`

Example:

```python
def test_get_spending_by_category_returns_ranked_categories() -> None:
    repo = ChatbotRepository(sql_runner=_FakeRunner([...]))

    result = repo.get_spending_by_category(
        user_id=7,
        date_from=date(2026, 3, 1),
        date_to=date(2026, 3, 26),
        include_comparison=True,
    )

    assert result["summary"]["total_spent"] == 230000
    assert result["evidence"][0]["category_name"] == "식비"
```

Add tool tests proving each wrapper uses the bound `user_id`.

**Step 2: Run test to verify it fails**

Run:

```bash
pytest tests/app/domain/chatbot/repository/test_chatbot_repository.py -v
pytest tests/app/domain/chatbot/tools/test_finance_tools.py -v
```

Expected: FAIL because the methods do not exist yet.

**Step 3: Write minimal implementation**

Extend the repository and tools with the remaining 3 MVP methods. Keep outputs small, explicit, and grounded in:

- `summary`
- `evidence`
- `warnings`

Normalize missing analysis metadata into warnings rather than exceptions when possible.

**Step 4: Run test to verify it passes**

Run:

```bash
pytest tests/app/domain/chatbot/repository/test_chatbot_repository.py -v
pytest tests/app/domain/chatbot/tools/test_finance_tools.py -v
```

Expected: PASS

**Step 5: Commit**

```bash
git add app/domain/chatbot/repository/chatbot_repository.py app/domain/chatbot/agents/tools/finance_tools.py tests/app/domain/chatbot/repository/test_chatbot_repository.py tests/app/domain/chatbot/tools/test_finance_tools.py
git commit -m "feat: add chatbot spending and anomaly tools"
```

### Task 7: Build the supervisor graph with memory and Langfuse-aware chat model wiring

**Files:**
- Create: `app/domain/chatbot/agents/states/state.py`
- Create: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`
- Create: `app/domain/chatbot/agents/graph.py`
- Create: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Write the failing test**

Create `tests/app/domain/chatbot/agents/test_chatbot_graph.py` with tests like:

```python
def test_build_chatbot_graph_compiles_with_checkpointer() -> None:
    graph = build_chatbot_graph(
        model=_FakeModel(),
        tools=_FakeTools(),
    )
    assert graph is not None
```

Add a memory scoping test:

```python
def test_thread_id_is_user_scoped() -> None:
    assert build_thread_id(user_id=7, conversation_id="same") != build_thread_id(user_id=8, conversation_id="same")
```

Add a routing test where a spending-focused question triggers the spending specialist and a budget-focused question triggers the budget specialist using a fake model/router response.

**Step 2: Run test to verify it fails**

Run: `pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -v`

Expected: FAIL because the graph, prompts, and state do not exist.

**Step 3: Write minimal implementation**

Create:

- graph state with messages, warnings, tool summaries, and final answer fields
- supervisor prompt that forbids speculation and cross-user reasoning
- specialist prompts for budget, goals, spending, and anomalies
- graph builder using `create_supervisor(...)`
- compiled graph with `InMemorySaver()` as checkpointer

Construct the chat model with `ChatOpenAI(...)` using:

```python
ChatOpenAI(
    model=settings.chat_model,
    api_key=settings.ai_api_key,
    base_url=settings.openai_base_url,
    streaming=True,
    callbacks=[...],
)
```

Attach Langfuse-compatible callbacks in the model factory.

**Step 4: Run test to verify it passes**

Run: `pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -v`

Expected: PASS

**Step 5: Commit**

```bash
git add app/domain/chatbot/agents/states/state.py app/domain/chatbot/agents/prompts/chatbot_prompts.py app/domain/chatbot/agents/graph.py tests/app/domain/chatbot/agents/test_chatbot_graph.py
git commit -m "feat: add chatbot supervisor graph"
```

### Task 8: Implement chatbot service, wire FastAPI dependencies, and finish end-to-end verification

**Files:**
- Create: `app/domain/chatbot/service/chatbot_service.py`
- Modify: `app/api/dependencies.py`
- Modify: `app/main.py`
- Modify: `app/domain/chatbot/api/controller/chatbot_controller.py`
- Create: `tests/app/domain/chatbot/service/test_chatbot_service.py`
- Modify: `tests/app/test_chatbot_api.py`

**Step 1: Write the failing test**

Create service tests that:

- call `reply(...)` and assert a stable `conversation_id`
- call `stream_reply(...)` and assert the event sequence
- prove `conversation_id=None` generates a new id
- prove the graph is invoked with `thread_id="moa:{user_id}:{conversation_id}"`

Example:

```python
def test_service_invokes_graph_with_user_scoped_thread_id() -> None:
    graph = _FakeGraph()
    service = ChatbotService(graph=graph, tracer=_FakeTracer())

    result = anyio.run(
        lambda: service.reply(
            user_id=7,
            message="이번 주 예산 괜찮아?",
            conversation_id="conv-1",
        )
    )

    assert graph.configs == [{"configurable": {"thread_id": "moa:7:conv-1"}}]
    assert result["conversation_id"] == "conv-1"
```

Then extend API tests to use the real dependency wiring through `create_app(...)`.

**Step 2: Run test to verify it fails**

Run:

```bash
pytest tests/app/domain/chatbot/service/test_chatbot_service.py -v
pytest tests/app/test_chatbot_api.py -v
```

Expected: FAIL because the service and app wiring are incomplete.

**Step 3: Write minimal implementation**

Implement `ChatbotService` with:

- `reply(...)`
- `stream_reply(...)`
- graph invocation config using the user-scoped thread id
- top-level Langfuse spans

Then wire:

- app lifespan construction of `chatbot_service`
- `get_chatbot_service(...)` dependency
- `app.include_router(chatbot_router)`

**Step 4: Run test to verify it passes**

Run:

```bash
pytest tests/app/domain/chatbot/service/test_chatbot_service.py -v
pytest tests/app/test_chatbot_api.py -v
```

Expected: PASS

**Step 5: Commit**

```bash
git add app/domain/chatbot/service/chatbot_service.py app/api/dependencies.py app/main.py app/domain/chatbot/api/controller/chatbot_controller.py tests/app/domain/chatbot/service/test_chatbot_service.py tests/app/test_chatbot_api.py
git commit -m "feat: wire MOA chatbot service"
```

### Task 9: Run focused regression verification

**Files:**
- No code changes required unless regressions are found

**Step 1: Run the chatbot-focused suite**

Run:

```bash
pytest tests/app/core/test_config_chatbot.py -v
pytest tests/app/domain/chatbot -v
pytest tests/app/test_chatbot_api.py -v
```

Expected: PASS

**Step 2: Run existing app smoke tests affected by shared wiring**

Run:

```bash
pytest tests/app/test_inference_api.py -v
pytest tests/app/test_image_generation_api.py -v
pytest tests/app/domain/image_generation -v
```

Expected: PASS

**Step 3: Fix regressions minimally if needed**

If shared dependency or lifespan changes break existing domains, patch only the minimal compatibility surface.

**Step 4: Re-run all commands**

Expected: all PASS

**Step 5: Commit**

```bash
git add app/main.py app/api/dependencies.py tests/app
git commit -m "test: verify chatbot and shared API regressions"
```
