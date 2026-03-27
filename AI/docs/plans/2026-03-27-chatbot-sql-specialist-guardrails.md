# MOA Chatbot SQL Specialist Guardrails Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a SQL specialist subagent that can answer flexible read-only analytics questions while enforcing pre-execution SQL guardrails.

**Architecture:** Keep the current supervisor topology and add one `sql_specialist` subagent. Give that subagent a single guarded SQL execution tool whose call is intercepted by `wrap_tool_call` middleware, so SQL is validated before it reaches the request-scoped read-only database.

**Tech Stack:** FastAPI, pytest, LangChain agents middleware, LangGraph supervisor, SQLAlchemy

---

### Task 1: Lock graph and prompt expectations with failing tests

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`
- Create: `tests/app/domain/chatbot/agents/test_sql_tools.py`

**Step 1: Write the failing test**

Add tests that assert:

- `ChatbotToolBundle` includes `sql`
- graph routing can reach `sql_specialist`
- SQL specialist prompt requires read-only, user-scoped SQL

**Step 2: Run test to verify it fails**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/agents/test_sql_tools.py -v`
Expected: FAIL because the SQL specialist and SQL tools do not exist yet.

**Step 3: Write minimal implementation**

Add SQL specialist prompt text, graph wiring, and tool bundle shape.

**Step 4: Run test to verify it passes**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/agents/test_sql_tools.py -v`
Expected: PASS

### Task 2: Add guarded SQL tool with middleware interception

**Files:**
- Create: `app/domain/chatbot/agents/tools/sql_tools.py`
- Modify: `app/domain/chatbot/service/chatbot_service.py`
- Modify: `app/core/database.py`

**Step 1: Write the failing test**

Add tool-level tests that prove:

- read-only `SELECT` queries can execute
- non-whitelisted tables are rejected
- missing `:user_id` scope is rejected
- write keywords are rejected before execution

**Step 2: Run test to verify it fails**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/agents/test_sql_tools.py -v`
Expected: FAIL because the guarded tool and middleware do not exist yet.

**Step 3: Write minimal implementation**

Implement:

- chatbot SQL whitelist and guardrail validator
- guarded SQL execution runner
- `wrap_tool_call` middleware that validates and rewrites parameters before execution

**Step 4: Run test to verify it passes**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/agents/test_sql_tools.py -v`
Expected: PASS

### Task 3: Integrate SQL specialist into request-scoped chatbot runtime

**Files:**
- Modify: `tests/app/domain/chatbot/service/test_chatbot_service.py`
- Modify: `app/domain/chatbot/service/chatbot_service.py`

**Step 1: Write the failing test**

Add a service-level test that asserts the SQL tool bundle is built with the same request-scoped repository/runtime model as the other specialists.

**Step 2: Run test to verify it fails**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/service/test_chatbot_service.py -v`
Expected: FAIL because SQL tools are not wired into the runtime.

**Step 3: Write minimal implementation**

Thread the SQL tool bundle into `_build_tool_bundle()` and `build_chatbot_graph()`.

**Step 4: Run test to verify it passes**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/service/test_chatbot_service.py -v`
Expected: PASS

### Task 4: Run focused verification

**Files:**
- Verify only: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`
- Verify only: `tests/app/domain/chatbot/agents/test_sql_tools.py`
- Verify only: `tests/app/domain/chatbot/service/test_chatbot_service.py`
- Verify only: `tests/app/domain/chatbot/repository/test_database_guardrails.py`

**Step 1: Run verification**

Run:

```bash
.venv\Scripts\python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -v
.venv\Scripts\python -m pytest tests/app/domain/chatbot/agents/test_sql_tools.py -v
.venv\Scripts\python -m pytest tests/app/domain/chatbot/service/test_chatbot_service.py -v
.venv\Scripts\python -m pytest tests/app/domain/chatbot/repository/test_database_guardrails.py -v
```

Expected: PASS
