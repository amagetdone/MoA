# Chatbot QuerySmith DDL Digest Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Improve QuerySmith's first-draft SQL quality by injecting a compact DDL-derived schema digest and strengthening its prompt to prefer canonical, user-scoped join paths.

**Architecture:** Keep the simplified chatbot graph intact. Add a small schema-digest builder near the chatbot agent prompt/input layer, inject the digest into QuerySmith runtime input, and strengthen the QuerySmith prompt so it uses the digest to choose minimal safe table paths.

**Tech Stack:** Python, LangGraph, LangChain agents, pytest.

---

### Task 1: Lock the new prompt contract with tests

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`

**Step 1: Write the failing test**

Add assertions that `build_querysmith_prompt()` now mentions:

- a DDL-derived schema digest
- canonical join paths
- minimal table paths
- analyzed facts preference
- ownership anchors for user scoping

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -q`
Expected: FAIL because the current prompt does not include this guidance yet.

**Step 3: Write minimal implementation**

Update the QuerySmith prompt so these instructions are present and explicit.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -q`
Expected: PASS.

### Task 2: Lock QuerySmith input schema-digest behavior with tests

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Write the failing test**

Add a test for `_prepare_querysmith_input()` that verifies the generated human
message includes:

- a schema digest section
- key ownership tables
- key analyzed transaction tables
- existing retry context when present

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -k querysmith -q`
Expected: FAIL because the current input does not include a digest section.

**Step 3: Write minimal implementation**

Add a schema-digest builder and inject its output into QuerySmith input.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -k querysmith -q`
Expected: PASS.

### Task 3: Implement the DDL-based schema digest

**Files:**
- Modify: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`
- Modify: `app/domain/chatbot/agents/graph.py`
- Optionally modify: `app/domain/chatbot/agents/tools/sql_tools.py`

**Step 1: Write the failing test**

Use the tests from Tasks 1 and 2 as the failing contract.

**Step 2: Run test to verify it fails**

Run:

`uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py tests/app/domain/chatbot/agents/test_chatbot_graph.py -q`

Expected: FAIL.

**Step 3: Write minimal implementation**

Implement:

- a compact schema digest for the allowlisted MOA finance tables
- prompt language that tells QuerySmith how to use it
- QuerySmith input wiring that includes the digest on every drafting turn

Keep the digest concise and operational; do not paste raw DDL into prompts.

**Step 4: Run test to verify it passes**

Run:

`uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py tests/app/domain/chatbot/agents/test_chatbot_graph.py -q`

Expected: PASS.

### Task 4: Run focused regression verification

**Files:**
- Verify only

**Step 1: Run targeted chatbot tests**

Run:

`uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/service/test_chatbot_service.py -q`

Expected: PASS.

**Step 2: Run broader chatbot suite**

Run:

`uv run python -m pytest tests/app/domain/chatbot -q`

Expected: PASS.
