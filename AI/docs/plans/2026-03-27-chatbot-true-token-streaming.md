# Chatbot True Token Streaming Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make MOA chatbot SSE emit real incremental supervisor text chunks instead of a single final message.

**Architecture:** Enable streaming at the shared LLM client layer and keep the existing SSE/service filtering path. Verify both unit-level config and live chatbot streaming behavior.

**Tech Stack:** FastAPI, LangChain `ChatOpenAI`, LangGraph supervisor, pytest

---

### Task 1: Lock the desired LLM config

**Files:**
- Modify: `tests/app/core/test_llm.py`
- Test: `tests/app/core/test_llm.py`

**Step 1: Write the failing test**

- Expect `build_chat_model()` to enable streaming.
- Expect `build_chat_model()` to stop forcing `disable_streaming="tool_calling"`.

**Step 2: Run test to verify it fails**

Run: `.\.venv\Scripts\python -m pytest tests\app\core\test_llm.py -v`

**Step 3: Write minimal implementation**

- Update `app/core/llm.py` kwargs so the shared `ChatOpenAI` client allows streaming for tool-bound chatbot agents.

**Step 4: Run test to verify it passes**

Run: `.\.venv\Scripts\python -m pytest tests\app\core\test_llm.py -v`

### Task 2: Verify chatbot streaming end-to-end

**Files:**
- Modify: none unless verification reveals a second blocker
- Test: `tests/app/domain/chatbot/service/test_chatbot_service.py`, `tests/app/test_chatbot_api.py`

**Step 1: Run regression tests**

Run: `.\.venv\Scripts\python -m pytest tests\app\domain\chatbot\service\test_chatbot_service.py tests\app\test_chatbot_api.py -v`

**Step 2: Run live verification**

- Call the real `ChatbotService.stream_reply()` with production-like settings.
- Record event timestamps and confirm multiple `token` events arrive before `done`.

**Step 3: Only if still single-chunk**

- Re-investigate the LangGraph streaming boundary before making any additional code changes.
