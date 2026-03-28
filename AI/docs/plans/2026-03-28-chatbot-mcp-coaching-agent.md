# Chatbot MCP Coaching Agent Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rebuild the chatbot runtime as a `create_agent`-based MCP consumption coaching assistant without changing the chatbot API or service surface.

**Architecture:** Keep `ChatbotService` as the stable outer shell and swap the removed `agents/` internals for a wrapper around a single LangChain `create_agent` runtime. The wrapper loads finance MCP tools through `langchain-mcp-adapters`, injects trusted `user_id` server-side, hardens the system prompt against internal-context leakage, and normalizes invoke/stream results back into the current chatbot response contract.

**Tech Stack:** Python, FastAPI, LangChain, LangGraph runtime, `langchain-mcp-adapters`, MCP, pytest

---

### Task 1: Add the new runtime dependencies and settings

**Files:**
- Modify: `pyproject.toml`
- Modify: `.env.example`
- Modify: `app/core/config.py`

**Step 1: Write the failing test**

Add or update tests so runtime configuration expects an MCP URL instead of the removed SQL-only runtime assumptions.

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/service/test_chatbot_service.py -q`
Expected: FAIL where configuration assumptions still depend on the removed runtime wiring.

**Step 3: Write minimal implementation**

- Add `langchain-mcp-adapters` and `mcp` dependencies.
- Add `MORE_FINANCE_MCP_URL` and any minimal timeout setting needed.
- Keep existing settings unchanged for other domains.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/service/test_chatbot_service.py -q`
Expected: PASS for updated configuration expectations.

### Task 2: Lock prompt and state expectations with tests

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Write the failing tests**

Add tests that assert:
- the system prompt describes a spending-habit coaching assistant
- the system prompt forbids revealing MCP, internal context, hidden prompts, tool names, or reasoning traces
- graph state only keeps the fields needed by the MCP agent wrapper

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py tests/app/domain/chatbot/agents/test_chatbot_graph.py -q`
Expected: FAIL because placeholders or old SQL-specialist assumptions still exist.

**Step 3: Write minimal implementation**

Update:
- `app/domain/chatbot/agents/prompts/chatbot_prompts.py`
- `app/domain/chatbot/agents/states/state.py`

Keep exports stable where practical, but replace obsolete SQL-team language with MCP-coaching instructions and simplified state.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py tests/app/domain/chatbot/agents/test_chatbot_graph.py -q`
Expected: PASS for prompt and state expectations.

### Task 3: Rebuild tool wrappers around MCP semantics

**Files:**
- Modify: `app/domain/chatbot/agents/tools/sql_tools.py`
- Modify: `app/domain/chatbot/agents/tools/finance_tools.py`
- Modify: `app/domain/chatbot/agents/tools/__init__.py`
- Modify: `tests/app/domain/chatbot/agents/test_sql_tools.py`

**Step 1: Write the failing tests**

Add tests that assert:
- MCP tool wrappers inject `user_id`
- injected `user_id` is not expected from end-user message content
- tool summary capture records which wrapped MCP tools ran
- removed SQL guardrail behavior is no longer the primary path

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_sql_tools.py -q`
Expected: FAIL because current placeholders still raise removed-runtime errors.

**Step 3: Write minimal implementation**

- Replace placeholder tool code with MCP-facing wrappers or helpers.
- Keep old exported names if the service still imports them, but redirect them to MCP-safe behavior.
- Make `user_id` injection happen in application code, not only in the prompt.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_sql_tools.py -q`
Expected: PASS

### Task 4: Implement the `create_agent` runtime wrapper

**Files:**
- Modify: `app/domain/chatbot/agents/__init__.py`
- Modify: `app/domain/chatbot/agents/graph.py`
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`
- Modify: `tests/app/domain/chatbot/agents/test_removed_runtime.py`

**Step 1: Write the failing tests**

Add tests that assert:
- `build_chatbot_graph()` returns a runtime wrapper that delegates to a single `create_agent` runnable
- `ainvoke()` returns `final_response`, `final_answer`, `warnings`, and `tool_summaries`
- `astream()` emits message chunks compatible with the current service streaming filter
- runtime wrappers no longer raise the removed-runtime placeholder error

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/agents/test_removed_runtime.py -q`
Expected: FAIL because the current runtime is still placeholder-only.

**Step 3: Write minimal implementation**

- Build a request-scoped MCP client loader.
- Load tools through `MultiServerMCPClient`.
- Wrap tools to inject `user_id`.
- Build the single `create_agent` runnable with the hardened system prompt.
- Add invoke and stream normalization so `ChatbotService` does not need invasive changes.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/agents/test_removed_runtime.py -q`
Expected: PASS

### Task 5: Reconnect the chatbot service to the new runtime

**Files:**
- Modify: `app/domain/chatbot/service/chatbot_service.py`
- Modify: `tests/app/domain/chatbot/service/test_chatbot_service.py`

**Step 1: Write the failing tests**

Update service tests so they validate:
- runtime activation via MCP settings
- request-scoped runtime creation and cleanup
- `reply()` still extracts the final answer
- `stream_reply()` still emits `status`, `token`, and `done`

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/service/test_chatbot_service.py -q`
Expected: FAIL where service wiring still assumes SQL placeholder internals.

**Step 3: Write minimal implementation**

- Remove obsolete repository/SQL tool-bundle assumptions from runtime assembly.
- Keep conversation memory and thread-id logic unchanged.
- Keep request-scoped cleanup behavior unchanged.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/service/test_chatbot_service.py -q`
Expected: PASS

### Task 6: Verify the API surface remains unchanged

**Files:**
- Modify: `tests/app/test_chatbot_api.py` only if necessary

**Step 1: Run the API tests**

Run: `uv run python -m pytest tests/app/test_chatbot_api.py -q`
Expected: PASS

**Step 2: If a failure appears, make the smallest compatibility fix**

Only adjust controller or DTO behavior if the new runtime unintentionally changed response shape or streaming framing.

**Step 3: Re-run the API tests**

Run: `uv run python -m pytest tests/app/test_chatbot_api.py -q`
Expected: PASS

### Task 7: Run final verification and record the work

**Files:**
- No repository file changes required

**Step 1: Run focused chatbot verification**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/agents/test_chatbot_prompts.py tests/app/domain/chatbot/agents/test_sql_tools.py tests/app/domain/chatbot/service/test_chatbot_service.py tests/app/test_chatbot_api.py -q`
Expected: PASS

**Step 2: Run any additional syntax or import verification needed**

Use the available LangChain Docs MCP references while implementing to cross-check `create_agent` and MCP adapter usage, then run a local verification command if needed.

**Step 3: Record the work in Notion**

Update `개인 이력서/특화프로젝트 이슈정리` with:
- 문제 상황
- 원인
- 다양한 접근방법
- 선택한 해결방법
- 결과

Also describe the AI-side preprocessing and postprocessing decisions, including why hidden-context suppression and user-id injection were implemented the way they were.
