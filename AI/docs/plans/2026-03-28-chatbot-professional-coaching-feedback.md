# Chatbot Professional Coaching Feedback Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make chatbot coaching feedback more professional and more helpful by tightening the prompt contract for diagnosis quality, evidence discipline, prioritization, and actionable advice.

**Architecture:** Keep the current chatbot runtime unchanged and improve only the prompt layer plus prompt-level tests. The spendwise coach prompt will define the target coaching behavior, and the response guard prompt will enforce it as the final output contract.

**Tech Stack:** Python, LangChain prompt builders, pytest

---

### Task 1: Lock the new coaching contract in tests

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`
- Reference: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`

**Step 1: Write the failing test**

Add assertions that require the coach prompt to mention:

- diagnosis-first coaching
- evidence vs inference labeling
- prioritizing the biggest one or two drivers
- separating controllable spending from unavoidable burden
- concrete, time-aware actions
- avoiding vague advice and filler encouragement

Add assertions that require the response guard prompt to mention:

- grounded evidence or clearly labeled inference
- rejection of generic coaching
- rejection of non-actionable next steps

**Step 2: Run test to verify it fails**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -q`

Expected: FAIL in the new prompt-contract assertions.

**Step 3: Write minimal implementation**

Do not change runtime code yet. Update the prompt builders only after the failing tests prove the contract is not present.

**Step 4: Run test to verify it passes**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -q`

Expected: PASS

### Task 2: Strengthen the spendwise coach prompt

**Files:**
- Modify: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`
- Test: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`

**Step 1: Update the prompt**

Add explicit instructions that the final answer must:

- open with a short diagnosis
- explain only the biggest drivers first
- mark inference when tool evidence is incomplete
- split controllable vs unavoidable pressure when relevant
- give one to three concrete actions with timing
- avoid vague phrases such as "manage better" or "spend less"

**Step 2: Keep the current markdown contract**

Preserve:

- Korean markdown
- fixed section headings unless user requests otherwise
- no JSON by default
- no code fences by default
- no internal leakage

**Step 3: Run the prompt tests**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -q`

Expected: PASS

### Task 3: Strengthen the response guard prompt

**Files:**
- Modify: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`
- Test: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`

**Step 1: Update the guard**

Require the guard to reject answers that:

- make unsupported numeric claims
- state guesses as facts
- skip prioritization
- include generic encouragement without an operational next step

**Step 2: Re-run prompt tests**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -q`

Expected: PASS

### Task 4: Run regression verification

**Files:**
- Verify: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`
- Verify: `tests/app/domain/chatbot`
- Verify: `tests/app/test_chatbot_api.py`

**Step 1: Run focused chatbot verification**

Run: `uv run python -m pytest tests/app/domain/chatbot tests/app/test_chatbot_api.py -q`

Expected: PASS with zero failures.

**Step 2: Summarize changed behavior**

Capture:

- what the prompt now enforces
- what generic output patterns are now discouraged
- what verification proves the contract is stable

### Task 5: Record the work in Notion

**Files:**
- Update: Notion page `특화 프로젝트 이슈정리`

**Step 1: Add a dated toggle entry**

Document:

- problem situation
- root cause
- alternative approaches
- chosen solution
- result

**Step 2: Include AI processing rationale**

Explain:

- what was tightened at prompt-input contract level
- what was tightened at prompt-output validation level
- why prompt-only hardening was chosen over a second rewriting layer
