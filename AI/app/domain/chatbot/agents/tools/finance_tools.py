from __future__ import annotations

from datetime import date, datetime, timedelta
from typing import Any, Callable

from langchain_core.tools import StructuredTool
from pydantic import BaseModel, ConfigDict, Field

from app.domain.chatbot.repository.chatbot_repository import ChatbotRepository


DEFAULT_READ_ONLY_PERIOD = "last_7_days"
SUPPORTED_READ_ONLY_PERIODS = (
    "this_week",
    "last_7_days",
    "this_month",
    "today",
    "yesterday",
)


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

    base_date: date | None = Field(
        default=None,
        description="Optional base date for the weekly budget lookup. Defaults to today.",
    )


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

    period: str | None = Field(
        default=None,
        description=(
            "Optional named safe read-only period. Supported values: "
            "this_week, last_7_days, this_month, today, yesterday."
        ),
    )
    date_from: date | None = Field(
        default=None,
        description="Optional start date for the spending window.",
    )
    date_to: date | None = Field(
        default=None,
        description="Optional end date for the spending window.",
    )


class _SpendingByCategoryToolInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    period: str | None = Field(
        default=None,
        description=(
            "Optional named safe read-only period. Supported values: "
            "this_week, last_7_days, this_month, today, yesterday."
        ),
    )
    date_from: date | None = Field(
        default=None,
        description="Optional start date for the category breakdown window.",
    )
    date_to: date | None = Field(
        default=None,
        description="Optional end date for the category breakdown window.",
    )
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
    today_factory: Callable[[], date],
) -> Callable[..., dict[str, Any]]:
    def _run(*, base_date: date | None = None) -> dict[str, Any]:
        return repository.get_weekly_budget_status(
            user_id=user_id,
            base_date=_resolve_budget_base_date(
                base_date=base_date,
                today_factory=today_factory,
            ),
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
    today_factory: Callable[[], date],
) -> Callable[..., dict[str, Any]]:
    def _run(
        *,
        period: str | None = None,
        date_from: date | None = None,
        date_to: date | None = None,
    ) -> dict[str, Any]:
        resolved_date_from, resolved_date_to = _resolve_read_only_window(
            period=period,
            date_from=date_from,
            date_to=date_to,
            today_factory=today_factory,
        )
        return repository.get_spending_summary(
            user_id=user_id,
            date_from=resolved_date_from,
            date_to=resolved_date_to,
        )

    return _run


def _build_spending_by_category_runner(
    *,
    repository: ChatbotRepository,
    user_id: int,
    today_factory: Callable[[], date],
) -> Callable[..., dict[str, Any]]:
    def _run(
        *,
        period: str | None = None,
        date_from: date | None = None,
        date_to: date | None = None,
        include_comparison: bool = False,
    ) -> dict[str, Any]:
        resolved_date_from, resolved_date_to = _resolve_read_only_window(
            period=period,
            date_from=date_from,
            date_to=date_to,
            today_factory=today_factory,
        )
        return repository.get_spending_by_category(
            user_id=user_id,
            date_from=resolved_date_from,
            date_to=resolved_date_to,
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


def _resolve_budget_base_date(
    *,
    base_date: date | None,
    today_factory: Callable[[], date],
) -> date:
    if base_date is None:
        return today_factory()
    return _coerce_date(base_date)


def _normalize_period(period: str | None) -> str | None:
    if period is None:
        return None

    normalized = period.strip().casefold().replace("-", "_").replace(" ", "_")
    if not normalized:
        return None
    return normalized


def _resolve_period_window(
    *,
    period: str,
    today: date,
) -> tuple[date, date]:
    if period == "this_week":
        return today - timedelta(days=today.weekday()), today
    if period == "last_7_days":
        return today - timedelta(days=6), today
    if period == "this_month":
        return today.replace(day=1), today
    if period == "today":
        return today, today
    if period == "yesterday":
        yesterday = today - timedelta(days=1)
        return yesterday, yesterday
    raise ValueError(
        "Unsupported period. Use one of: "
        + ", ".join(SUPPORTED_READ_ONLY_PERIODS)
    )


def _resolve_read_only_window(
    *,
    period: str | None,
    date_from: date | None,
    date_to: date | None,
    today_factory: Callable[[], date],
) -> tuple[date, date]:
    if date_from is not None and date_to is not None:
        return _coerce_date(date_from), _coerce_date(date_to)

    if date_from is not None or date_to is not None:
        raise ValueError("date_from and date_to must be provided together")

    today = today_factory()
    normalized_period = _normalize_period(period) or DEFAULT_READ_ONLY_PERIOD
    return _resolve_period_window(period=normalized_period, today=today)


def build_budget_tool(
    *,
    repository: ChatbotRepository,
    user_id: int,
    today_factory: Callable[[], date] | None = None,
) -> StructuredTool:
    return StructuredTool(
        name="get_weekly_budget_status",
        description=(
            "Get the authenticated user's weekly budget snapshot for a given base date. "
            "If base_date is omitted, use today."
        ),
        args_schema=_BudgetToolInput,
        func=_build_budget_runner(
            repository=repository,
            user_id=user_id,
            today_factory=today_factory or date.today,
        ),
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
    today_factory: Callable[[], date] | None = None,
) -> StructuredTool:
    return StructuredTool(
        name="get_spending_summary",
        description=(
            "Get the authenticated user's spending summary for a date range. "
            "You may provide date_from and date_to together, or provide period as "
            "this_week, last_7_days, this_month, today, or yesterday. "
            "If no range is provided, default to last_7_days."
        ),
        args_schema=_SpendingSummaryToolInput,
        func=_build_spending_summary_runner(
            repository=repository,
            user_id=user_id,
            today_factory=today_factory or date.today,
        ),
    )


def build_spending_by_category_tool(
    *,
    repository: ChatbotRepository,
    user_id: int,
    today_factory: Callable[[], date] | None = None,
) -> StructuredTool:
    return StructuredTool(
        name="get_spending_by_category",
        description=(
            "Get the authenticated user's spending breakdown by category. "
            "You may provide date_from and date_to together, or provide period as "
            "this_week, last_7_days, this_month, today, or yesterday. "
            "If no range is provided, default to last_7_days."
        ),
        args_schema=_SpendingByCategoryToolInput,
        func=_build_spending_by_category_runner(
            repository=repository,
            user_id=user_id,
            today_factory=today_factory or date.today,
        ),
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
