# Chatbot SQL Approval Rate Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Improve SQL-team approval rate by turning QueryGuard and deterministic guardrail failures into structured repair feedback that QuerySmith can use to revise the previous SQL draft.

**Architecture:** Keep the current supervisor-based chatbot graph. Expand the QueryGuard report contract, store structured retry context in chatbot state, and run deterministic SQL validation during QueryGuard capture so local guardrail failures also re-enter the repair loop before execution.

**Tech Stack:** LangGraph `create_supervisor`, LangChain `create_agent`, Pydantic contracts, deterministic SQL guardrails, pytest.

---

### Task 1: Document the structured remediation contract in tests

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Write the failing test**

Add prompt assertions that require QueryGuard to return structured remediation keys and
that QuerySmith is instructed to revise previous SQL using explicit feedback.

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -q`
Expected: FAIL because the new remediation contract is not in the prompts yet.

**Step 3: Write minimal implementation**

Update the QuerySmith and QueryGuard prompts to describe the repair-oriented contract.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -q`
Expected: PASS.

### Task 2: Expand the query validation contract and chatbot state

**Files:**
- Modify: `app/domain/chatbot/agents/contracts.py`
- Modify: `app/domain/chatbot/agents/states/state.py`
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Write the failing test**

Add state-schema and contract assertions for:

- `sql_retry_context`
- `last_candidate_sql`
- `failed_checks`
- `required_predicates`
- `forbidden_patterns`
- `allowed_tables_hint`
- `rewrite_strategy`

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -q`
Expected: FAIL because the new fields do not exist yet.

**Step 3: Write minimal implementation**

Extend the Pydantic query-validation contract and chatbot graph state with the new
structured remediation fields.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -q`
Expected: PASS for the new schema assertions.

### Task 3: Make retries revision-oriented instead of generic

**Files:**
- Modify: `app/domain/chatbot/agents/graph.py`
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Write the failing test**

Add a graph test where:

- first SQL draft is rejected
- QuerySmith sees the previous SQL draft and structured remediation context
- second SQL draft is revised and accepted

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -k retry -q`
Expected: FAIL because QuerySmith currently only receives a flat retry string.

**Step 3: Write minimal implementation**

Update `_prepare_querysmith_input()` and related SQL-team state handling so retries
include:

- previous candidate SQL
- structured remediation fields
- explicit revise-in-place instructions

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -k retry -q`
Expected: PASS.

### Task 4: Turn deterministic guardrail failures into another SQL-team retry

**Files:**
- Modify: `app/domain/chatbot/agents/graph.py`
- Modify: `app/domain/chatbot/agents/tools/sql_tools.py`
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Write the failing test**

Add a graph test where QueryGuard approves SQL that still fails deterministic SQL
validation, and verify that:

- execution does not happen yet
- the failure becomes structured retry feedback
- QuerySmith gets another chance

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -k deterministic -q`
Expected: FAIL because local SQL guardrail failures are not fed back into the retry loop yet.

**Step 3: Write minimal implementation**

Reuse deterministic SQL validation during QueryGuard capture and convert failures into
the same structured remediation contract used for LLM-level rejections.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -k deterministic -q`
Expected: PASS.

### Task 5: Run targeted regression verification

**Files:**
- Verify only

**Step 1: Run focused chatbot tests**

Run:

`uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/agents/test_sql_tools.py tests/app/domain/chatbot/service/test_chatbot_service.py -q`

Expected: PASS.

**Step 2: Run broader chatbot suite**

Run:

`uv run python -m pytest tests/app/domain/chatbot -q`

Expected: PASS.
