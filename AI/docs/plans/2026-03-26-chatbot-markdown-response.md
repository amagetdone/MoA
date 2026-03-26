# MOA Chatbot Markdown Response Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the chatbot supervisor produce user-facing answers in basic markdown.

**Architecture:** Keep the API contract unchanged and update only the supervisor prompt because only supervisor output is exposed to the user. Add one prompt-level test to lock the markdown instruction into place.

**Tech Stack:** FastAPI, pytest, LangGraph, prompt-based response shaping

---

### Task 1: Lock the markdown requirement with a failing prompt test

**Files:**
- Create: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`
- Modify: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`

**Step 1: Write the failing test**

Add a test that asserts `build_supervisor_prompt()` includes:

- a markdown requirement
- guidance to keep formatting readable and not excessive

**Step 2: Run test to verify it fails**

Run: `python -m uv run pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -v`

Expected: FAIL because the prompt does not yet mention markdown.

**Step 3: Write minimal implementation**

Update `build_supervisor_prompt()` so the final answer is explicitly requested in basic markdown, with restrained use of bullets, headings, and tables.

**Step 4: Run test to verify it passes**

Run: `python -m uv run pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -v`

Expected: PASS

**Step 5: Run broader verification**

Run:

```bash
python -m uv run pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -v
python -m uv run pytest tests/app -q
```

Expected: PASS
