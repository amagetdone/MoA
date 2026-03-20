# Merchant Info Augmentation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Extend the merchant training notebook so it augments only the training split with merchant-info CSV rows mapped through the small-category mapping file while keeping the original valid and test splits unchanged.

**Architecture:** Put the augmentation logic in a small Python module under `AI/lab` so it can be imported by the notebook and tested with pytest. Keep the notebook responsible for orchestration, logging, and saving augmented CSV artifacts while preserving the current training flow.

**Tech Stack:** Python 3.12, pandas, pytest, Jupyter notebook JSON

---

### Task 1: Create augmentation module tests

**Files:**
- Create: `AI/tests/lab/test_merchant_info_augmentation.py`

**Step 1: Write the failing test**

Write tests that verify:
- merchant names are normalized with the expected rule
- mapping joins by `상권업종소분류코드`
- unsupported categories are dropped
- duplicate augmented rows are removed
- augmented rows are split into `8:1:1`
- rows already present in the base dataset are excluded

**Step 2: Run test to verify it fails**

Run: `python -m pytest AI/tests/lab/test_merchant_info_augmentation.py -v`
Expected: FAIL because the module does not exist yet.

**Step 3: Commit**

Do not commit yet if the implementation is not in place.

### Task 2: Implement the augmentation module

**Files:**
- Create: `AI/lab/merchant_info_augmentation.py`
- Test: `AI/tests/lab/test_merchant_info_augmentation.py`

**Step 1: Write minimal implementation**

Add functions for:
- text normalization
- loading mapping CSV
- loading merchant-info CSV files
- converting raw merchant-info rows into dataset rows
- dropping duplicates and overlaps
- deterministic `8:1:1` split
- saving augmented split CSVs

**Step 2: Run focused tests**

Run: `python -m pytest AI/tests/lab/test_merchant_info_augmentation.py -v`
Expected: PASS

**Step 3: Refactor**

Keep helpers small and return plain pandas DataFrames so the notebook can use them directly.

### Task 3: Wire the notebook to the module

**Files:**
- Modify: `AI/lab/koelectra_merchant_training_kfold.ipynb`
- Modify: `AI/lab/merchant_info_augmentation.py`

**Step 1: Add notebook import and config**

Add paths for:
- merchant-info directory
- mapping CSV
- augmented output directory
- augmentation split ratio and seed reuse

**Step 2: Add augmentation loading cell**

Load existing `train/valid/test`, call the augmentation module, merge the new rows into `train` only, and print:
- original row counts
- augmented row counts
- merged row counts
- category distribution summary

**Step 3: Save augmented artifacts**

Save generated split CSVs under the dataset directory so reruns can inspect the augmented source directly.

**Step 4: Run notebook JSON sanity check**

Run a lightweight script that opens the notebook as JSON and confirms the new code cells exist and the notebook is valid JSON.

### Task 4: Verify end-to-end behavior

**Files:**
- Test: `AI/tests/lab/test_merchant_info_augmentation.py`
- Verify: `AI/lab/koelectra_merchant_training_kfold.ipynb`

**Step 1: Run tests**

Run: `python -m pytest AI/tests/lab/test_merchant_info_augmentation.py -v`
Expected: PASS

**Step 2: Run notebook smoke verification**

Run a small Python script that imports the augmentation module, loads a small sample of the real files, and prints split counts.
Expected: non-empty augmented train split and no schema mismatch.

**Step 3: Review git diff**

Run: `git diff -- AI/lab/merchant_info_augmentation.py AI/tests/lab/test_merchant_info_augmentation.py AI/lab/koelectra_merchant_training_kfold.ipynb`
Expected: only the planned files changed.

**Step 4: Commit**

If everything passes and the diff is clean, create a focused commit for the feature work.
