# MOA Chatbot Financial Coach Prompt Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the chatbot supervisor sound like an AI financial coach while preserving all user-scoped, read-only safety guardrails.

**Architecture:** Only the supervisor prompt changes because it is the sole user-facing response source. The API contract, graph structure, tool contracts, and specialist prompts remain unchanged.

**Tech Stack:** pytest, LangGraph supervisor prompt shaping

---

### Task 1: Lock the new prompt contract with a failing test

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`
- Modify: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`

**Step 1: Write the failing test**

Extend the prompt test so `build_supervisor_prompt()` must include:

- an AI financial coach identity
- supportive and practical wording
- read-only / no speculation / user-scoped constraints

**Step 2: Run test to verify it fails**

Run: `python -m uv run pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -v`

Expected: FAIL because the current prompt does not yet mention the coach identity or the reinforced style.

**Step 3: Write minimal implementation**

Update the supervisor prompt to:

- identify as MOA AI financial coach
- keep responses calm, supportive, and practical
- summarize, interpret, and optionally suggest one or two grounded next steps
- preserve all safety guardrails
- retain the markdown response rule

**Step 4: Run focused verification**

Run:

```bash
python -m uv run pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -v
python -m uv run pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -v
```

Expected: PASS

**Step 5: Run broader verification**

Run:

```bash
python -m uv run pytest tests/app -q
```

Expected: PASS
