import unittest
from datetime import date

from tools import (
    build_tool_response,
    parse_iso_year_week,
    parse_year_month,
    summarize_asset_linkage,
    is_pure_expense,
    summarize_budget_dashboard,
)


class ToolsHelpersTest(unittest.TestCase):
    def test_build_tool_response_wraps_sections(self):
        result = build_tool_response(
            tool_name="demo",
            summary={"value": 1},
            evidence=[{"label": "proof", "value": 1}],
            warnings=["warn"],
            raw={"raw": True},
        )

        self.assertEqual("demo", result["tool_name"])
        self.assertEqual({"value": 1}, result["summary"])
        self.assertEqual([{"label": "proof", "value": 1}], result["evidence"])
        self.assertEqual(["warn"], result["warnings"])
        self.assertEqual({"raw": True}, result["raw"])
        self.assertIn("as_of", result)

    def test_parse_iso_year_week_returns_monday(self):
        self.assertEqual(date(2026, 3, 23), parse_iso_year_week("2026-W13"))

    def test_parse_year_month_accepts_iso_month(self):
        self.assertEqual("2026-03", str(parse_year_month("2026-03")))

    def test_summarize_asset_linkage_counts_active_accounts(self):
        summary = summarize_asset_linkage(
            accounts=[
                {"account_no": "111", "is_verified": True, "is_active": True},
                {"account_no": "222", "is_verified": False, "is_active": False},
            ],
            cards=[
                {"card_no": "1234"},
                {"card_no": "5678"},
            ],
            recent_activities=[
                {"source_type": "ACCOUNT", "occurred_at": "2026-03-27T10:00:00+09:00"},
            ],
        )

        self.assertEqual(2, summary["linked_account_count"])
        self.assertEqual(1, summary["active_account_count"])
        self.assertEqual(1, summary["verified_account_count"])
        self.assertEqual(2, summary["linked_card_count"])
        self.assertEqual("2026-03-27T10:00:00+09:00", summary["latest_activity_at"])

    def test_is_pure_expense_excludes_income_and_internal_transfer(self):
        self.assertTrue(
            is_pure_expense(
                {
                    "source_type": "ACCOUNT",
                    "transaction_type": 2,
                    "excluded_reason": None,
                }
            )
        )
        self.assertFalse(
            is_pure_expense(
                {
                    "source_type": "ACCOUNT",
                    "transaction_type": 1,
                    "excluded_reason": None,
                }
            )
        )
        self.assertFalse(
            is_pure_expense(
                {
                    "source_type": "CARD",
                    "transaction_type": None,
                    "excluded_reason": "INTERNAL_TRANSFER",
                }
            )
        )

    def test_summarize_budget_dashboard_reports_remaining_amount(self):
        summary = summarize_budget_dashboard(
            budget_week={
                "week_start_date": date(2026, 3, 23),
                "week_end_date": date(2026, 3, 29),
                "final_free_amount": 100_000,
                "actual_spent_amount": 40_000,
            },
            today=date(2026, 3, 28),
        )

        self.assertEqual(60_000, summary["remaining_amount"])
        self.assertEqual(2, summary["days_remaining"])
        self.assertEqual(30_000, summary["daily_available"])


if __name__ == "__main__":
    unittest.main()
