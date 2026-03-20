# Beauty Eval Augmentation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Split merchant-info `미용` weak labels across train/valid/test and merge them into the official evaluation splits.

**Architecture:** Extend the augmentation module with an opt-in category-aware split function. By default, augmentation stays train-only, but callers can nominate categories such as `미용` for 8:1:1 distribution into train/valid/test. The notebook then enables that option explicitly.

**Tech Stack:** Python, pandas, numpy, pytest, Jupyter notebook JSON

---

### Task 1: Add failing regression coverage

**Files:**
- Modify: `AI/tests/lab/test_merchant_info_augmentation.py`

**Step 1: Write the failing test**

Add a test that builds a fixture with `미용` merchant-info rows and asserts:

- `미용` rows are split into train/valid/test
- other categories still go to train only
- merged valid/test gain the expected number of `미용` rows

**Step 2: Run test to verify it fails**

Run: `pytest tests/lab/test_merchant_info_augmentation.py -v`

Expected: FAIL because the augmentation module only supports train-only splitting.

### Task 2: Implement category-aware split logic

**Files:**
- Modify: `AI/lab/merchant_info_augmentation.py`

**Step 1: Write minimal implementation**

Add:

- a helper that accepts eval-augmented categories and split ratios
- deterministic 8:1:1 splitting for nominated categories
- merged valid/test behavior that includes augmented rows for those categories

**Step 2: Run targeted tests**

Run: `pytest tests/lab/test_merchant_info_augmentation.py -v`

Expected: PASS

### Task 3: Enable beauty eval augmentation in the notebook

**Files:**
- Modify: `AI/lab/koelectra_merchant_training_kfold.ipynb`

**Step 1: Update notebook settings**

Add explicit notebook config for:

- eval-augmented categories: `('미용',)`
- eval split ratios: `(0.8, 0.1, 0.1)`

Update the explanatory text to reflect that `미용` merchant-info rows are merged into valid/test as official evaluation data.

**Step 2: Verify notebook structure**

Run the notebook structure test after the JSON edit.

### Task 4: Run verification

**Files:**
- Test: `AI/tests/lab/test_merchant_info_augmentation.py`
- Test: `AI/tests/lab/test_koelectra_training_notebook_structure.py`
- Test: `AI/tests/lab/test_beauty_retail_relabel.py`

**Step 1: Run verification**

Run:

- `pytest tests/lab/test_merchant_info_augmentation.py -v`
- `pytest tests/lab/test_koelectra_training_notebook_structure.py -v`
- `pytest tests/lab/test_beauty_retail_relabel.py -v`

**Step 2: Summarize result**

Report:

- how many `미용` rows were added to train/valid/test
- whether valid/test now contain `미용`
- that default train-only behavior still remains available
