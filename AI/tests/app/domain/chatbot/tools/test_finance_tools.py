from __future__ import annotations

from datetime import date

import pytest
from langchain_core.tools import StructuredTool

from app.domain.chatbot.agents.tools.finance_tools import (
    build_budget_tool,
    build_goal_tool,
    build_recent_spending_anomalies_tool,
    build_spending_by_category_tool,
    build_spending_summary_tool,
)


class _FakeRepository:
    def __init__(self) -> None:
        self.calls: list[dict[str, object]] = []

    def get_weekly_budget_status(self, *, user_id: int, base_date):
        self.calls.append(
            {
                "method": "get_weekly_budget_status",
                "user_id": user_id,
                "base_date": base_date,
            }
        )
        return {"summary": {"final_free_amount": 1}, "evidence": [], "warnings": []}

    def get_goal_progress_overview(
        self,
        *,
        user_id: int,
        status_filter=None,
        limit=5,
    ):
        self.calls.append(
            {
                "method": "get_goal_progress_overview",
                "user_id": user_id,
                "status_filter": status_filter,
                "limit": limit,
            }
        )
        return {"summary": {"goal_count": 0, "plans": []}, "evidence": [], "warnings": []}

    def get_spending_summary(
        self,
        *,
        user_id: int,
        date_from,
        date_to,
    ):
        self.calls.append(
            {
                "method": "get_spending_summary",
                "user_id": user_id,
                "date_from": date_from,
                "date_to": date_to,
            }
        )
        return {"summary": {"total_spent": 230000}, "evidence": [], "warnings": []}

    def get_spending_by_category(
        self,
        *,
        user_id: int,
        date_from,
        date_to,
        include_comparison,
    ):
        self.calls.append(
            {
                "method": "get_spending_by_category",
                "user_id": user_id,
                "date_from": date_from,
                "date_to": date_to,
                "include_comparison": include_comparison,
            }
        )
        return {
            "summary": {"total_spent": 230000, "category_count": 1},
            "evidence": [],
            "warnings": [],
        }

    def get_recent_spending_anomalies(
        self,
        *,
        user_id: int,
        lookback_days,
        limit,
    ):
        self.calls.append(
            {
                "method": "get_recent_spending_anomalies",
                "user_id": user_id,
                "lookback_days": lookback_days,
                "limit": limit,
            }
        )
        return {
            "summary": {"anomaly_count": 1, "lookback_days": lookback_days, "limit": limit},
            "evidence": [],
            "warnings": [],
        }


def test_build_budget_tool_binds_trusted_user_id() -> None:
    repository = _FakeRepository()
    tool = build_budget_tool(repository=repository, user_id=7)

    assert isinstance(tool, StructuredTool)
    assert "user_id" not in tool.args_schema.model_fields

    result = tool.invoke({"base_date": "2026-03-26"})

    assert result["summary"]["final_free_amount"] == 1
    assert repository.calls == [
        {
            "method": "get_weekly_budget_status",
            "user_id": 7,
            "base_date": date(2026, 3, 26),
        }
    ]


def test_build_budget_tool_rejects_caller_supplied_user_id() -> None:
    repository = _FakeRepository()
    tool = build_budget_tool(repository=repository, user_id=7)

    with pytest.raises(Exception):
        tool.invoke({"base_date": "2026-03-26", "user_id": 99})


def test_build_goal_tool_binds_trusted_user_id() -> None:
    repository = _FakeRepository()
    tool = build_goal_tool(repository=repository, user_id=7)

    assert isinstance(tool, StructuredTool)
    assert "user_id" not in tool.args_schema.model_fields

    result = tool.invoke({"status_filter": "ACTIVE", "limit": 3})

    assert result["summary"]["goal_count"] == 0
    assert repository.calls == [
        {
            "method": "get_goal_progress_overview",
            "user_id": 7,
            "status_filter": "ACTIVE",
            "limit": 3,
        }
    ]


def test_build_goal_tool_rejects_caller_supplied_user_id() -> None:
    repository = _FakeRepository()
    tool = build_goal_tool(repository=repository, user_id=7)

    with pytest.raises(Exception):
        tool.invoke({"status_filter": "ACTIVE", "limit": 3, "user_id": 99})


def test_build_spending_summary_tool_binds_trusted_user_id() -> None:
    repository = _FakeRepository()
    tool = build_spending_summary_tool(repository=repository, user_id=7)

    assert isinstance(tool, StructuredTool)
    assert "user_id" not in tool.args_schema.model_fields

    result = tool.invoke(
        {
            "date_from": "2026-03-01",
            "date_to": "2026-03-26",
        }
    )

    assert result["summary"]["total_spent"] == 230000
    assert repository.calls == [
        {
            "method": "get_spending_summary",
            "user_id": 7,
            "date_from": date(2026, 3, 1),
            "date_to": date(2026, 3, 26),
        }
    ]


def test_build_spending_summary_tool_rejects_caller_supplied_user_id() -> None:
    repository = _FakeRepository()
    tool = build_spending_summary_tool(repository=repository, user_id=7)

    with pytest.raises(Exception):
        tool.invoke(
            {
                "date_from": "2026-03-01",
                "date_to": "2026-03-26",
                "user_id": 99,
            }
        )


def test_build_spending_by_category_tool_binds_trusted_user_id() -> None:
    repository = _FakeRepository()
    tool = build_spending_by_category_tool(repository=repository, user_id=7)

    assert isinstance(tool, StructuredTool)
    assert "user_id" not in tool.args_schema.model_fields

    result = tool.invoke(
        {
            "date_from": "2026-03-01",
            "date_to": "2026-03-26",
            "include_comparison": True,
        }
    )

    assert result["summary"]["total_spent"] == 230000
    assert repository.calls == [
        {
            "method": "get_spending_by_category",
            "user_id": 7,
            "date_from": date(2026, 3, 1),
            "date_to": date(2026, 3, 26),
            "include_comparison": True,
        }
    ]


def test_build_spending_by_category_tool_rejects_caller_supplied_user_id() -> None:
    repository = _FakeRepository()
    tool = build_spending_by_category_tool(repository=repository, user_id=7)

    with pytest.raises(Exception):
        tool.invoke(
            {
                "date_from": "2026-03-01",
                "date_to": "2026-03-26",
                "include_comparison": True,
                "user_id": 99,
            }
        )


def test_build_recent_spending_anomalies_tool_binds_trusted_user_id() -> None:
    repository = _FakeRepository()
    tool = build_recent_spending_anomalies_tool(repository=repository, user_id=7)

    assert isinstance(tool, StructuredTool)
    assert "user_id" not in tool.args_schema.model_fields

    result = tool.invoke({"lookback_days": 14, "limit": 3})

    assert result["summary"]["anomaly_count"] == 1
    assert repository.calls == [
        {
            "method": "get_recent_spending_anomalies",
            "user_id": 7,
            "lookback_days": 14,
            "limit": 3,
        }
    ]


def test_build_recent_spending_anomalies_tool_rejects_caller_supplied_user_id() -> None:
    repository = _FakeRepository()
    tool = build_recent_spending_anomalies_tool(repository=repository, user_id=7)

    with pytest.raises(Exception):
        tool.invoke({"lookback_days": 14, "limit": 3, "user_id": 99})
