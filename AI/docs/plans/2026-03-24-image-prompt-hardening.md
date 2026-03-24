# Image Prompt Hardening Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Keep the raw image-generation request unchanged in `Goal request` and strengthen prompt rules to block speech bubbles and other overlay-like decorations.

**Architecture:** This change stays inside the image-generation prompt builder. Tests will define the new behavior first, then the prompt template will be minimally updated to remove structured parsing and add stronger overlay suppression instructions.

**Tech Stack:** Python, LangChain `ChatPromptTemplate`, pytest

---

### Task 1: Lock the new prompt contract in tests

**Files:**
- Modify: `tests/app/domain/image_generation/test_prompt_graph.py`
- Test: `tests/app/domain/image_generation/test_prompt_graph.py`

**Step 1: Write the failing test**

Update the structured-input test so it expects:

```python
assert "Goal request: [Title] ..." in prompt
```

Add assertions that the prompt contains explicit overlay suppression language, for example:

```python
assert "speech bubbles" in prompt
assert "UI overlays" in prompt
```

**Step 2: Run test to verify it fails**

Run: `pytest tests/app/domain/image_generation/test_prompt_graph.py -v`
Expected: FAIL because the implementation still splits title/description and does not include the stronger overlay restrictions.

**Step 3: Write minimal implementation**

Remove the parsing helper and pass the raw `user_prompt` directly into the human prompt as `Goal request`. Extend the system prompt with explicit overlay suppression rules.

**Step 4: Run test to verify it passes**

Run: `pytest tests/app/domain/image_generation/test_prompt_graph.py -v`
Expected: PASS

**Step 5: Commit**

```bash
git add docs/plans/2026-03-24-image-prompt-hardening-design.md docs/plans/2026-03-24-image-prompt-hardening.md tests/app/domain/image_generation/test_prompt_graph.py app/domain/image_generation/agents/prompts/image_generation_prompt.py
git commit -m "fix: harden image generation prompt overlays"
```

### Task 2: Verify no prompt regressions in service flow

**Files:**
- Modify: `tests/app/domain/image_generation/test_image_generation_service.py` only if needed
- Test: `tests/app/domain/image_generation/test_image_generation_service.py`

**Step 1: Write the failing test**

Only if prompt wording changes break an existing service assertion, update the expectation to assert the raw request is still present and that prompt generation remains enriched.

**Step 2: Run test to verify it fails**

Run: `pytest tests/app/domain/image_generation/test_image_generation_service.py -v`
Expected: FAIL only if an outdated prompt assumption exists.

**Step 3: Write minimal implementation**

If needed, adjust only the affected assertion to match the new prompt contract.

**Step 4: Run test to verify it passes**

Run: `pytest tests/app/domain/image_generation/test_image_generation_service.py -v`
Expected: PASS

**Step 5: Commit**

```bash
git add tests/app/domain/image_generation/test_image_generation_service.py
git commit -m "test: align image generation service expectations"
```
