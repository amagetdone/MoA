# ELECTRA Model Loading Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Keep auto-model loading in place while failing fast when the uploaded merchant classifier no longer resolves to an ELECTRA sequence-classification model.

**Architecture:** The loader in `AI/app/core/model_loader.py` remains responsible for model creation. After the Hugging Face auto-model is loaded, a tiny validation helper checks the resolved class/config and raises a clear error if the runtime model is not ELECTRA. Tests in `AI/tests/unit/test_model_loading_config.py` pin both the allowed and rejected behaviors.

**Tech Stack:** Python 3.12, FastAPI, transformers, pytest

---

### Task 1: Add loader validation tests

**Files:**
- Modify: `AI/tests/unit/test_model_loading_config.py`
- Test: `AI/tests/unit/test_model_loading_config.py`

**Step 1: Write the failing test**

Add tests that:
- accept a loaded model whose class/config resolve to ELECTRA
- reject a loaded model whose class/config resolve to a different sequence-classification architecture

**Step 2: Run test to verify it fails**

Run: `python -m pytest AI/tests/unit/test_model_loading_config.py -v`
Expected: FAIL because the loader does not yet validate the resolved architecture.

**Step 3: Write minimal implementation**

Add a small validation helper in `AI/app/core/model_loader.py` and call it immediately after loading the model.

**Step 4: Run test to verify it passes**

Run: `python -m pytest AI/tests/unit/test_model_loading_config.py -v`
Expected: PASS

### Task 2: Verify targeted regression coverage

**Files:**
- Modify: `AI/tests/unit/test_model_loading_config.py`

**Step 1: Run focused verification**

Run: `python -m pytest AI/tests/unit/test_model_loading_config.py -v`
Expected: all tests pass, including the new ELECTRA validation coverage.

**Step 2: Review error messages**

Ensure the rejection path tells us the unexpected class/config so startup failures are easy to diagnose.
