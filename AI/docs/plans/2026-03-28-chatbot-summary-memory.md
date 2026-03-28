# Chatbot Summary Memory Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add `ConversationSummaryMemory`-based internal conversation summarization while preserving the current chatbot API and conversation ID contract.

**Architecture:** Keep `ConversationMemoryStore` as the raw transcript and TTL source of truth. Add internal summary state plus a small `ConversationSummaryMemory` adapter, and make `ChatbotService` use summary + unsummarized tail only when a summarizer is configured.

**Tech Stack:** Python, LangChain, langchain-classic, FastAPI, pytest

---

### Task 1: Lock Repository Summary State With Failing Tests

**Files:**
- Modify: `tests/app/domain/chatbot/repository/test_conversation_memory_store.py`
- Test: `tests/app/domain/chatbot/repository/test_conversation_memory_store.py`

**Step 1: Write the failing test**

Add tests that require:
- storing internal summary state
- returning summary + unsummarized tail for internal prompt context
- keeping `get_conversation()` unchanged for external consumers

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/repository/test_conversation_memory_store.py -q`

Expected: FAIL because summary state methods do not exist yet.

**Step 3: Write minimal implementation**

Add `summary_text` and `summarized_message_count` to the internal record plus internal helper methods for prompt context and summary state persistence.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/repository/test_conversation_memory_store.py -q`

Expected: PASS

### Task 2: Lock Service Summary Behavior With Failing Tests

**Files:**
- Modify: `tests/app/domain/chatbot/service/test_chatbot_service.py`
- Create: `app/domain/chatbot/service/conversation_summary_memory.py`
- Modify: `app/domain/chatbot/service/chatbot_service.py`

**Step 1: Write the failing test**

Add tests that require:
- graph input to use summary + unsummarized tail when a summarizer is configured
- reply/stream to update summary state after the assistant final answer is available
- no external response shape changes

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/service/test_chatbot_service.py -q`

Expected: FAIL because the service still forwards the full raw transcript.

**Step 3: Write minimal implementation**

Add a small adapter around `langchain_classic.memory.ConversationSummaryMemory` and wire it into `ChatbotService` as an optional collaborator.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/service/test_chatbot_service.py -q`

Expected: PASS

### Task 3: Add Dependency And Production Wiring

**Files:**
- Modify: `pyproject.toml`
- Modify: `uv.lock`
- Modify: `app/domain/chatbot/service/chatbot_service.py`

**Step 1: Add dependency**

Add `langchain-classic` so `ConversationSummaryMemory` is available from `langchain_classic.memory`.

**Step 2: Wire summarizer into production service creation**

Use the existing model factory path so the chatbot service can construct a summarizer adapter without changing the external API.

**Step 3: Verify focused regression**

Run: `uv run python -m pytest tests/app/domain/chatbot/repository/test_conversation_memory_store.py tests/app/domain/chatbot/service/test_chatbot_service.py tests/app/test_chatbot_api.py -q`

Expected: PASS

### Task 4: Run Broader Regression And Capture The Work

**Files:**
- Modify: `docs/plans/2026-03-28-chatbot-summary-memory-design.md`
- Modify: `docs/plans/2026-03-28-chatbot-summary-memory.md`
- Update externally: Notion issue log page

**Step 1: Run broader regression**

Run: `uv run python -m pytest tests/app/domain/chatbot tests/app/test_chatbot_api.py tests/lab/test_chatbot_graph_visualization_notebook.py -q`

Expected: PASS

**Step 2: Record the implementation**

Log the problem, cause, approaches, chosen solution, and results to the Notion issue page.
