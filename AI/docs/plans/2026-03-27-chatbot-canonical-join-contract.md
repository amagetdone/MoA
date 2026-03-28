# Chatbot Canonical Join Contract Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Improve SQL approval rate for common spending-feedback requests by aligning QuerySmith and QueryGuard with the repository's trusted ownership join contract.

**Architecture:** Keep the simplified chatbot flow unchanged. Tighten the prompt contract by encoding the repository-proven join keys directly into the QuerySmith schema digest and QueryGuard instructions, then lock that behavior with focused prompt tests.

**Tech Stack:** Python, LangGraph, LangChain, pytest.

---

### Task 1: Lock the canonical join contract in prompt tests

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`

**Step 1: Write the failing test**

Add assertions that the prompt layer now explicitly includes:

- `account_transaction_override.id = transaction_override.id`
- `card_transaction_override.id = transaction_override.id`
- repository-aligned canonical join language for QuerySmith
- validator-side acceptance of canonical ownership joins for QueryGuard

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -q`

Expected: FAIL because the current prompts do not encode the exact bridge keys.

**Step 3: Write minimal implementation**

Update the prompt builders so the exact canonical join contract is present.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -q`

Expected: PASS.

### Task 2: Lock the schema digest details in QuerySmith runtime-input tests

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Write the failing test**

Extend the existing QuerySmith input test so the runtime digest must include:

- `account_transaction_override.id = transaction_override.id`
- `card_transaction_override.id = transaction_override.id`
- the analyzed-fact join `transaction_analysis_result.transaction_override_id = transaction_override.id`

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -k schema_digest -q`

Expected: FAIL because the current digest describes the bridges too vaguely.

**Step 3: Write minimal implementation**

Tighten the schema digest text in `chatbot_prompts.py` so the exact bridge
predicates are included.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -k schema_digest -q`

Expected: PASS.

### Task 3: Implement the canonical join contract in prompt builders

**Files:**
- Modify: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`

**Step 1: Use the failing tests from Tasks 1 and 2**

Do not add new behavior beyond satisfying the prompt-contract failures.

**Step 2: Run the focused failing set**

Run:

`uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py tests/app/domain/chatbot/agents/test_chatbot_graph.py -q`

Expected: FAIL before implementation.

**Step 3: Write minimal implementation**

Implement:

- exact canonical join keys in the schema digest
- clearer QuerySmith guidance to use those joins directly
- clearer QueryGuard guidance to treat those joins as verified when user scope is present

Do not change the deterministic `UNION` policy in this task.

**Step 4: Run the focused set again**

Run:

`uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py tests/app/domain/chatbot/agents/test_chatbot_graph.py -q`

Expected: PASS.

### Task 4: Run regression verification

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
