# Chatbot SQL Supervisor Orchestration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the chatbot's SQL path with explicit generator, validator, and supervisor-executor agents inside a custom StateGraph while preserving the public service contract.

**Architecture:** The chatbot graph will move from `create_supervisor(...)` to a custom `StateGraph`. The router decides whether a request uses a domain specialist or the SQL pipeline. SQL generation and validation happen in separate agent nodes, and only the supervisor executor node owns the guarded SQL tool.

**Tech Stack:** LangGraph `StateGraph`, LangChain `create_agent`, existing read-only database guardrails, pytest.

---

### Task 1: Extend chatbot state for explicit SQL orchestration

**Files:**
- Modify: `app/domain/chatbot/agents/states/state.py`
- Test: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Write the failing test**

Add assertions that `ChatbotGraphState` exposes SQL orchestration fields such as
`active_route`, `candidate_sql`, `validated_sql`, and `sql_result`.

**Step 2: Run test to verify it fails**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -v`
Expected: FAIL because the state schema does not yet include the new fields.

**Step 3: Write minimal implementation**

Extend `ChatbotGraphState` with the SQL orchestration keys required by the new graph.

**Step 4: Run test to verify it passes**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -v`
Expected: PASS for the state-schema assertions.

### Task 2: Add custom graph tests for SQL pipeline sequencing

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Write the failing test**

Add a graph test that expects SQL questions to visit:

- supervisor router
- SQL generator
- SQL validator
- supervisor SQL executor

and expects non-SQL questions to skip the SQL nodes entirely.

**Step 2: Run test to verify it fails**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -v`
Expected: FAIL because the graph is still built around `create_supervisor(...)`.

**Step 3: Write minimal implementation**

Replace the chatbot graph builder with a custom `StateGraph` and wire conditional
edges for SQL vs non-SQL routes.

**Step 4: Run test to verify it passes**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -v`
Expected: PASS.

### Task 3: Add validator/executor regression tests

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_sql_tools.py`
- Modify: `tests/app/domain/chatbot/service/test_chatbot_service.py`

**Step 1: Write the failing test**

Add or update tests to verify:

- validator rejection prevents SQL execution
- approved SQL is executed only through the supervisor executor node/tool
- service-level tool bundle still includes the guarded SQL tool for the executor

**Step 2: Run test to verify it fails**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/agents/test_sql_tools.py tests/app/domain/chatbot/service/test_chatbot_service.py -v`
Expected: FAIL until the new orchestration path is wired.

**Step 3: Write minimal implementation**

Keep the deterministic SQL middleware and reuse it from the supervisor executor node.

**Step 4: Run test to verify it passes**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/agents/test_sql_tools.py tests/app/domain/chatbot/service/test_chatbot_service.py -v`
Expected: PASS.

### Task 4: Preserve final answer behavior and streaming contract

**Files:**
- Modify: `app/domain/chatbot/agents/graph.py`
- Modify: `app/domain/chatbot/service/chatbot_service.py`
- Test: `tests/app/domain/chatbot/service/test_chatbot_service.py`

**Step 1: Write the failing test**

Add assertions that the final user-facing answer still comes from the supervisor node
and that the service contract still exposes `answer`, `warnings`, and `tool_summaries`.

**Step 2: Run test to verify it fails**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/service/test_chatbot_service.py -v`
Expected: FAIL if the new graph no longer produces the expected final answer shape.

**Step 3: Write minimal implementation**

Make the custom graph write `final_answer` and supervisor-named AI messages in the same
shape that the service already expects.

**Step 4: Run test to verify it passes**

Run: `.venv\Scripts\python -m pytest tests/app/domain/chatbot/service/test_chatbot_service.py -v`
Expected: PASS.

### Task 5: Run full targeted verification

**Files:**
- Verify only

**Step 1: Run the full verification command**

Run:

`.venv\Scripts\python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/agents/test_sql_tools.py tests/app/domain/chatbot/service/test_chatbot_service.py tests/app/domain/chatbot/repository/test_database_guardrails.py -v`

Expected: All tests pass.

**Step 2: Review warnings**

Note any remaining upstream deprecation warnings without masking real failures.
