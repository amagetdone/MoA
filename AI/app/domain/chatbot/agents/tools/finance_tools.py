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


class _SpendingSummaryToolInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    date_from: date = Field(description="Start date for the spending window.")
    date_to: date = Field(description="End date for the spending window.")


class _SpendingByCategoryToolInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    date_from: date = Field(description="Start date for the category breakdown window.")
    date_to: date = Field(description="End date for the category breakdown window.")
    include_comparison: bool = Field(
        default=False,
        description="Whether to compare the current window with the preceding window.",
    )


class _RecentSpendingAnomaliesToolInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    lookback_days: int = Field(
        default=14,
        ge=1,
        description="How many recent days to inspect for anomalies.",
    )
    limit: int = Field(
        default=5,
        ge=1,
        description="Maximum number of anomalies to return.",
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


def _build_spending_summary_runner(
    *,
    repository: ChatbotRepository,
    user_id: int,
) -> Callable[..., dict[str, Any]]:
    def _run(*, date_from: date, date_to: date) -> dict[str, Any]:
        return repository.get_spending_summary(
            user_id=user_id,
            date_from=_coerce_date(date_from),
            date_to=_coerce_date(date_to),
        )

    return _run


def _build_spending_by_category_runner(
    *,
    repository: ChatbotRepository,
    user_id: int,
) -> Callable[..., dict[str, Any]]:
    def _run(
        *,
        date_from: date,
        date_to: date,
        include_comparison: bool = False,
    ) -> dict[str, Any]:
        return repository.get_spending_by_category(
            user_id=user_id,
            date_from=_coerce_date(date_from),
            date_to=_coerce_date(date_to),
            include_comparison=include_comparison,
        )

    return _run


def _build_recent_spending_anomalies_runner(
    *,
    repository: ChatbotRepository,
    user_id: int,
) -> Callable[..., dict[str, Any]]:
    def _run(*, lookback_days: int = 14, limit: int = 5) -> dict[str, Any]:
        return repository.get_recent_spending_anomalies(
            user_id=user_id,
            lookback_days=lookback_days,
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


def build_spending_summary_tool(
    *,
    repository: ChatbotRepository,
    user_id: int,
) -> StructuredTool:
    return StructuredTool(
        name="get_spending_summary",
        description="Get the authenticated user's spending summary for a date range.",
        args_schema=_SpendingSummaryToolInput,
        func=_build_spending_summary_runner(repository=repository, user_id=user_id),
    )


def build_spending_by_category_tool(
    *,
    repository: ChatbotRepository,
    user_id: int,
) -> StructuredTool:
    return StructuredTool(
        name="get_spending_by_category",
        description="Get the authenticated user's spending breakdown by category.",
        args_schema=_SpendingByCategoryToolInput,
        func=_build_spending_by_category_runner(repository=repository, user_id=user_id),
    )


def build_recent_spending_anomalies_tool(
    *,
    repository: ChatbotRepository,
    user_id: int,
) -> StructuredTool:
    return StructuredTool(
        name="get_recent_spending_anomalies",
        description="Get the authenticated user's recent spending anomalies.",
        args_schema=_RecentSpendingAnomaliesToolInput,
        func=_build_recent_spending_anomalies_runner(repository=repository, user_id=user_id),
    )
