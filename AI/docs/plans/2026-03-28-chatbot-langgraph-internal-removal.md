# Chatbot LangGraph Internal Removal Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove the chatbot LangGraph internals while keeping the chatbot service and API surface intact.

**Architecture:** Keep the chatbot domain file layout and exported symbols stable, but replace the agents package with compatibility placeholders that deliberately fail at runtime. Service and controller code continue to call through the same interfaces.

**Tech Stack:** Python, FastAPI, pytest, LangChain-compatible tool shims

---

### Task 1: Lock the desired removed-runtime behavior with failing tests

**Files:**
- Create: `tests/app/domain/chatbot/agents/test_removed_runtime.py`

**Step 1: Write the failing tests**

Add tests that assert:
- `build_chatbot_graph()` returns a runtime placeholder whose `ainvoke` raises a removed-runtime error.
- Prompt builders return placeholder text.
- SQL and finance tool builders produce placeholders that fail when invoked.

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_removed_runtime.py -q`
Expected: FAIL because the current agent implementation still provides real behavior.

### Task 2: Replace chatbot agent internals with compatibility placeholders

**Files:**
- Modify: `app/domain/chatbot/agents/__init__.py`
- Modify: `app/domain/chatbot/agents/graph.py`
- Modify: `app/domain/chatbot/agents/contracts.py`
- Modify: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`
- Modify: `app/domain/chatbot/agents/states/state.py`
- Modify: `app/domain/chatbot/agents/tools/__init__.py`
- Modify: `app/domain/chatbot/agents/tools/sql_tools.py`
- Modify: `app/domain/chatbot/agents/tools/finance_tools.py`

**Step 1: Add a shared removed-runtime error**

Define a dedicated exception used across the chatbot agents package.

**Step 2: Replace graph internals**

Keep `ChatbotToolBundle`, `_normalize_tool_call_ids_in_messages`, and `build_chatbot_graph`, but remove the real graph implementation and return a placeholder runtime.

**Step 3: Replace prompt, contract, state, and tool internals**

Keep exported names stable, but replace behavior with placeholders or simplified definitions that no longer depend on LangGraph internals.

**Step 4: Run focused tests**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_removed_runtime.py -q`
Expected: PASS

### Task 3: Verify the intended breakage and preserved outer layers

**Files:**
- No file changes required

**Step 1: Run agent test suite**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents -q`
Expected: FAIL because previous agent behavior has been removed.

**Step 2: Run service and API surface tests**

Run: `uv run python -m pytest tests/app/domain/chatbot/service/test_chatbot_service.py tests/app/test_chatbot_api.py -q`
Expected: PASS for tests that rely on fake services or fake graph factories.

### Task 4: Document the result in Notion

**Files:**
- No repository file changes required

**Step 1: Capture the work summary**

Use the Notion MCP workflow required by `AGENTS.md` to record:
- problem situation
- cause
- multiple approaches
- chosen solution
- result
