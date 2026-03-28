# Chatbot Markdown Output Contract Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the spending coach prompt require a stable Korean markdown output structure.

**Architecture:** Keep the existing `create_agent` runtime and tighten only the prompt contract. Add tests first, then update the coaching and response-guard prompts so the agent is guided toward Korean markdown sections without changing the service or API layer.

**Tech Stack:** Python, LangChain, LangGraph, pytest

---

### Task 1: Lock The Markdown Contract In Tests

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`
- Test: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`

**Step 1: Write the failing test**

Add assertions that require:
- `## 이번 소비 한눈에`
- `## 왜 이렇게 됐는지`
- `## 이번 주 행동 제안`
- Korean markdown guidance
- no JSON/code-block oriented default contract

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -q`

Expected: FAIL because the current prompt still uses the older English headings.

**Step 3: Write minimal implementation**

Update the chatbot prompt builders so the Korean markdown structure is explicit in the system prompt.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -q`

Expected: PASS

### Task 2: Verify Regression Surface

**Files:**
- Modify: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`
- Test: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`
- Test: `tests/app/domain/chatbot/service/test_chatbot_service.py`

**Step 1: Run focused regression**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py tests/app/domain/chatbot/service/test_chatbot_service.py tests/app/test_chatbot_api.py -q`

Expected: PASS

**Step 2: Run broader regression**

Run: `uv run python -m pytest tests/app/domain/chatbot tests/app/test_chatbot_api.py tests/lab/test_chatbot_graph_visualization_notebook.py -q`

Expected: PASS

### Task 3: Capture The Change

**Files:**
- Modify: `docs/plans/2026-03-28-chatbot-markdown-output-contract-design.md`
- Modify: `docs/plans/2026-03-28-chatbot-markdown-output-contract.md`
- Update externally: Notion issue log page

**Step 1: Summarize the change**

Record the strengthened markdown contract, why prompt-only reinforcement was chosen, and what verification was run.

**Step 2: Log to Notion**

Add a dated toggle entry under the existing issue log page using the required structure:
- 문제 상황
- 원인
- 다양한 접근방법
- 선택한 해결방법
- 결과
