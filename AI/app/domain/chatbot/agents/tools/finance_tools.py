from __future__ import annotations

from datetime import date, datetime
from typing import Any, Callable

from langchain_core.tools import StructuredTool
from pydantic import BaseModel, ConfigDict, Field

from app.domain.chatbot.repository.chatbot_repository import ChatbotRepository


def _coerce_date(value: Any) -> date:
    if isinstance(value, date) and not isinstance(value, datetime):
        return value
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, str):
        return date.fromisoformat(value)
    raise ValueError("base_date must be a date or ISO-8601 date string")


class _BudgetToolInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    base_date: date = Field(description="Base date for the weekly budget lookup.")


class _GoalToolInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    status_filter: str | None = Field(
        default=None,
        description="Optional plan status filter, such as ACTIVE.",
    )
    limit: int = Field(
        default=5,
        ge=1,
        description="Maximum number of plans to return.",
    )


def _build_budget_runner(
    *,
    repository: ChatbotRepository,
    user_id: int,
) -> Callable[..., dict[str, Any]]:
    def _run(*, base_date: date) -> dict[str, Any]:
        return repository.get_weekly_budget_status(
            user_id=user_id,
            base_date=_coerce_date(base_date),
        )

    return _run


def _build_goal_runner(
    *,
    repository: ChatbotRepository,
    user_id: int,
) -> Callable[..., dict[str, Any]]:
    def _run(*, status_filter: str | None = None, limit: int = 5) -> dict[str, Any]:
        return repository.get_goal_progress_overview(
            user_id=user_id,
            status_filter=status_filter,
            limit=limit,
        )

    return _run


def build_budget_tool(*, repository: ChatbotRepository, user_id: int) -> StructuredTool:
    return StructuredTool(
        name="get_weekly_budget_status",
        description=(
            "Get the authenticated user's weekly budget snapshot for a given base date."
        ),
        args_schema=_BudgetToolInput,
        func=_build_budget_runner(repository=repository, user_id=user_id),
    )


def build_goal_tool(*, repository: ChatbotRepository, user_id: int) -> StructuredTool:
    return StructuredTool(
        name="get_goal_progress_overview",
        description="Get the authenticated user's goal progress overview.",
        args_schema=_GoalToolInput,
        func=_build_goal_runner(repository=repository, user_id=user_id),
    )
