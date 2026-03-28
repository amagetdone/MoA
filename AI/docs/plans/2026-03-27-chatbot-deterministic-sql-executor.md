# Chatbot Deterministic SQL Executor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the fragile LLM-based SQL execution step with deterministic guarded execution so empty `sql_result` values cannot be treated as successful coaching inputs.

**Architecture:** Keep the simplified chatbot flow intact, but swap the `sql_executor` subgraph from model-driven tool use to a deterministic node that calls the guarded SQL tool directly. Add conditional routing so execution failure finalizes safely instead of continuing into coaching.

**Tech Stack:** Python, LangGraph, LangChain tools, pytest.

---

### Task 1: Lock fail-closed SQL execution behavior with tests

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Write the failing test**

Add a test that demonstrates:

- `approved_sql` exists
- SQL execution cannot produce `sql_result`
- `SpendWise Coach` is not called
- final result becomes a safe fallback instead of a completed coaching answer

Also update the happy-path test so it no longer expects the `sql_executor`
model to be invoked.

**Step 2: Run test to verify it fails**

Run:

`uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -k "full_coachops_flow or sql_execution" -q`

Expected: FAIL because the current graph still routes through the LLM executor
and lets empty execution pass.

**Step 3: Write minimal implementation**

Implement deterministic execution and fail-closed routing.

**Step 4: Run test to verify it passes**

Run the same command and confirm PASS.

### Task 2: Extract guarded SQL execution into a reusable helper

**Files:**
- Modify: `app/domain/chatbot/agents/tools/sql_tools.py`
- Modify: `tests/app/domain/chatbot/agents/test_sql_tools.py`

**Step 1: Write the failing test**

Add a focused test for a new helper that:

- validates SQL with existing guardrails
- injects `user_id`
- invokes the SQL tool directly

**Step 2: Run test to verify it fails**

Run:

`uv run python -m pytest tests/app/domain/chatbot/agents/test_sql_tools.py -k guarded_sql -q`

Expected: FAIL because the helper does not exist yet.

**Step 3: Write minimal implementation**

Add a helper that reuses the current validation and parameter sanitization path.

**Step 4: Run test to verify it passes**

Run the same command and confirm PASS.

### Task 3: Rewire the graph to deterministic SQL execution

**Files:**
- Modify: `app/domain/chatbot/agents/graph.py`
- Modify: `app/domain/chatbot/agents/states/state.py`

**Step 1: Use the failing tests from Tasks 1 and 2**

Do not add extra behavior beyond deterministic execution and fail-closed state.

**Step 2: Run the focused failing set**

Run:

`uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/agents/test_sql_tools.py -q`

Expected: FAIL before implementation.

**Step 3: Write minimal implementation**

Implement:

- deterministic SQL execution node
- `sql_execution_failed` state handling
- conditional routing after SQL execution
- finalization support for execution failure

**Step 4: Run the focused set again**

Run the same command and confirm PASS.

### Task 4: Run regression verification

**Files:**
- Verify only

**Step 1: Run targeted chatbot tests**

Run:

`uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/agents/test_sql_tools.py tests/app/domain/chatbot/service/test_chatbot_service.py -q`

Expected: PASS.

**Step 2: Run broader chatbot suite**

Run:

`uv run python -m pytest tests/app/domain/chatbot -q`

Expected: PASS.
