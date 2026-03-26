# Register Merchants Daegu Gyeongbuk Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Restrict the merchant registration notebook to load only Daegu and Gyeongbuk regional CSV files.

**Architecture:** The notebook will keep using the existing CSV discovery helper, then immediately filter the returned paths by region-specific filename markers. No shared library behavior will change, which keeps the update scoped to the notebook workflow only.

**Tech Stack:** Jupyter Notebook JSON, Python

---

### Task 1: Update notebook CSV selection

**Files:**
- Modify: `lab/register_merchants.ipynb`
- Verify: `lab/register_merchants.ipynb`

**Step 1: Write the failing check**

Run a small JSON inspection command that expects the notebook to contain Daegu/Gyeongbuk filtering logic.

```python
import json
from pathlib import Path

nb = json.loads(Path("lab/register_merchants.ipynb").read_text(encoding="utf-8"))
preview_cell = next(cell for cell in nb["cells"] if cell.get("id") == "preview-input")
source = "".join(preview_cell["source"])
assert "_대구_" in source and "_경북_" in source
```

**Step 2: Run check to verify it fails**

Run: `.\.venv\Scripts\python.exe -c "<inspection script>"`
Expected: FAIL because the notebook currently loads all regions.

**Step 3: Write minimal implementation**

Insert a region filter in the preview cell and clear stale outputs for that cell.

**Step 4: Run check to verify it passes**

Run the same JSON inspection command.
Expected: PASS

**Step 5: Commit**

```bash
git add lab/register_merchants.ipynb docs/plans/2026-03-24-register-merchants-daegu-gyeongbuk-design.md docs/plans/2026-03-24-register-merchants-daegu-gyeongbuk.md
git commit -m "chore: limit merchant notebook to daegu and gyeongbuk csvs"
```
