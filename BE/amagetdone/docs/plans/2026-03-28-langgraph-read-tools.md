# LangGraph Read Tools Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Spring의 자산, 소비 분석, 예산 조회 로직을 DB 직접 조회 기반의 LangGraph용 `tools.py`로 추출한다.

**Architecture:** 루트 `tools.py`에 환경변수 기반 DB 연결, 공통 파싱/응답 헬퍼, 도메인별 조회 함수, LangChain tool 데코레이터 함수를 둔다. Spring 서비스 규칙은 유지하되 응답은 `summary/evidence/warnings/raw` 형태로 재구성한다.

**Tech Stack:** Python 3, sqlalchemy, langchain-core, unittest, PostgreSQL

---

### Task 1: Add Python Test Skeleton For Shared Helpers

**Files:**
- Create: `C:\Users\SSAFY\more\more-be\BE\amagetdone\tests\test_tools.py`
- Create: `C:\Users\SSAFY\more\more-be\BE\amagetdone\tools.py`

**Step 1: Write the failing test**

```python
import unittest

from tools import build_tool_response, parse_iso_year_week, parse_year_month


class ToolsHelpersTest(unittest.TestCase):
    def test_build_tool_response_wraps_sections(self):
        result = build_tool_response(
            "demo",
            {"value": 1},
            [{"label": "proof", "value": 1}],
            ["warn"],
            {"raw": True},
        )
        self.assertEqual("demo", result["tool_name"])
        self.assertEqual({"value": 1}, result["summary"])

    def test_parse_iso_year_week_returns_monday(self):
        self.assertEqual("2026-03-23", str(parse_iso_year_week("2026-W13")))

    def test_parse_year_month_accepts_iso_month(self):
        self.assertEqual("2026-03", str(parse_year_month("2026-03")))
```

**Step 2: Run test to verify it fails**

Run: `python -m unittest tests.test_tools -v`
Expected: FAIL with import errors because `tools.py` helpers do not exist yet.

**Step 3: Write minimal implementation**

```python
def build_tool_response(tool_name, summary, evidence, warnings, raw, window=None):
    ...

def parse_iso_year_week(value):
    ...

def parse_year_month(value):
    ...
```

**Step 4: Run test to verify it passes**

Run: `python -m unittest tests.test_tools -v`
Expected: PASS for helper tests.

**Step 5: Commit**

```bash
git add tests/test_tools.py tools.py
git commit -m "test: add langgraph tools helper coverage"
```

### Task 2: Add DB Connection And Shared Query Helpers

**Files:**
- Modify: `C:\Users\SSAFY\more\more-be\BE\amagetdone\tools.py`
- Modify: `C:\Users\SSAFY\more\more-be\BE\amagetdone\tests\test_tools.py`

**Step 1: Write the failing test**

```python
def test_default_transaction_window_uses_last_month(self):
    start, end = resolve_transaction_window(None, None)
    self.assertLess(start, end)
```

**Step 2: Run test to verify it fails**

Run: `python -m unittest tests.test_tools -v`
Expected: FAIL because `resolve_transaction_window` does not exist.

**Step 3: Write minimal implementation**

```python
def create_db_engine():
    ...

def resolve_transaction_window(from_date, to_date):
    ...

def fetch_all(connection, sql, params):
    ...
```

**Step 4: Run test to verify it passes**

Run: `python -m unittest tests.test_tools -v`
Expected: PASS including new shared helper test.

**Step 5: Commit**

```bash
git add tools.py tests/test_tools.py
git commit -m "feat: add langgraph tools db helpers"
```

### Task 3: Implement Asset Query Helpers And Tools

**Files:**
- Modify: `C:\Users\SSAFY\more\more-be\BE\amagetdone\tools.py`
- Modify: `C:\Users\SSAFY\more\more-be\BE\amagetdone\tests\test_tools.py`

**Step 1: Write the failing test**

```python
def test_summarize_asset_linkage_counts_active_accounts(self):
    summary = summarize_asset_linkage(
        [{"account_no": "1", "is_verified": True, "is_active": True}],
        [],
        [],
    )
    self.assertEqual(1, summary["linked_account_count"])
```

**Step 2: Run test to verify it fails**

Run: `python -m unittest tests.test_tools -v`
Expected: FAIL because asset summarizer is missing.

**Step 3: Write minimal implementation**

```python
def fetch_linked_accounts(...):
    ...

def fetch_linked_cards(...):
    ...

def summarize_asset_linkage(...):
    ...

@tool
def get_linked_accounts(...):
    ...
```

**Step 4: Run test to verify it passes**

Run: `python -m unittest tests.test_tools -v`
Expected: PASS for asset helper tests.

**Step 5: Commit**

```bash
git add tools.py tests/test_tools.py
git commit -m "feat: add asset read tools"
```

### Task 4: Implement Transaction Projection Helpers And Transaction Tools

**Files:**
- Modify: `C:\Users\SSAFY\more\more-be\BE\amagetdone\tools.py`
- Modify: `C:\Users\SSAFY\more\more-be\BE\amagetdone\tests\test_tools.py`

**Step 1: Write the failing test**

```python
def test_classify_pure_expense_excludes_income_and_internal_transfer(self):
    projection = {
        "transaction_type": 2,
        "excluded_reason": None,
        "analysis_included": True,
    }
    self.assertTrue(is_pure_expense(projection))
```

**Step 2: Run test to verify it fails**

Run: `python -m unittest tests.test_tools -v`
Expected: FAIL because transaction classifiers do not exist.

**Step 3: Write minimal implementation**

```python
def fetch_transaction_projections(...):
    ...

def is_pure_expense(...):
    ...

def is_income(...):
    ...

@tool
def get_monthly_expense_summary(...):
    ...
```

**Step 4: Run test to verify it passes**

Run: `python -m unittest tests.test_tools -v`
Expected: PASS for transaction helper tests.

**Step 5: Commit**

```bash
git add tools.py tests/test_tools.py
git commit -m "feat: add transaction analysis read tools"
```

### Task 5: Implement Budget Tools

**Files:**
- Modify: `C:\Users\SSAFY\more\more-be\BE\amagetdone\tools.py`
- Modify: `C:\Users\SSAFY\more\more-be\BE\amagetdone\tests\test_tools.py`

**Step 1: Write the failing test**

```python
def test_budget_dashboard_summary_reports_remaining_amount(self):
    result = summarize_budget_dashboard(
        {
            "final_free_amount": 100000,
            "actual_spent_amount": 40000,
            "week_end_date": date(2026, 3, 30),
        },
        date(2026, 3, 28),
    )
    self.assertEqual(60000, result["remaining_amount"])
```

**Step 2: Run test to verify it fails**

Run: `python -m unittest tests.test_tools -v`
Expected: FAIL because budget summarizer is missing.

**Step 3: Write minimal implementation**

```python
def fetch_current_budget_week(...):
    ...

def summarize_budget_dashboard(...):
    ...

@tool
def get_budget_dashboard(...):
    ...
```

**Step 4: Run test to verify it passes**

Run: `python -m unittest tests.test_tools -v`
Expected: PASS for budget helper tests.

**Step 5: Commit**

```bash
git add tools.py tests/test_tools.py
git commit -m "feat: add budget read tools"
```

### Task 6: Export Tool Registry And Verify

**Files:**
- Modify: `C:\Users\SSAFY\more\more-be\BE\amagetdone\tools.py`

**Step 1: Write the failing test**

```python
def test_tools_registry_contains_budget_dashboard(self):
    self.assertTrue(any(tool.name == "get_budget_dashboard" for tool in TOOLS))
```

**Step 2: Run test to verify it fails**

Run: `python -m unittest tests.test_tools -v`
Expected: FAIL because `TOOLS` registry is missing.

**Step 3: Write minimal implementation**

```python
TOOLS = [
    get_linked_accounts,
    get_account_detail,
    ...
    get_weekly_essential_expense_detail,
]

__all__ = ["TOOLS", ...]
```

**Step 4: Run test to verify it passes**

Run: `python -m unittest tests.test_tools -v`
Expected: PASS for all tests.

**Step 5: Commit**

```bash
git add tools.py tests/test_tools.py
git commit -m "feat: export langgraph read tools registry"
```

### Task 7: Manual Smoke Verification

**Files:**
- Modify: `C:\Users\SSAFY\more\more-be\BE\amagetdone\tools.py`

**Step 1: Run unit tests**

Run: `python -m unittest tests.test_tools -v`
Expected: PASS

**Step 2: Run import smoke test**

Run: `python -c "import tools; print(len(tools.TOOLS))"`
Expected: positive integer output

**Step 3: Run non-destructive sample call**

Run: `python -c "import tools; print(tools.get_budget_summary.invoke({'user_id': 1}))"`
Expected: structured response or domain-level not-found warning, but no import/runtime crash

**Step 4: Commit**

```bash
git add tools.py tests/test_tools.py
git commit -m "feat: add langgraph read tools"
```
