# Chatbot Autonomous Read-Only Range Resolution Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the chatbot answer read-only finance questions without unnecessary follow-up questions by inferring safe date ranges and using sensible defaults.

**Architecture:** Keep the existing supervisor graph and repository contract, but strengthen the supervisor prompt and the finance tool layer. The prompt will tell the model to infer common read-only ranges and avoid asking follow-up questions unless the request is unsafe or requires modification. The finance tools will accept omitted dates or named safe periods and resolve them to concrete dates before calling the repository.

**Tech Stack:** Python, FastAPI, LangGraph, LangChain tools, pytest

---

### Task 1: Add failing tests for autonomous prompt guidance

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`

**Step 1: Write the failing test**

Add assertions that the supervisor prompt explicitly says:
- read-only questions should be answered with reasonable defaults
- common periods like `this week` and `last 7 days` are inferred automatically
- follow-up questions are reserved for unsafe or modifying requests

**Step 2: Run test to verify it fails**

Run: `pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -v`
Expected: FAIL because the current prompt does not contain the new guidance.

**Step 3: Write minimal implementation**

Update `app/domain/chatbot/agents/prompts/chatbot_prompts.py` so the supervisor and specialist prompts include the new autonomous read-only analysis rules.

**Step 4: Run test to verify it passes**

Run: `pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -v`
Expected: PASS

### Task 2: Add failing tests for safe date-range defaults

**Files:**
- Modify: `tests/app/domain/chatbot/tools/test_finance_tools.py`

**Step 1: Write the failing test**

Add tests that prove:
- the budget tool uses today's date when `base_date` is omitted
- spending summary defaults to the recent 7 days when no date range is supplied
- spending summary can resolve a named period like `this_week`
- spending by category can resolve the same named period

**Step 2: Run test to verify it fails**

Run: `pytest tests/app/domain/chatbot/tools/test_finance_tools.py -v`
Expected: FAIL because the current tool schemas require explicit dates.

**Step 3: Write minimal implementation**

Update `app/domain/chatbot/agents/tools/finance_tools.py` to:
- add a shared date-window resolver
- support named safe read-only periods
- fall back to recent 7 days when a spending window is omitted
- fall back to today when a budget base date is omitted

**Step 4: Run test to verify it passes**

Run: `pytest tests/app/domain/chatbot/tools/test_finance_tools.py -v`
Expected: PASS

### Task 3: Verify targeted chatbot behavior

**Files:**
- Modify: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`
- Modify: `app/domain/chatbot/agents/tools/finance_tools.py`
- Test: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`
- Test: `tests/app/domain/chatbot/tools/test_finance_tools.py`

**Step 1: Run focused verification**

Run: `pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py tests/app/domain/chatbot/tools/test_finance_tools.py -v`
Expected: PASS

**Step 2: Check for accidental regressions in chatbot integration**

Run: `pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/service/test_chatbot_service.py -v`
Expected: PASS

### Task 4: Document the finished work

**Files:**
- External: Notion page `개인이력서/특화프로젝트 이슈정리`

**Step 1: Summarize the change**

Write the completed work in the required Notion page using:
- 문제 상황
- 원인
- 다양한 접근방법
- 선택한 해결방법
- 결과

**Step 2: Include AI-specific reasoning**

Explain:
- how prompt-level preprocessing was strengthened
- how tool-level post-processing/default resolution was added
- why both were needed to reduce unnecessary follow-up questions safely
