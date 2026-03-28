# Chatbot User-Facing Leak Guardrails Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Stop internal analysis labels from reaching user-visible coaching and require user-friendly explanations instead.

**Architecture:** Harden the coach prompt and response guard so the model is instructed to translate internal logic into plain Korean, then expand the existing fail-closed leak detector to catch known internal labels if prompt compliance fails. Keep the API and graph topology unchanged.

**Tech Stack:** Python, LangChain/LangGraph prompt builders, pytest

---

### Task 1: Add failing prompt and leak tests

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_professional_prompt_contract.py`
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Write the failing tests**

- Assert the coach prompt forbids raw internal labels such as `NON_ESSENTIAL`, `INCOME`, `OUT`, and `analysisIncluded`.
- Assert the guard prompt rejects backend labels, field names, and raw tool payload snippets.
- Assert the graph drops final answers that include raw internal classification labels.

**Step 2: Run the targeted tests to verify they fail**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py tests/app/domain/chatbot/agents/test_chatbot_professional_prompt_contract.py tests/app/domain/chatbot/agents/test_chatbot_graph.py -k "internal or leak or prompt" -q`

### Task 2: Implement the prompt and guardrail hardening

**Files:**
- Modify: `app/domain/chatbot/agents/prompts/chatbot_prompts.py`
- Modify: `app/domain/chatbot/agents/graph.py`

**Step 1: Update prompt contracts**

- Tell the coach to translate internal classification logic into user-facing Korean.
- Explicitly ban raw enum values, field names, and raw tool snippets in the final answer.
- Tell the guard to reject answers containing backend-style labels or key-value fragments.

**Step 2: Update fail-closed leak patterns**

- Add internal finance-analysis labels and field-name style snippets to the forbidden response patterns.

### Task 3: Verify regressions

**Files:**
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_prompts.py`
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_professional_prompt_contract.py`
- Modify: `tests/app/domain/chatbot/agents/test_chatbot_graph.py`

**Step 1: Run targeted tests**

Run: `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_prompts.py tests/app/domain/chatbot/agents/test_chatbot_professional_prompt_contract.py tests/app/domain/chatbot/agents/test_chatbot_graph.py -q`

**Step 2: Run broader chatbot regression**

Run: `uv run python -m pytest tests/app/domain/chatbot tests/app/test_chatbot_api.py -q`
