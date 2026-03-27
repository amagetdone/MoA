# MOA Chatbot Markdown Hardening Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the chatbot supervisor consistently answer in markdown and align anomaly date resolution with the other chatbot tools.

**Architecture:** Tighten the supervisor system prompt because that is the only user-facing LLM output. Add prompt and tool regression tests first, then make the minimal internal anomaly-date change so all relative read-only windows derive from the same injected date source.

**Tech Stack:** FastAPI, pytest, LangGraph, LangChain tools, prompt-based response shaping

---

### Task 1: Lock the markdown contract with failing tests

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`

**Step 1: Write the failing test**

Add assertions that `build_supervisor_prompt()` requires:

- markdown-only final answers
- explicit section, bullet, and comparison-table guidance
- avoidance of plain-text walls

**Step 2: Run test to verify it fails**

Run: `uv run pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -v`
Expected: FAIL because the prompt is not strict enough yet.

**Step 3: Write minimal implementation**

Update `app/domain/chatbot/agents/prompts/chatbot_prompts.py` with stronger markdown instructions and output constraints.

**Step 4: Run test to verify it passes**

Run: `uv run pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -v`
Expected: PASS

### Task 2: Align anomaly date resolution with other tools

**Files:**
- Modify: `tests/app/domain/chatbot/tools/test_finance_tools.py`
- Modify: `app/domain/chatbot/agents/tools/finance_tools.py`
- Modify: `app/domain/chatbot/repository/chatbot_repository.py`
- Modify: `app/domain/chatbot/service/chatbot_service.py`

**Step 1: Write the failing test**

Add a test that injects `today_factory=lambda: date(2026, 3, 27)` into the anomaly tool and expects the repository to receive that explicit date source.

**Step 2: Run test to verify it fails**

Run: `uv run pytest tests/app/domain/chatbot/tools/test_finance_tools.py -v`
Expected: FAIL because anomaly currently falls back to `date.today()`.

**Step 3: Write minimal implementation**

Thread the injected base date through the anomaly tool and repository call path.

**Step 4: Run test to verify it passes**

Run: `uv run pytest tests/app/domain/chatbot/tools/test_finance_tools.py -v`
Expected: PASS

### Task 3: Verify chatbot integration remains stable

**Files:**
- Verify only: `tests/app/domain/chatbot/service/test_chatbot_service.py`
- Verify only: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Run focused verification**

Run:

```bash
uv run pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -v
uv run pytest tests/app/domain/chatbot/tools/test_finance_tools.py -v
uv run pytest tests/app/domain/chatbot/service/test_chatbot_service.py -v
uv run pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -v
```

Expected: PASS
