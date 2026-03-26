from __future__ import annotations

from datetime import date

import pytest
from langchain_core.tools import StructuredTool

from app.domain.chatbot.agents.tools.finance_tools import (
    build_budget_tool,
    build_goal_tool,
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
