# MOA Chatbot Professional Counselor Tone Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the chatbot always answer in a professional, composed counselor tone regardless of the user's speaking style.

**Architecture:** Update only the supervisor system prompt because it is the sole user-facing response source. Lock the behavior with prompt-level tests so the tone contract stays explicit and durable.

**Tech Stack:** pytest, LangGraph supervisor prompt shaping

---

### Task 1: Lock the tone contract with a failing prompt test

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`
- Modify: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`

**Step 1: Write the failing test**

Add a prompt test that requires `build_supervisor_prompt()` to mention:

- a professional and composed counselor identity
- a rule not to mirror casual, rude, or emotional user phrasing
- the same tone for missing-data, limitation, refusal, and follow-up responses

**Step 2: Run test to verify it fails**

Run: `python -m uv run pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -v`

Expected: FAIL because the current supervisor prompt does not yet lock those tone rules.

**Step 3: Write minimal implementation**

Update `build_supervisor_prompt()` to:

- define the chatbot as professional and composed like a financial counselor
- keep the tone steady even when the user writes casually or emotionally
- keep limitation and no-data responses equally professional
- avoid slang, cheerleading, emojis, or playful mirroring

**Step 4: Run focused verification**

Run:

```bash
python -m uv run pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py -v
```

Expected: PASS

**Step 5: Run broader regression verification**

Run:

```bash
python -m uv run pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py -v
python -m uv run pytest tests/app/domain/chatbot/service/test_chatbot_service.py -v
```

Expected: PASS
