# Beauty Retail Relabel Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Re-label beauty-retail merchants in the gold dataset from `미용` to `쇼핑/패션` without moving true beauty service merchants.

**Architecture:** Add a small relabeling utility under `AI/lab/` that detects beauty-retail text using explicit inclusion and exclusion rules. Cover the rule with regression tests, then apply it to the three dataset CSV files and verify the changed counts.

**Tech Stack:** Python, pandas, pytest

---

### Task 1: Add regression tests for the relabeling rule

**Files:**
- Create: `AI/tests/lab/test_beauty_retail_relabel.py`
- Reference: `AI/lab/data/merchant_category_dataset_v1/train.csv`

**Step 1: Write the failing test**

Add tests that assert:

- `올리브영`, `미샤`, `랄라블라`, `화장품`, `코스메틱` retail-style names are detected as beauty retail
- `헤어`, `네일`, `피부관리`, `마사지`, `사우나` service-style names are not detected as beauty retail
- relabeling changes `category_id/category_name` from `6/미용` to `9/쇼핑/패션` only for matching rows

**Step 2: Run test to verify it fails**

Run: `pytest tests/lab/test_beauty_retail_relabel.py -v`

Expected: FAIL because the utility does not exist yet.

### Task 2: Implement the relabel utility

**Files:**
- Create: `AI/lab/beauty_retail_relabel.py`
- Modify: `AI/lab/__init__.py`

**Step 1: Write minimal implementation**

Implement:

- inclusion keyword list
- exclusion keyword list
- `is_beauty_retail_text(text: object) -> bool`
- `relabel_beauty_retail_rows(df: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame]`

`relabel_beauty_retail_rows` should return the updated dataframe and a dataframe containing only changed rows for auditing.

**Step 2: Run targeted tests**

Run: `pytest tests/lab/test_beauty_retail_relabel.py -v`

Expected: PASS

### Task 3: Apply relabeling to dataset CSV files

**Files:**
- Modify: `AI/lab/data/merchant_category_dataset_v1/train.csv`
- Modify: `AI/lab/data/merchant_category_dataset_v1/valid.csv`
- Modify: `AI/lab/data/merchant_category_dataset_v1/test.csv`

**Step 1: Apply utility**

Run a small script that:

- loads each CSV
- applies `relabel_beauty_retail_rows`
- overwrites the CSV in UTF-8 BOM format
- prints changed counts and sample rows

**Step 2: Verify data changes**

Check that:

- known beauty-retail brands move to `쇼핑/패션`
- service examples stay in `미용`
- category IDs stay aligned with names

### Task 4: Run full verification

**Files:**
- Test: `AI/tests/lab/test_beauty_retail_relabel.py`
- Test: `AI/tests/lab/test_merchant_info_augmentation.py`

**Step 1: Run verification**

Run:

- `pytest tests/lab/test_beauty_retail_relabel.py -v`
- `pytest tests/lab/test_merchant_info_augmentation.py -v`

**Step 2: Summarize output**

Report:

- changed rows per split
- representative examples moved to `쇼핑/패션`
- whether any expected service examples were touched
