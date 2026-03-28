from __future__ import annotations

import os
import re
from collections import defaultdict
from contextlib import contextmanager
from dataclasses import dataclass
from datetime import date, datetime, time, timedelta, timezone
from typing import Any, Callable, Iterable, Mapping

try:
    from langchain_core.tools import tool as _langchain_tool
except ImportError:  # pragma: no cover
    _langchain_tool = None

try:
    from sqlalchemy import create_engine as _sqlalchemy_create_engine
    from sqlalchemy import text as _sql_text
except ImportError:  # pragma: no cover
    _sqlalchemy_create_engine = None
    _sql_text = None


KST = timezone(timedelta(hours=9))
ISO_YEAR_WEEK_PATTERN = re.compile(r"^(?P<year>\d{4})-W(?P<week>\d{2})$")
MONTH_PATTERN = re.compile(r"^(?P<year>\d{4})-(?P<month>\d{2})$")
TRANSACTION_WEEK_PATTERN = re.compile(r"^(?P<year>\d{4})-(?P<month>\d{2})-(?P<week>\d{1,2})$")
ACCOUNT_ACTIVITY_LIMIT = 5
CARD_ACTIVITY_LIMIT = 5
TRANSACTION_SAMPLE_LIMIT = 5


class ToolRuntimeError(RuntimeError):
    pass


class MissingDependencyError(ToolRuntimeError):
    pass


class DataNotFoundError(ToolRuntimeError):
    pass


@dataclass(frozen=True)
class YearMonthValue:
    year: int
    month: int

    def __str__(self) -> str:
        return f"{self.year:04d}-{self.month:02d}"

    @property
    def first_day(self) -> date:
        return date(self.year, self.month, 1)

    @property
    def last_day(self) -> date:
        if self.month == 12:
            return date(self.year + 1, 1, 1) - timedelta(days=1)
        return date(self.year, self.month + 1, 1) - timedelta(days=1)


class _FallbackTool:
    def __init__(self, func: Callable[..., Any]) -> None:
        self.func = func
        self.__name__ = func.__name__
        self.name = func.__name__
        self.__doc__ = func.__doc__

    def __call__(self, *args: Any, **kwargs: Any) -> Any:
        return self.func(*args, **kwargs)

    def invoke(self, payload: Mapping[str, Any] | Any) -> Any:
        if isinstance(payload, Mapping):
            return self.func(**dict(payload))
        return self.func(payload)


def tool(func: Callable[..., Any]) -> Any:
    if _langchain_tool is None:
        return _FallbackTool(func)
    return _langchain_tool(func)


def now_kst() -> datetime:
    return datetime.now(tz=KST)


def add_warning(warnings: list[str], message: str | None) -> None:
    if message and message not in warnings:
        warnings.append(message)


def ensure_int(value: Any) -> int:
    if isinstance(value, bool):
        return int(value)
    return 0 if value is None else int(value)


def ensure_float(value: Any) -> float:
    return 0.0 if value is None else float(value)


def ensure_text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def coerce_bool(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        return value.strip().lower() in {"true", "1", "yes", "y"}
    return bool(value)


def to_iso(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, datetime):
        if value.tzinfo is None:
            return value.replace(tzinfo=KST).isoformat()
        return value.isoformat()
    if isinstance(value, date):
        return value.isoformat()
    return str(value)


def parse_year_month(value: str | None) -> YearMonthValue:
    if value is None:
        today = now_kst().date()
        return YearMonthValue(today.year, today.month)
    match = MONTH_PATTERN.match(value)
    if not match:
        raise ValueError(f"Invalid year_month: {value}. Expected YYYY-MM.")
    year = int(match.group("year"))
    month = int(match.group("month"))
    if month < 1 or month > 12:
        raise ValueError(f"Invalid year_month: {value}.")
    return YearMonthValue(year, month)


def parse_iso_year_week(value: str) -> date:
    match = ISO_YEAR_WEEK_PATTERN.match(value)
    if not match:
        raise ValueError(f"Invalid year_week: {value}. Expected YYYY-Www.")
    return date.fromisocalendar(int(match.group("year")), int(match.group("week")), 1)


def parse_transaction_year_week(value: str) -> tuple[str, date]:
    iso_match = ISO_YEAR_WEEK_PATTERN.match(value)
    if iso_match:
        week_start = date.fromisocalendar(int(iso_match.group("year")), int(iso_match.group("week")), 1)
        iso_year, iso_week, _ = week_start.isocalendar()
        return f"{iso_year}-W{iso_week:02d}", week_start

    match = TRANSACTION_WEEK_PATTERN.match(value)
    if not match:
        raise ValueError(f"Invalid transaction year_week: {value}. Expected YYYY-MM-w or YYYY-Www.")

    year = int(match.group("year"))
    month = int(match.group("month"))
    week_index = int(match.group("week"))
    if week_index <= 0:
        raise ValueError(f"Invalid transaction year_week: {value}.")

    cursor = date(year, month, 1)
    while cursor.weekday() != 0:
        cursor += timedelta(days=1)
    week_start = cursor + timedelta(weeks=week_index - 1)
    if week_start.month != month:
        raise ValueError(f"Invalid transaction year_week: {value}.")
    return f"{year:04d}-{month:02d}-{week_index}", week_start


def resolve_transaction_window(from_date: date | str | None, to_date: date | str | None) -> tuple[date, date]:
    resolved_to = parse_date_value(to_date) or now_kst().date()
    resolved_from = parse_date_value(from_date)
    if resolved_from is None:
        month = resolved_to.month - 1 or 12
        year = resolved_to.year - 1 if resolved_to.month == 1 else resolved_to.year
        day = min(resolved_to.day, YearMonthValue(year, month).last_day.day)
        resolved_from = date(year, month, day)
    if resolved_from > resolved_to:
        raise ValueError("from_date must be on or before to_date.")
    return resolved_from, resolved_to


def parse_date_value(value: date | str | None) -> date | None:
    if value is None or value == "":
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value))


def build_tool_response(
    tool_name: str,
    summary: dict[str, Any],
    evidence: list[dict[str, Any]],
    warnings: list[str],
    raw: dict[str, Any],
    window: dict[str, Any] | None = None,
) -> dict[str, Any]:
    return {
        "tool_name": tool_name,
        "as_of": now_kst().isoformat(),
        "window": window or {},
        "summary": summary,
        "evidence": evidence,
        "warnings": warnings,
        "raw": raw,
    }


def summarize_asset_linkage(
    accounts: list[dict[str, Any]],
    cards: list[dict[str, Any]],
    recent_activities: list[dict[str, Any]],
) -> dict[str, Any]:
    latest_activity = None
    if recent_activities:
        latest_activity = max((ensure_text(item.get("occurred_at")) for item in recent_activities), default=None)

    return {
        "linked_account_count": len(accounts),
        "active_account_count": sum(1 for item in accounts if coerce_bool(item.get("is_active"))),
        "verified_account_count": sum(1 for item in accounts if coerce_bool(item.get("is_verified"))),
        "linked_card_count": len(cards),
        "latest_activity_at": latest_activity,
    }


def is_pure_expense(projection: Mapping[str, Any]) -> bool:
    if projection.get("excluded_reason") in {"INCOME", "INTERNAL_TRANSFER"}:
        return False
    if projection.get("source_type") == "ACCOUNT":
        return ensure_int(projection.get("transaction_type")) != 1
    return True


def is_income(projection: Mapping[str, Any]) -> bool:
    if projection.get("source_type") == "ACCOUNT":
        return ensure_int(projection.get("transaction_type")) == 1
    return projection.get("excluded_reason") == "INCOME"


def is_analysis_included(projection: Mapping[str, Any]) -> bool:
    return coerce_bool(projection.get("analysis_included"))


def is_non_essential_free_spending(projection: Mapping[str, Any]) -> bool:
    return (not is_analysis_included(projection)) and projection.get("excluded_reason") == "NON_ESSENTIAL"


def resolve_expense_scope(projection: Mapping[str, Any]) -> str:
    return ensure_text(projection.get("expense_scope")) or "VARIABLE"


def summarize_budget_dashboard(budget_week: Mapping[str, Any], today: date | None = None) -> dict[str, Any]:
    current_date = today or now_kst().date()
    week_end_date = budget_week["week_end_date"]
    final_free_amount = ensure_int(budget_week.get("final_free_amount"))
    actual_spent_amount = ensure_int(budget_week.get("actual_spent_amount"))
    remaining_amount = final_free_amount - actual_spent_amount
    days_remaining = max(0, (week_end_date - current_date).days + 1)
    daily_available = remaining_amount // days_remaining if days_remaining > 0 else 0

    return {
        "week_start_date": to_iso(budget_week.get("week_start_date")),
        "week_end_date": to_iso(week_end_date),
        "days_remaining": days_remaining,
        "final_free_amount": final_free_amount,
        "actual_spent_amount": actual_spent_amount,
        "remaining_amount": remaining_amount,
        "daily_available": daily_available,
    }


def _require_sql_support() -> None:
    if _sqlalchemy_create_engine is None or _sql_text is None:
        raise MissingDependencyError(
            "tools.py requires sqlalchemy to query PostgreSQL. Install sqlalchemy and a PostgreSQL driver in the LangGraph server environment."
        )


def _jdbc_to_sqlalchemy_url(jdbc_url: str) -> str:
    normalized = jdbc_url.strip()
    if normalized.startswith("jdbc:postgresql://"):
        return "postgresql+psycopg://" + normalized[len("jdbc:postgresql://") :]
    if normalized.startswith("postgresql://") or normalized.startswith("postgresql+psycopg://"):
        return normalized
    return normalized


def create_db_engine() -> Any:
    _require_sql_support()
    db_url = ensure_text(os.getenv("DB_URL"))
    db_username = ensure_text(os.getenv("DB_USERNAME"))
    db_password = ensure_text(os.getenv("DB_PASSWORD"))
    if not db_url or not db_username or db_password is None:
        raise ToolRuntimeError("DB_URL, DB_USERNAME, and DB_PASSWORD environment variables must be set.")
    return _sqlalchemy_create_engine(
        _jdbc_to_sqlalchemy_url(db_url),
        future=True,
        pool_pre_ping=True,
        connect_args={"user": db_username, "password": db_password},
    )


_ENGINE: Any | None = None


def get_engine() -> Any:
    global _ENGINE
    if _ENGINE is None:
        _ENGINE = create_db_engine()
    return _ENGINE


@contextmanager
def db_connection() -> Iterable[Any]:
    engine = get_engine()
    with engine.connect() as connection:
        yield connection


def fetch_all(connection: Any, sql: str, params: Mapping[str, Any] | None = None) -> list[dict[str, Any]]:
    result = connection.execute(_sql_text(sql), params or {})
    return [dict(row._mapping) for row in result]


def fetch_one(connection: Any, sql: str, params: Mapping[str, Any] | None = None) -> dict[str, Any] | None:
    result = connection.execute(_sql_text(sql), params or {})
    row = result.fetchone()
    return None if row is None else dict(row._mapping)


def _window_payload(date_from: Any = None, date_to: Any = None, **extra: Any) -> dict[str, Any]:
    payload = {}
    if date_from is not None:
        payload["date_from"] = to_iso(date_from)
    if date_to is not None:
        payload["date_to"] = to_iso(date_to)
    for key, value in extra.items():
        if value is not None:
            payload[key] = to_iso(value) if isinstance(value, (date, datetime)) else value
    return payload


def fetch_linked_accounts_rows(connection: Any, user_id: int) -> list[dict[str, Any]]:
    return fetch_all(
        connection,
        """
        SELECT
            a.id,
            a.account_no,
            a.account_name,
            a.account_type_name,
            a.alias,
            a.bank_code,
            a.bank_name,
            a.currency,
            a.is_verified,
            a.is_active,
            a.created_at,
            a.updated_at
        FROM account a
        WHERE a.users_id = :user_id
          AND a.is_deleted = FALSE
        ORDER BY a.is_verified DESC, a.is_active DESC, a.account_no ASC
        """,
        {"user_id": user_id},
    )


def fetch_linked_cards_rows(connection: Any, user_id: int) -> list[dict[str, Any]]:
    return fetch_all(
        connection,
        """
        SELECT
            c.cardUniqueNo AS card_unique_no,
            c.cardNo AS card_no,
            c.name AS card_name,
            c.cardIssuerName AS card_issuer_name,
            c.withdrawalAccountNo AS linked_account_no,
            c.withdrawalDate AS withdrawal_date,
            c.cardExpiryDate AS card_expiry_date
        FROM card c
        JOIN account a ON a.id = c.withdrawalAccountNo
        WHERE a.users_id = :user_id
          AND a.is_deleted = FALSE
          AND a.is_verified = TRUE
          AND a.is_active = TRUE
        ORDER BY c.cardUniqueNo ASC
        """,
        {"user_id": user_id},
    )


def fetch_recent_account_activity_rows(connection: Any, user_id: int, limit: int = ACCOUNT_ACTIVITY_LIMIT) -> list[dict[str, Any]]:
    return fetch_all(
        connection,
        """
        SELECT
            'ACCOUNT' AS source_type,
            ato.account_id AS source_id,
            t.id AS transaction_id,
            t.transactionDateTime AS occurred_at,
            t.transactionBalance AS amount,
            ato.transactionSummary AS merchant_text,
            ato.transactionType AS transaction_type,
            ato.transactionTypeName AS transaction_type_name,
            ato.transactionAfterBalance AS transaction_after_balance
        FROM account_transaction_override ato
        JOIN transaction_override t ON t.id = ato.id
        JOIN account a ON a.id = ato.account_id
        WHERE a.users_id = :user_id
          AND a.is_deleted = FALSE
        ORDER BY t.transactionDateTime DESC
        LIMIT :limit
        """,
        {"user_id": user_id, "limit": limit},
    )


def fetch_recent_card_activity_rows(connection: Any, user_id: int, limit: int = CARD_ACTIVITY_LIMIT) -> list[dict[str, Any]]:
    return fetch_all(
        connection,
        """
        SELECT
            'CARD' AS source_type,
            cto.cardUniqueNo::text AS source_id,
            t.id AS transaction_id,
            t.transactionDateTime AS occurred_at,
            t.transactionBalance AS amount,
            COALESCE(m.name, t.transactionMemo) AS merchant_text,
            NULL AS transaction_type,
            NULL AS transaction_type_name,
            NULL AS transaction_after_balance
        FROM card_transaction_override cto
        JOIN transaction_override t ON t.id = cto.id
        JOIN card c ON c.cardUniqueNo = cto.cardUniqueNo
        JOIN account a ON a.id = c.withdrawalAccountNo
        LEFT JOIN merchant m ON m.id = cto.merchant_id
        WHERE a.users_id = :user_id
          AND a.is_deleted = FALSE
        ORDER BY t.transactionDateTime DESC
        LIMIT :limit
        """,
        {"user_id": user_id, "limit": limit},
    )


def fetch_account_estimated_balances(connection: Any, user_id: int) -> dict[str, int]:
    rows = fetch_all(
        connection,
        """
        SELECT DISTINCT ON (ato.account_id)
            ato.account_id,
            ato.transactionAfterBalance AS estimated_balance
        FROM account_transaction_override ato
        JOIN transaction_override t ON t.id = ato.id
        JOIN account a ON a.id = ato.account_id
        WHERE a.users_id = :user_id
          AND a.is_deleted = FALSE
        ORDER BY ato.account_id, t.transactionDateTime DESC
        """,
        {"user_id": user_id},
    )
    return {str(row["account_id"]): ensure_int(row["estimated_balance"]) for row in rows}


def fetch_account_detail_row(connection: Any, user_id: int, account_no: str) -> dict[str, Any] | None:
    return fetch_one(
        connection,
        """
        SELECT
            a.id,
            a.account_no,
            a.account_name,
            a.account_type_name,
            a.alias,
            a.bank_code,
            a.bank_name,
            a.currency,
            a.is_verified,
            a.is_active,
            a.created_at,
            a.updated_at
        FROM account a
        WHERE a.users_id = :user_id
          AND a.account_no = :account_no
          AND a.is_deleted = FALSE
        """,
        {"user_id": user_id, "account_no": account_no},
    )


def fetch_latest_analysis_metadata(connection: Any, user_id: int) -> dict[str, Any] | None:
    return fetch_one(
        connection,
        """
        SELECT
            tam.id,
            tam.requested_months,
            tam.window_start_at,
            tam.window_end_at,
            tam.transaction_count,
            tam.latest_transaction_at,
            tam.analyzed_at
        FROM transaction_analysis_metadata tam
        WHERE tam.users_id = :user_id
          AND tam.is_deleted = FALSE
        ORDER BY tam.analyzed_at DESC
        LIMIT 1
        """,
        {"user_id": user_id},
    )


def fetch_transaction_projections(
    connection: Any,
    user_id: int,
    from_datetime: datetime,
    to_datetime: datetime,
    analyzed_at: datetime | None = None,
) -> list[dict[str, Any]]:
    params: dict[str, Any] = {
        "user_id": user_id,
        "from_datetime": from_datetime,
        "to_datetime": to_datetime,
    }
    analysis_join_predicate = ""
    if analyzed_at is not None:
        analysis_join_predicate = "AND tar.analyzed_at = :analyzed_at"
        params["analyzed_at"] = analyzed_at

    analysis_subquery = f"""
        SELECT
            tar.transaction_override_id,
            tar.category_id,
            cat.name AS analysis_category_name,
            tar.analysis_included,
            tar.excluded_reason,
            tar.expense_scope,
            tar.raw_label,
            tar.final_label,
            tar.confidence,
            tar.fallback_applied,
            tar.threshold_used,
            tar.analyzed_at,
            ROW_NUMBER() OVER (
                PARTITION BY tar.transaction_override_id
                ORDER BY tar.analyzed_at DESC, tar.id DESC
            ) AS row_num
        FROM transaction_analysis_result tar
        JOIN category cat ON cat.id = tar.category_id
        WHERE tar.is_deleted = FALSE
          {analysis_join_predicate}
    """

    return fetch_all(
        connection,
        f"""
        WITH latest_analysis AS (
            {analysis_subquery}
        ),
        account_transactions AS (
            SELECT
                'ACCOUNT' AS source_type,
                a.id AS source_id,
                t.id AS transaction_id,
                t.transactionDateTime AS occurred_at,
                t.transactionBalance AS amount,
                t.transactionMemo AS transaction_memo,
                base_category.id AS base_category_id,
                base_category.name AS base_category_name,
                la.category_id AS analysis_category_id,
                la.analysis_category_name,
                la.analysis_included,
                la.excluded_reason,
                la.expense_scope,
                la.raw_label,
                la.final_label,
                la.confidence,
                la.fallback_applied,
                la.threshold_used,
                la.analyzed_at,
                ato.transactionType AS transaction_type,
                ato.transactionTypeName AS transaction_type_name,
                ato.transactionAfterBalance AS transaction_after_balance,
                ato.transactionSummary AS merchant_text
            FROM account_transaction_override ato
            JOIN transaction_override t ON t.id = ato.id
            JOIN account a ON a.id = ato.account_id
            JOIN category base_category ON base_category.id = t.category_id
            LEFT JOIN latest_analysis la ON la.transaction_override_id = t.id AND la.row_num = 1
            WHERE a.users_id = :user_id
              AND a.is_deleted = FALSE
              AND t.transactionDateTime BETWEEN :from_datetime AND :to_datetime
        ),
        card_transactions AS (
            SELECT
                'CARD' AS source_type,
                c.cardUniqueNo::text AS source_id,
                t.id AS transaction_id,
                t.transactionDateTime AS occurred_at,
                t.transactionBalance AS amount,
                t.transactionMemo AS transaction_memo,
                base_category.id AS base_category_id,
                base_category.name AS base_category_name,
                la.category_id AS analysis_category_id,
                la.analysis_category_name,
                la.analysis_included,
                la.excluded_reason,
                la.expense_scope,
                la.raw_label,
                la.final_label,
                la.confidence,
                la.fallback_applied,
                la.threshold_used,
                la.analyzed_at,
                NULL AS transaction_type,
                NULL AS transaction_type_name,
                NULL AS transaction_after_balance,
                COALESCE(m.name, t.transactionMemo) AS merchant_text
            FROM card_transaction_override cto
            JOIN transaction_override t ON t.id = cto.id
            JOIN card c ON c.cardUniqueNo = cto.cardUniqueNo
            JOIN account a ON a.id = c.withdrawalAccountNo
            JOIN category base_category ON base_category.id = t.category_id
            LEFT JOIN merchant m ON m.id = cto.merchant_id
            LEFT JOIN latest_analysis la ON la.transaction_override_id = t.id AND la.row_num = 1
            WHERE a.users_id = :user_id
              AND a.is_deleted = FALSE
              AND t.transactionDateTime BETWEEN :from_datetime AND :to_datetime
        )
        SELECT *
        FROM (
            SELECT * FROM account_transactions
            UNION ALL
            SELECT * FROM card_transactions
        ) merged
        ORDER BY occurred_at DESC, transaction_id DESC
        """,
        params,
    )


def fetch_current_budget_week(connection: Any, user_id: int) -> dict[str, Any] | None:
    today = now_kst().date()
    return fetch_one(
        connection,
        """
        SELECT *
        FROM budget_week bw
        WHERE bw.users_id = :user_id
          AND bw.is_deleted = FALSE
          AND bw.week_start_date <= :today
          AND bw.week_end_date >= :today
        ORDER BY bw.id DESC
        LIMIT 1
        """,
        {"user_id": user_id, "today": today},
    )


def fetch_budget_week_by_start(connection: Any, user_id: int, week_start_date: date) -> dict[str, Any] | None:
    return fetch_one(
        connection,
        """
        SELECT *
        FROM budget_week bw
        WHERE bw.users_id = :user_id
          AND bw.is_deleted = FALSE
          AND bw.week_start_date <= :week_start_date
          AND bw.week_end_date >= :week_start_date
        ORDER BY bw.week_sequence ASC
        LIMIT 1
        """,
        {"user_id": user_id, "week_start_date": week_start_date},
    )


def fetch_budget_income_row(connection: Any, user_id: int) -> dict[str, Any] | None:
    first_day = now_kst().date().replace(day=1)
    return fetch_one(
        connection,
        """
        SELECT bm.total_income_amount, bm.base_date
        FROM budget_month bm
        WHERE bm.users_id = :user_id
          AND bm.is_deleted = FALSE
          AND bm.base_date = :base_date
        ORDER BY bm.id DESC
        LIMIT 1
        """,
        {"user_id": user_id, "base_date": first_day},
    )


def fetch_active_plans(connection: Any, user_id: int, limit: int | None = None) -> list[dict[str, Any]]:
    sql = """
        SELECT
            p.id,
            p.title,
            p.description,
            p.target_amount,
            p.saved_amount,
            p.start_date,
            p.end_date,
            p.target_weeks,
            p.priority,
            p.status,
            p.auto_allocate_enabled,
            p.plan_img_url
        FROM plan p
        WHERE p.users_id = :user_id
          AND p.is_deleted = FALSE
          AND p.status = 'ACTIVE'
        ORDER BY p.priority ASC, p.title ASC
    """
    params: dict[str, Any] = {"user_id": user_id}
    if limit is not None:
        sql += " LIMIT :limit"
        params["limit"] = limit
    return fetch_all(connection, sql, params)


def fetch_plan_allocations_for_budget_week(connection: Any, budget_week_id: int) -> list[dict[str, Any]]:
    return fetch_all(
        connection,
        """
        SELECT
            pwa.id,
            pwa.plan_id,
            pwa.budget_week_id,
            pwa.allocation_type,
            pwa.planned_amount,
            pwa.final_amount,
            pwa.actual_amount,
            pwa.note,
            p.title,
            p.priority,
            p.saved_amount,
            p.target_amount,
            p.end_date,
            p.plan_img_url
        FROM plan_week_allocation pwa
        JOIN plan p ON p.id = pwa.plan_id
        WHERE pwa.budget_week_id = :budget_week_id
          AND pwa.is_deleted = FALSE
          AND p.is_deleted = FALSE
        ORDER BY p.priority ASC, p.title ASC
        """,
        {"budget_week_id": budget_week_id},
    )


def fetch_budget_adjustments_for_week(connection: Any, budget_week_id: int) -> list[dict[str, Any]]:
    return fetch_all(
        connection,
        """
        SELECT
            ba.id,
            ba.plan_id,
            ba.adjustment_type,
            ba.adjustment_amount,
            ba.reason,
            ba.created_at
        FROM budget_adjustment ba
        WHERE ba.budget_week_id = :budget_week_id
          AND ba.is_deleted = FALSE
        ORDER BY ba.created_at DESC, ba.id DESC
        """,
        {"budget_week_id": budget_week_id},
    )


def fetch_essential_snapshot(connection: Any, user_id: int, year_week: str) -> dict[str, Any] | None:
    return fetch_one(
        connection,
        """
        SELECT *
        FROM essential_expense_snapshot_week e
        WHERE e.users_id = :user_id
          AND e.year_week = :year_week
          AND e.is_deleted = FALSE
        LIMIT 1
        """,
        {"user_id": user_id, "year_week": year_week},
    )


def fetch_essential_snapshot_categories(connection: Any, snapshot_week_id: int) -> list[dict[str, Any]]:
    return fetch_all(
        connection,
        """
        SELECT
            esc.id,
            esc.snapshot_week_id,
            esc.expense_scope,
            esc.amount,
            c.id AS category_id,
            c.name AS category_name
        FROM essential_expense_snapshot_week_category esc
        JOIN category c ON c.id = esc.category_id
        WHERE esc.snapshot_week_id = :snapshot_week_id
          AND esc.is_deleted = FALSE
        ORDER BY c.id ASC
        """,
        {"snapshot_week_id": snapshot_week_id},
    )


def fetch_essential_week_allocations(connection: Any, budget_week_id: int) -> list[dict[str, Any]]:
    return fetch_all(
        connection,
        """
        SELECT
            ewa.essential_week_allocation_id AS id,
            ewa.budget_week_id,
            ewa.expense_scope,
            ewa.planned_amount,
            ewa.actual_spent_amount,
            ewa.note,
            c.id AS category_id,
            c.name AS category_name
        FROM essential_week_allocation ewa
        JOIN category c ON c.id = ewa.category_id
        WHERE ewa.budget_week_id = :budget_week_id
          AND ewa.is_deleted = FALSE
        ORDER BY c.id ASC
        """,
        {"budget_week_id": budget_week_id},
    )


def group_transactions_by_category_scope(projections: Iterable[dict[str, Any]]) -> dict[tuple[int, str], list[dict[str, Any]]]:
    grouped: dict[tuple[int, str], list[dict[str, Any]]] = defaultdict(list)
    for item in projections:
        category_id = ensure_int(item.get("analysis_category_id") or item.get("base_category_id"))
        grouped[(category_id, resolve_expense_scope(item))].append(item)
    return grouped


def build_transaction_item(projection: Mapping[str, Any]) -> dict[str, Any]:
    return {
        "transaction_id": projection.get("transaction_id"),
        "source_type": projection.get("source_type"),
        "source_id": ensure_text(projection.get("source_id")),
        "occurred_at": to_iso(projection.get("occurred_at")),
        "merchant_text": ensure_text(projection.get("merchant_text")) or ensure_text(projection.get("transaction_memo")),
        "amount": ensure_int(projection.get("amount")),
        "transaction_type": projection.get("transaction_type"),
        "transaction_type_name": projection.get("transaction_type_name"),
        "after_balance": projection.get("transaction_after_balance"),
        "category": {
            "id": projection.get("analysis_category_id") or projection.get("base_category_id"),
            "name": projection.get("analysis_category_name") or projection.get("base_category_name"),
        },
        "analysis_included": coerce_bool(projection.get("analysis_included")),
        "excluded_reason": projection.get("excluded_reason"),
        "expense_scope": projection.get("expense_scope"),
        "analyzed_at": to_iso(projection.get("analyzed_at")),
    }


def build_category_totals(projections: Iterable[dict[str, Any]]) -> list[dict[str, Any]]:
    amounts: dict[tuple[int, str], int] = defaultdict(int)
    for item in projections:
        category_id = ensure_int(item.get("analysis_category_id") or item.get("base_category_id"))
        category_name = ensure_text(item.get("analysis_category_name") or item.get("base_category_name")) or "UNKNOWN"
        amounts[(category_id, category_name)] += ensure_int(item.get("amount"))
    ordered = sorted(amounts.items(), key=lambda kv: kv[1], reverse=True)
    return [
        {"category_id": category_id, "category_name": category_name, "amount": amount}
        for (category_id, category_name), amount in ordered
    ]


def build_expense_points_by_day(week_start: date, projections: list[dict[str, Any]]) -> list[dict[str, Any]]:
    by_day: dict[date, int] = defaultdict(int)
    for item in projections:
        tx_date = item["occurred_at"].date() if isinstance(item["occurred_at"], datetime) else parse_date_value(item["occurred_at"])
        if tx_date is not None:
            by_day[tx_date] += ensure_int(item.get("amount"))
    return [
        {"date": to_iso(week_start + timedelta(days=index)), "amount": by_day.get(week_start + timedelta(days=index), 0)}
        for index in range(7)
    ]


def build_expense_points_by_week(target_month: YearMonthValue, projections: list[dict[str, Any]]) -> list[dict[str, Any]]:
    cursor = target_month.first_day
    while cursor.weekday() != 0:
        cursor -= timedelta(days=1)
    month_end = target_month.last_day
    points: list[dict[str, Any]] = []
    week_index = 1
    while cursor <= month_end:
        week_start = cursor
        week_end = cursor + timedelta(days=6)
        amount = 0
        for item in projections:
            occurred_at = item["occurred_at"]
            tx_date = occurred_at.date() if isinstance(occurred_at, datetime) else parse_date_value(occurred_at)
            if tx_date and target_month.first_day <= tx_date <= month_end and week_start <= tx_date <= week_end:
                amount += ensure_int(item.get("amount"))
        points.append(
            {
                "label": f"{target_month.month}월 {week_index}주차",
                "week_start_date": to_iso(week_start),
                "week_end_date": to_iso(week_end),
                "amount": amount,
            }
        )
        cursor += timedelta(weeks=1)
        week_index += 1
    return points


def summarize_monthly_expense(projections: list[dict[str, Any]], month: YearMonthValue) -> dict[str, Any]:
    pure_expenses = [item for item in projections if is_pure_expense(item)]
    fixed_amount = sum(ensure_int(item.get("amount")) for item in pure_expenses if resolve_expense_scope(item) == "FIXED")
    variable_amount = sum(ensure_int(item.get("amount")) for item in pure_expenses if resolve_expense_scope(item) == "VARIABLE")
    income_excluded = sum(ensure_int(item.get("amount")) for item in projections if item.get("excluded_reason") == "INCOME")
    internal_transfer_excluded = sum(
        ensure_int(item.get("amount")) for item in projections if item.get("excluded_reason") == "INTERNAL_TRANSFER"
    )
    latest_analyzed_at = max((item.get("analyzed_at") for item in projections if item.get("analyzed_at") is not None), default=None)
    return {
        "month": str(month),
        "expense_amount": fixed_amount + variable_amount,
        "fixed_expense_amount": fixed_amount,
        "variable_expense_amount": variable_amount,
        "excluded_income_amount": income_excluded,
        "excluded_internal_transfer_amount": internal_transfer_excluded,
        "transaction_count": len(pure_expenses),
        "last_analyzed_at": to_iso(latest_analyzed_at),
        "top_categories": build_category_totals(pure_expenses)[:3],
    }


def summarize_onboarding(metadata: Mapping[str, Any], projections: list[dict[str, Any]]) -> dict[str, Any]:
    requested_months = max(1, ensure_int(metadata.get("requested_months")))
    income_amount = sum(ensure_int(item.get("amount")) for item in projections if is_income(item))
    expense_amount = sum(ensure_int(item.get("amount")) for item in projections if is_pure_expense(item))
    fixed_amount = sum(
        ensure_int(item.get("amount")) for item in projections if is_pure_expense(item) and resolve_expense_scope(item) == "FIXED"
    )
    variable_amount = sum(
        ensure_int(item.get("amount")) for item in projections if is_pure_expense(item) and resolve_expense_scope(item) == "VARIABLE"
    )
    avg_income = round(income_amount / requested_months)
    avg_expense = round(expense_amount / requested_months)
    fixed_ratio = round((fixed_amount * 100.0 / expense_amount), 2) if expense_amount else 0.0
    variable_ratio = round((variable_amount * 100.0 / expense_amount), 2) if expense_amount else 0.0
    return {
        "requested_months": requested_months,
        "average_income": avg_income,
        "average_expense": avg_expense,
        "remaining_amount": avg_income - avg_expense,
        "fixed_expense_ratio": fixed_ratio,
        "variable_expense_ratio": variable_ratio,
        "top_categories": build_category_totals([item for item in projections if is_pure_expense(item)])[:5],
        "analysis_window_start_at": to_iso(metadata.get("window_start_at")),
        "analysis_window_end_at": to_iso(metadata.get("window_end_at")),
        "analyzed_at": to_iso(metadata.get("analyzed_at")),
    }


def summarize_weekly_analysis(label: str, week_start: date, projections: list[dict[str, Any]]) -> dict[str, Any]:
    pure_expenses = [item for item in projections if is_pure_expense(item)]
    essential_expenses = [item for item in pure_expenses if is_analysis_included(item)]
    return {
        "year_week": label,
        "week_start_date": to_iso(week_start),
        "week_end_date": to_iso(week_start + timedelta(days=6)),
        "expense_amount": sum(ensure_int(item.get("amount")) for item in pure_expenses),
        "essential_amount": sum(ensure_int(item.get("amount")) for item in essential_expenses),
        "fixed_essential_amount": sum(
            ensure_int(item.get("amount")) for item in essential_expenses if resolve_expense_scope(item) == "FIXED"
        ),
        "variable_essential_amount": sum(
            ensure_int(item.get("amount")) for item in essential_expenses if resolve_expense_scope(item) == "VARIABLE"
        ),
        "top_categories": build_category_totals(pure_expenses)[:5],
        "last_analyzed_at": to_iso(max((item.get("analyzed_at") for item in projections if item.get("analyzed_at")), default=None)),
    }


def summarize_monthly_analysis(
    target_month: YearMonthValue,
    projections: list[dict[str, Any]],
    snapshots: list[dict[str, Any]],
) -> dict[str, Any]:
    pure_expenses = [item for item in projections if is_pure_expense(item)]
    return {
        "month": str(target_month),
        "expense_amount": sum(ensure_int(item.get("amount")) for item in pure_expenses),
        "essential_amount": sum(ensure_int(item.get("essential_amount")) for item in snapshots),
        "fixed_essential_amount": sum(ensure_int(item.get("fixed_essential_amount")) for item in snapshots),
        "variable_essential_amount": sum(ensure_int(item.get("variable_essential_amount")) for item in snapshots),
        "top_categories": build_category_totals(pure_expenses)[:5],
        "last_analyzed_at": to_iso(max((item.get("analyzed_at") for item in snapshots if item.get("analyzed_at")), default=None)),
    }


def summarize_budget_home(
    budget_week: Mapping[str, Any],
    selected_plans: list[dict[str, Any]],
    allocations: list[dict[str, Any]],
    accounts: list[dict[str, Any]],
    estimated_balances: dict[str, int],
) -> dict[str, Any]:
    today = now_kst().date()
    free_spending = summarize_budget_dashboard(budget_week, today)
    selected_ids = {ensure_int(item.get("id")) for item in selected_plans}
    this_week_saving_amount = sum(
        ensure_int(item.get("final_amount")) for item in allocations if ensure_int(item.get("plan_id")) in selected_ids
    )
    remaining_to_target_amount = sum(
        ensure_int(item.get("target_amount")) - ensure_int(item.get("saved_amount")) for item in selected_plans
    )
    total_saved_amount = sum(ensure_int(item.get("saved_amount")) for item in selected_plans)
    total_target_amount = sum(ensure_int(item.get("target_amount")) for item in selected_plans)
    progress_rate = round((total_saved_amount * 100 / total_target_amount), 2) if total_target_amount else 0.0
    asset_accounts = []
    total_asset_amount = 0
    for account in accounts:
        estimated_balance = estimated_balances.get(str(account.get("id")))
        if estimated_balance is not None:
            total_asset_amount += estimated_balance
        asset_accounts.append(
            {
                "account_id": account.get("id"),
                "bank_name": account.get("bank_name"),
                "account_no": account.get("account_no"),
                "estimated_balance": estimated_balance,
                "verified": coerce_bool(account.get("is_verified")),
            }
        )

    weekly_target_by_plan: dict[int, int] = defaultdict(int)
    weekly_saved_by_plan: dict[int, int] = defaultdict(int)
    for allocation in allocations:
        plan_id = ensure_int(allocation.get("plan_id"))
        weekly_target_by_plan[plan_id] += ensure_int(allocation.get("planned_amount"))
        weekly_saved_by_plan[plan_id] += ensure_int(allocation.get("actual_amount"))

    plan_cards = []
    for plan in selected_plans:
        plan_id = ensure_int(plan.get("id"))
        weekly_target = weekly_target_by_plan.get(plan_id, 0)
        weekly_saved = weekly_saved_by_plan.get(plan_id, 0)
        end_date = plan.get("end_date")
        d_day = 0 if end_date is None else (end_date - today).days
        plan_cards.append(
            {
                "plan_id": plan_id,
                "title": plan.get("title"),
                "priority": ensure_int(plan.get("priority")),
                "d_day": d_day,
                "weekly_saved_amount": weekly_saved,
                "weekly_target_amount": weekly_target,
                "weekly_progress_rate": round((weekly_saved * 100 / weekly_target), 2) if weekly_target else 0.0,
                "plan_image_url": plan.get("plan_img_url"),
            }
        )

    return {
        "free_spending": free_spending,
        "weekly_saving": {
            "this_week_saving_amount": this_week_saving_amount,
            "remaining_to_target_amount": remaining_to_target_amount,
            "progress_rate": progress_rate,
        },
        "asset_summary": {
            "total_estimated_asset_amount": total_asset_amount,
            "accounts": asset_accounts,
        },
        "plan_cards": plan_cards,
    }


def summarize_weekly_savings_detail(
    budget_week: Mapping[str, Any],
    allocations: list[dict[str, Any]],
    adjustments: list[dict[str, Any]],
    year_week: str,
) -> dict[str, Any]:
    adjustments_by_plan: dict[int, list[dict[str, Any]]] = defaultdict(list)
    for adjustment in adjustments:
        plan_id = adjustment.get("plan_id")
        if plan_id is None:
            continue
        adjustment_type = ensure_text(adjustment.get("adjustment_type"))
        if adjustment_type in {"PLAN_SAVED_AUTO_ADD", "PLAN_SAVED_AUTO_SUBTRACT"}:
            continue
        adjustments_by_plan[ensure_int(plan_id)].append(adjustment)

    saving_details = []
    for allocation in allocations:
        plan_id = ensure_int(allocation.get("plan_id"))
        grouped = adjustments_by_plan.get(plan_id, [])
        latest = grouped[0] if grouped else None
        saving_details.append(
            {
                "title": allocation.get("title"),
                "priority": ensure_int(allocation.get("priority")),
                "planned_amount": ensure_int(allocation.get("planned_amount")),
                "final_amount": ensure_int(allocation.get("final_amount")),
                "saved_amount": ensure_int(allocation.get("saved_amount")),
                "adjustment_summary": {
                    "count": len(grouped),
                    "latest_type": latest.get("adjustment_type") if latest else None,
                    "latest_reason": latest.get("reason") if latest else None,
                },
            }
        )

    final_saving_amount = sum(ensure_int(item.get("final_amount")) for item in allocations)
    return {
        "year_week": year_week,
        "week_start_date": to_iso(budget_week.get("week_start_date")),
        "week_end_date": to_iso(budget_week.get("week_end_date")),
        "planned_saving_amount": ensure_int(budget_week.get("planned_saving_amount")),
        "final_saving_amount": final_saving_amount,
        "saving_details": saving_details,
    }


def summarize_weekly_free_spending_detail(
    budget_week: Mapping[str, Any],
    projections: list[dict[str, Any]],
    year_week: str,
) -> dict[str, Any]:
    allocated_amount = ensure_int(budget_week.get("final_free_amount"))
    spent_amount = ensure_int(budget_week.get("actual_spent_amount"))
    return {
        "year_week": year_week,
        "week_start_date": to_iso(budget_week.get("week_start_date")),
        "week_end_date": to_iso(budget_week.get("week_end_date")),
        "allocated_amount": allocated_amount,
        "spent_amount": spent_amount,
        "remaining_amount": allocated_amount - spent_amount,
        "transactions": [build_transaction_item(item) for item in projections],
    }


def summarize_weekly_essential_snapshot(
    snapshot: Mapping[str, Any],
    categories: list[dict[str, Any]],
) -> dict[str, Any]:
    fixed = []
    variable = []
    for item in categories:
        entry = {
            "category_id": item.get("category_id"),
            "category_name": item.get("category_name"),
            "amount": ensure_int(item.get("amount")),
        }
        if item.get("expense_scope") == "FIXED":
            fixed.append(entry)
        else:
            variable.append(entry)
    return {
        "year_week": snapshot.get("year_week"),
        "week_start_date": to_iso(snapshot.get("week_start_date")),
        "week_end_date": to_iso(snapshot.get("week_end_date")),
        "base_months": ensure_int(snapshot.get("base_months")),
        "essential_amount": ensure_int(snapshot.get("essential_amount")),
        "fixed_essential_amount": ensure_int(snapshot.get("fixed_essential_amount")),
        "variable_essential_amount": ensure_int(snapshot.get("variable_essential_amount")),
        "fixed_categories": fixed,
        "variable_categories": variable,
        "analyzed_at": to_iso(snapshot.get("analyzed_at")),
    }


def summarize_weekly_essential_detail(
    budget_week: Mapping[str, Any],
    allocations: list[dict[str, Any]],
    expense_projections: list[dict[str, Any]],
    year_week: str,
) -> dict[str, Any]:
    grouped_transactions = group_transactions_by_category_scope([item for item in expense_projections if is_analysis_included(item)])
    fixed_categories = []
    variable_categories = []
    for allocation in allocations:
        scope = allocation.get("expense_scope")
        category_id = ensure_int(allocation.get("category_id"))
        entry = {
            "category_id": category_id,
            "category_name": allocation.get("category_name"),
            "planned_amount": ensure_int(allocation.get("planned_amount")),
            "actual_spent_amount": ensure_int(allocation.get("actual_spent_amount")),
            "remaining_amount": ensure_int(allocation.get("planned_amount")) - ensure_int(allocation.get("actual_spent_amount")),
            "transactions": [build_transaction_item(item) for item in grouped_transactions.get((category_id, scope), [])],
        }
        if scope == "FIXED":
            fixed_categories.append(entry)
        else:
            variable_categories.append(entry)
    planned_essential_amount = sum(ensure_int(item.get("planned_amount")) for item in allocations)
    actual_spent_amount = sum(ensure_int(item.get("actual_spent_amount")) for item in allocations)
    return {
        "year_week": year_week,
        "week_start_date": to_iso(budget_week.get("week_start_date")),
        "week_end_date": to_iso(budget_week.get("week_end_date")),
        "planned_essential_amount": planned_essential_amount,
        "actual_spent_amount": actual_spent_amount,
        "remaining_amount": planned_essential_amount - actual_spent_amount,
        "fixed_categories": fixed_categories,
        "variable_categories": variable_categories,
    }


@tool
def get_linked_accounts(user_id: int, include_recent_activity: bool = False) -> dict[str, Any]:
    """Return linked account status for a user using only local DB data."""
    warnings: list[str] = []
    with db_connection() as connection:
        accounts = fetch_linked_accounts_rows(connection, user_id)
        cards = fetch_linked_cards_rows(connection, user_id)
        activities = fetch_recent_account_activity_rows(connection, user_id) if include_recent_activity else []
        estimated_balances = fetch_account_estimated_balances(connection, user_id)

    if not accounts:
        add_warning(warnings, "No linked accounts found in the local database.")

    account_views = []
    for account in accounts:
        account_id = str(account.get("id"))
        account_views.append(
            {
                "account_no": account.get("account_no"),
                "bank_name": account.get("bank_name"),
                "account_name": account.get("account_name"),
                "alias": account.get("alias"),
                "verified": coerce_bool(account.get("is_verified")),
                "active": coerce_bool(account.get("is_active")),
                "estimated_balance": estimated_balances.get(account_id),
                "currency": account.get("currency"),
            }
        )
        if estimated_balances.get(account_id) is None:
            add_warning(warnings, f"Estimated balance is unavailable for account {account.get('account_no')}.")

    summary = summarize_asset_linkage(accounts, cards, activities)
    summary["accounts"] = account_views
    evidence = [
        {"label": "linked_accounts", "value": len(accounts)},
        {"label": "linked_cards", "value": len(cards)},
    ]
    if activities:
        evidence.append({"label": "recent_activity_sample", "value": [build_transaction_item(item) for item in activities[:3]]})
    return build_tool_response(
        "get_linked_accounts",
        summary,
        evidence,
        warnings,
        {"accounts": account_views, "recent_activities": [build_transaction_item(item) for item in activities]},
    )


@tool
def get_account_detail(user_id: int, account_no: str, include_recent_activity: bool = True) -> dict[str, Any]:
    """Return a single linked account detail using local DB data."""
    warnings: list[str] = []
    with db_connection() as connection:
        account = fetch_account_detail_row(connection, user_id, account_no)
        if account is None:
            raise DataNotFoundError(f"Account not found for user_id={user_id}, account_no={account_no}.")
        estimated_balances = fetch_account_estimated_balances(connection, user_id)
        activities = []
        if include_recent_activity:
            activities = [
                item
                for item in fetch_recent_account_activity_rows(connection, user_id, ACCOUNT_ACTIVITY_LIMIT)
                if item.get("source_id") == account["id"]
            ]

    estimated_balance = estimated_balances.get(str(account["id"]))
    if estimated_balance is None:
        add_warning(warnings, "Estimated balance could not be derived from recent account transactions.")

    summary = {
        "account_no": account["account_no"],
        "bank_name": account["bank_name"],
        "account_name": account["account_name"],
        "alias": account["alias"],
        "verified": coerce_bool(account["is_verified"]),
        "active": coerce_bool(account["is_active"]),
        "estimated_balance": estimated_balance,
        "recent_activity_count": len(activities),
    }
    evidence = [{"label": "recent_transactions", "value": [build_transaction_item(item) for item in activities]}]
    return build_tool_response(
        "get_account_detail",
        summary,
        evidence,
        warnings,
        {"account": account, "recent_activities": [build_transaction_item(item) for item in activities]},
        window=_window_payload(),
    )


@tool
def get_linked_cards(user_id: int) -> dict[str, Any]:
    """Return linked card list for a user."""
    warnings: list[str] = []
    with db_connection() as connection:
        cards = fetch_linked_cards_rows(connection, user_id)
    if not cards:
        add_warning(warnings, "No linked cards found in the local database.")
    summary = {
        "linked_card_count": len(cards),
        "cards": [
            {
                "card_unique_no": item["card_unique_no"],
                "card_no": item["card_no"],
                "card_name": item["card_name"],
                "card_issuer_name": item["card_issuer_name"],
                "linked_account_no": item["linked_account_no"],
            }
            for item in cards
        ],
    }
    evidence = [{"label": "card_issuers", "value": sorted({item["card_issuer_name"] for item in cards})}]
    return build_tool_response("get_linked_cards", summary, evidence, warnings, {"cards": cards})


@tool
def get_asset_linkage_status(user_id: int, lookback_days: int = 30) -> dict[str, Any]:
    """Return asset linkage health using accounts, cards, and recent activity."""
    warnings: list[str] = []
    with db_connection() as connection:
        accounts = fetch_linked_accounts_rows(connection, user_id)
        cards = fetch_linked_cards_rows(connection, user_id)
        activities = fetch_recent_account_activity_rows(connection, user_id, ACCOUNT_ACTIVITY_LIMIT) + fetch_recent_card_activity_rows(
            connection, user_id, CARD_ACTIVITY_LIMIT
        )

    cutoff = now_kst() - timedelta(days=lookback_days)
    recent_activities = [
        item for item in activities if isinstance(item.get("occurred_at"), datetime) and item["occurred_at"].replace(tzinfo=KST) >= cutoff
    ]
    summary = summarize_asset_linkage(accounts, cards, recent_activities)
    summary["activity_within_lookback_count"] = len(recent_activities)
    if not recent_activities:
        add_warning(warnings, f"No account or card activity found in the last {lookback_days} days.")
    return build_tool_response(
        "get_asset_linkage_status",
        summary,
        [{"label": "recent_activity", "value": [build_transaction_item(item) for item in recent_activities[:5]]}],
        warnings,
        {
            "accounts": accounts,
            "cards": cards,
            "recent_activities": [build_transaction_item(item) for item in recent_activities],
        },
        window=_window_payload(date_from=cutoff.date(), date_to=now_kst().date()),
    )


@tool
def get_transaction_list(
    user_id: int,
    from_date: date | str | None = None,
    to_date: date | str | None = None,
    analysis_inclusion: str = "ALL",
    page: int = 0,
    size: int = 20,
) -> dict[str, Any]:
    """Return unified transaction list from account and card transaction tables."""
    warnings: list[str] = []
    resolved_from, resolved_to = resolve_transaction_window(from_date, to_date)
    with db_connection() as connection:
        projections = fetch_transaction_projections(
            connection,
            user_id,
            datetime.combine(resolved_from, time.min),
            datetime.combine(resolved_to, time.max),
        )

    inclusion = (analysis_inclusion or "ALL").upper()
    filtered = []
    for item in projections:
        included = is_analysis_included(item)
        if inclusion == "INCLUDED" and not included:
            continue
        if inclusion == "EXCLUDED" and included:
            continue
        filtered.append(item)
    start = max(page, 0) * max(size, 1)
    end = start + max(size, 1)
    items = [build_transaction_item(item) for item in filtered[start:end]]
    if not filtered:
        add_warning(warnings, "No transactions found for the requested window.")
    summary = {
        "total_count": len(filtered),
        "page": max(page, 0),
        "size": max(size, 1),
        "returned_count": len(items),
        "analysis_inclusion": inclusion,
    }
    return build_tool_response(
        "get_transaction_list",
        summary,
        [{"label": "sample_transactions", "value": items[:3]}],
        warnings,
        {"items": items},
        window=_window_payload(date_from=resolved_from, date_to=resolved_to),
    )


@tool
def get_monthly_expense_summary(user_id: int, month: str | None = None) -> dict[str, Any]:
    """Return monthly expense summary aligned with Spring transaction analysis rules."""
    warnings: list[str] = []
    target_month = parse_year_month(month)
    with db_connection() as connection:
        metadata = fetch_latest_analysis_metadata(connection, user_id)
        projections = fetch_transaction_projections(
            connection,
            user_id,
            datetime.combine(target_month.first_day, time.min),
            datetime.combine(target_month.last_day, time.max),
            metadata.get("analyzed_at") if metadata else None,
        )
    if metadata is None:
        add_warning(warnings, "No transaction analysis metadata found. Returning DB-based expense summary only.")
    summary = summarize_monthly_expense(projections, target_month)
    return build_tool_response(
        "get_monthly_expense_summary",
        summary,
        [{"label": "top_categories", "value": summary["top_categories"]}],
        warnings,
        {"transactions": [build_transaction_item(item) for item in projections[:TRANSACTION_SAMPLE_LIMIT]], "summary": summary},
        window=_window_payload(date_from=target_month.first_day, date_to=target_month.last_day),
    )


@tool
def get_weekly_expense_graph(user_id: int, year_week: str) -> dict[str, Any]:
    """Return weekly expense graph points for transaction spending."""
    warnings: list[str] = []
    label, week_start = parse_transaction_year_week(year_week)
    week_end = week_start + timedelta(days=6)
    with db_connection() as connection:
        projections = fetch_transaction_projections(
            connection,
            user_id,
            datetime.combine(week_start, time.min),
            datetime.combine(week_end, time.max),
        )
    pure_expenses = [item for item in projections if is_pure_expense(item)]
    points = build_expense_points_by_day(week_start, pure_expenses)
    if not pure_expenses:
        add_warning(warnings, "No expense transactions found for the requested week.")
    summary = {
        "year_week": label,
        "week_start_date": to_iso(week_start),
        "week_end_date": to_iso(week_end),
        "expense_amount": sum(ensure_int(item.get("amount")) for item in pure_expenses),
        "points": points,
    }
    return build_tool_response(
        "get_weekly_expense_graph",
        summary,
        [{"label": "points", "value": points}],
        warnings,
        {"transactions": [build_transaction_item(item) for item in pure_expenses[:TRANSACTION_SAMPLE_LIMIT]]},
        window=_window_payload(date_from=week_start, date_to=week_end),
    )


@tool
def get_monthly_expense_graph(user_id: int, year_month: str) -> dict[str, Any]:
    """Return weekly buckets for a monthly expense graph."""
    warnings: list[str] = []
    target_month = parse_year_month(year_month)
    with db_connection() as connection:
        projections = fetch_transaction_projections(
            connection,
            user_id,
            datetime.combine(target_month.first_day, time.min),
            datetime.combine(target_month.last_day, time.max),
        )
    pure_expenses = [item for item in projections if is_pure_expense(item)]
    points = build_expense_points_by_week(target_month, pure_expenses)
    summary = {
        "month": str(target_month),
        "expense_amount": sum(ensure_int(item.get("amount")) for item in pure_expenses),
        "points": points,
    }
    if not pure_expenses:
        add_warning(warnings, "No expense transactions found for the requested month.")
    return build_tool_response(
        "get_monthly_expense_graph",
        summary,
        [{"label": "points", "value": points}],
        warnings,
        {"transactions": [build_transaction_item(item) for item in pure_expenses[:TRANSACTION_SAMPLE_LIMIT]]},
        window=_window_payload(date_from=target_month.first_day, date_to=target_month.last_day),
    )


@tool
def get_onboarding_spending_analysis(user_id: int) -> dict[str, Any]:
    """Return onboarding-style spending analysis from the latest analysis metadata window."""
    warnings: list[str] = []
    with db_connection() as connection:
        metadata = fetch_latest_analysis_metadata(connection, user_id)
        if metadata is None:
            raise DataNotFoundError(f"Transaction analysis metadata not found for user_id={user_id}.")
        projections = fetch_transaction_projections(
            connection,
            user_id,
            metadata["window_start_at"],
            metadata["window_end_at"],
            metadata["analyzed_at"],
        )
    summary = summarize_onboarding(metadata, projections)
    return build_tool_response(
        "get_onboarding_spending_analysis",
        summary,
        [{"label": "top_categories", "value": summary["top_categories"]}],
        warnings,
        {"metadata": metadata, "sample_transactions": [build_transaction_item(item) for item in projections[:TRANSACTION_SAMPLE_LIMIT]]},
        window=_window_payload(date_from=metadata["window_start_at"], date_to=metadata["window_end_at"]),
    )


@tool
def get_weekly_spending_analysis(user_id: int, year_week: str) -> dict[str, Any]:
    """Return weekly spending analysis for a transaction week window."""
    warnings: list[str] = []
    label, week_start = parse_transaction_year_week(year_week)
    week_end = week_start + timedelta(days=6)
    with db_connection() as connection:
        metadata = fetch_latest_analysis_metadata(connection, user_id)
        projections = fetch_transaction_projections(
            connection,
            user_id,
            datetime.combine(week_start, time.min),
            datetime.combine(week_end, time.max),
            metadata.get("analyzed_at") if metadata else None,
        )
    if metadata is None:
        add_warning(warnings, "No transaction analysis metadata found. Weekly analysis uses currently stored transaction projections only.")
    summary = summarize_weekly_analysis(label, week_start, projections)
    if summary["expense_amount"] == 0:
        add_warning(warnings, "No expense transactions found for the requested week.")
    return build_tool_response(
        "get_weekly_spending_analysis",
        summary,
        [{"label": "top_categories", "value": summary["top_categories"]}],
        warnings,
        {"sample_transactions": [build_transaction_item(item) for item in projections[:TRANSACTION_SAMPLE_LIMIT]]},
        window=_window_payload(date_from=week_start, date_to=week_end),
    )


@tool
def get_monthly_spending_analysis(user_id: int, year_month: str) -> dict[str, Any]:
    """Return monthly spending analysis using transaction projections and essential expense snapshots."""
    warnings: list[str] = []
    target_month = parse_year_month(year_month)
    month_start = target_month.first_day
    month_end = target_month.last_day
    with db_connection() as connection:
        metadata = fetch_latest_analysis_metadata(connection, user_id)
        if metadata is None:
            raise DataNotFoundError(f"Transaction analysis metadata not found for user_id={user_id}.")
        projections = fetch_transaction_projections(
            connection,
            user_id,
            datetime.combine(month_start, time.min),
            datetime.combine(month_end, time.max),
            metadata["analyzed_at"],
        )
        snapshots = fetch_all(
            connection,
            """
            SELECT
                e.year_week,
                e.week_start_date,
                e.week_end_date,
                e.essential_amount,
                e.fixed_essential_amount,
                e.variable_essential_amount,
                e.analyzed_at
            FROM essential_expense_snapshot_week e
            WHERE e.users_id = :user_id
              AND e.is_deleted = FALSE
              AND e.analyzed_at = :analyzed_at
              AND e.week_start_date BETWEEN :range_start AND :range_end
            ORDER BY e.week_start_date ASC
            """,
            {
                "user_id": user_id,
                "analyzed_at": metadata["analyzed_at"],
                "range_start": month_start - timedelta(days=month_start.weekday()),
                "range_end": month_end,
            },
        )
    summary = summarize_monthly_analysis(target_month, projections, snapshots)
    if not snapshots:
        add_warning(warnings, "No essential expense snapshots found for the requested month.")
    return build_tool_response(
        "get_monthly_spending_analysis",
        summary,
        [{"label": "top_categories", "value": summary["top_categories"]}],
        warnings,
        {"snapshots": snapshots, "sample_transactions": [build_transaction_item(item) for item in projections[:TRANSACTION_SAMPLE_LIMIT]]},
        window=_window_payload(date_from=month_start, date_to=month_end),
    )


@tool
def get_budget_income(user_id: int) -> dict[str, Any]:
    """Return current month budget income."""
    warnings: list[str] = []
    current_month = now_kst().date().replace(day=1)
    with db_connection() as connection:
        income_row = fetch_budget_income_row(connection, user_id)
    if income_row is None:
        add_warning(warnings, "Current month budget income row not found. Returning zero income.")
        income_row = {"total_income_amount": 0, "base_date": current_month}
    summary = {
        "base_date": to_iso(income_row["base_date"]),
        "total_income_amount": ensure_int(income_row["total_income_amount"]),
    }
    return build_tool_response(
        "get_budget_income",
        summary,
        [{"label": "base_month", "value": summary["base_date"]}],
        warnings,
        {"income": summary},
        window=_window_payload(date_from=income_row["base_date"], date_to=income_row["base_date"]),
    )


@tool
def get_budget_dashboard(user_id: int) -> dict[str, Any]:
    """Return current budget dashboard summary."""
    warnings: list[str] = []
    with db_connection() as connection:
        budget_week = fetch_current_budget_week(connection, user_id)
    if budget_week is None:
        raise DataNotFoundError(f"Current budget week not found for user_id={user_id}.")
    summary = summarize_budget_dashboard(budget_week)
    evidence = [
        {"label": "planned_income_amount", "value": ensure_int(budget_week.get("planned_income_amount"))},
        {"label": "planned_saving_amount", "value": ensure_int(budget_week.get("planned_saving_amount"))},
    ]
    return build_tool_response(
        "get_budget_dashboard",
        summary,
        evidence,
        warnings,
        {"budget_week": budget_week},
        window=_window_payload(date_from=budget_week["week_start_date"], date_to=budget_week["week_end_date"]),
    )


@tool
def get_budget_home(user_id: int) -> dict[str, Any]:
    """Return current budget home summary with plans and local asset estimate."""
    warnings: list[str] = []
    with db_connection() as connection:
        budget_week = fetch_current_budget_week(connection, user_id)
        if budget_week is None:
            raise DataNotFoundError(f"Current budget week not found for user_id={user_id}.")
        selected_plans = fetch_active_plans(connection, user_id, limit=3)
        allocations = fetch_plan_allocations_for_budget_week(connection, ensure_int(budget_week["id"]))
        accounts = fetch_linked_accounts_rows(connection, user_id)
        estimated_balances = fetch_account_estimated_balances(connection, user_id)
    if not selected_plans:
        add_warning(warnings, "No active plans found for budget home.")
    summary = summarize_budget_home(budget_week, selected_plans, allocations, accounts, estimated_balances)
    if any(item.get("estimated_balance") is None for item in summary["asset_summary"]["accounts"]):
        add_warning(warnings, "Some asset balances are estimated as unavailable because recent account balances were not found locally.")
    evidence = [
        {"label": "free_spending", "value": summary["free_spending"]},
        {"label": "weekly_saving", "value": summary["weekly_saving"]},
    ]
    return build_tool_response(
        "get_budget_home",
        summary,
        evidence,
        warnings,
        {
            "budget_week": budget_week,
            "selected_plans": selected_plans,
            "allocations": allocations,
            "accounts": summary["asset_summary"]["accounts"],
        },
        window=_window_payload(date_from=budget_week["week_start_date"], date_to=budget_week["week_end_date"]),
    )


@tool
def get_budget_summary(user_id: int) -> dict[str, Any]:
    """Return active/completed plan counts and total saved amount."""
    warnings: list[str] = []
    with db_connection() as connection:
        active_count = fetch_one(
            connection,
            """
            SELECT COUNT(*) AS count
            FROM plan p
            WHERE p.users_id = :user_id
              AND p.is_deleted = FALSE
              AND p.status = 'ACTIVE'
            """,
            {"user_id": user_id},
        )["count"]
        completed_count = fetch_one(
            connection,
            """
            SELECT COUNT(*) AS count
            FROM plan p
            WHERE p.users_id = :user_id
              AND p.is_deleted = FALSE
              AND p.status = 'COMPLETED'
            """,
            {"user_id": user_id},
        )["count"]
        total_saved_row = fetch_one(
            connection,
            """
            SELECT COALESCE(SUM(p.saved_amount), 0) AS total_saved_amount
            FROM plan p
            WHERE p.users_id = :user_id
              AND p.is_deleted = FALSE
            """,
            {"user_id": user_id},
        )
    summary = {
        "active_plan_count": ensure_int(active_count),
        "completed_plan_count": ensure_int(completed_count),
        "total_saved_amount": ensure_int(total_saved_row["total_saved_amount"]),
    }
    return build_tool_response("get_budget_summary", summary, [{"label": "summary", "value": summary}], warnings, {"summary": summary})


@tool
def get_weekly_savings_detail(user_id: int, year_week: str) -> dict[str, Any]:
    """Return savings detail for a specific ISO budget week."""
    warnings: list[str] = []
    week_start = parse_iso_year_week(year_week)
    with db_connection() as connection:
        budget_week = fetch_budget_week_by_start(connection, user_id, week_start)
        if budget_week is None:
            raise DataNotFoundError(f"Budget week not found for user_id={user_id}, year_week={year_week}.")
        allocations = fetch_plan_allocations_for_budget_week(connection, ensure_int(budget_week["id"]))
        adjustments = fetch_budget_adjustments_for_week(connection, ensure_int(budget_week["id"]))
    summary = summarize_weekly_savings_detail(budget_week, allocations, adjustments, year_week)
    if not allocations:
        add_warning(warnings, "No savings allocations found for the requested week.")
    return build_tool_response(
        "get_weekly_savings_detail",
        summary,
        [{"label": "saving_details", "value": summary["saving_details"][:3]}],
        warnings,
        {"allocations": allocations, "adjustments": adjustments},
        window=_window_payload(date_from=budget_week["week_start_date"], date_to=budget_week["week_end_date"]),
    )


@tool
def get_weekly_free_spending_detail(user_id: int, year_week: str) -> dict[str, Any]:
    """Return non-essential free spending detail for a specific ISO budget week."""
    warnings: list[str] = []
    week_start = parse_iso_year_week(year_week)
    with db_connection() as connection:
        budget_week = fetch_budget_week_by_start(connection, user_id, week_start)
        if budget_week is None:
            raise DataNotFoundError(f"Budget week not found for user_id={user_id}, year_week={year_week}.")
        projections = fetch_transaction_projections(
            connection,
            user_id,
            datetime.combine(budget_week["week_start_date"], time.min),
            datetime.combine(budget_week["week_end_date"], time.max),
        )
    free_spending = [item for item in projections if is_non_essential_free_spending(item)]
    summary = summarize_weekly_free_spending_detail(budget_week, free_spending, year_week)
    if not free_spending:
        add_warning(warnings, "No non-essential free spending transactions found for the requested week.")
    return build_tool_response(
        "get_weekly_free_spending_detail",
        summary,
        [{"label": "transactions", "value": summary["transactions"][:3]}],
        warnings,
        {"transactions": summary["transactions"]},
        window=_window_payload(date_from=budget_week["week_start_date"], date_to=budget_week["week_end_date"]),
    )


@tool
def get_weekly_essential_expenses(user_id: int, year_week: str) -> dict[str, Any]:
    """Return weekly essential expense snapshot."""
    warnings: list[str] = []
    with db_connection() as connection:
        snapshot = fetch_essential_snapshot(connection, user_id, year_week)
        if snapshot is None:
            raise DataNotFoundError(f"Weekly essential expense snapshot not found for user_id={user_id}, year_week={year_week}.")
        categories = fetch_essential_snapshot_categories(connection, ensure_int(snapshot["id"]))
    summary = summarize_weekly_essential_snapshot(snapshot, categories)
    return build_tool_response(
        "get_weekly_essential_expenses",
        summary,
        [
            {"label": "fixed_categories", "value": summary["fixed_categories"][:3]},
            {"label": "variable_categories", "value": summary["variable_categories"][:3]},
        ],
        warnings,
        {"snapshot": snapshot, "categories": categories},
        window=_window_payload(date_from=snapshot["week_start_date"], date_to=snapshot["week_end_date"]),
    )


@tool
def get_weekly_essential_expense_detail(user_id: int, year_week: str) -> dict[str, Any]:
    """Return weekly essential expense detail with category allocations and transactions."""
    warnings: list[str] = []
    week_start = parse_iso_year_week(year_week)
    with db_connection() as connection:
        budget_week = fetch_budget_week_by_start(connection, user_id, week_start)
        if budget_week is None:
            raise DataNotFoundError(f"Budget week not found for user_id={user_id}, year_week={year_week}.")
        allocations = fetch_essential_week_allocations(connection, ensure_int(budget_week["id"]))
        projections = fetch_transaction_projections(
            connection,
            user_id,
            datetime.combine(budget_week["week_start_date"], time.min),
            datetime.combine(budget_week["week_end_date"], time.max),
        )
    expense_projections = [item for item in projections if is_pure_expense(item)]
    summary = summarize_weekly_essential_detail(budget_week, allocations, expense_projections, year_week)
    if not allocations:
        add_warning(warnings, "No essential week allocations found for the requested week.")
    return build_tool_response(
        "get_weekly_essential_expense_detail",
        summary,
        [
            {"label": "fixed_categories", "value": summary["fixed_categories"][:3]},
            {"label": "variable_categories", "value": summary["variable_categories"][:3]},
        ],
        warnings,
        {"allocations": allocations, "sample_transactions": [build_transaction_item(item) for item in expense_projections[:TRANSACTION_SAMPLE_LIMIT]]},
        window=_window_payload(date_from=budget_week["week_start_date"], date_to=budget_week["week_end_date"]),
    )


TOOLS = [
    get_linked_accounts,
    get_account_detail,
    get_linked_cards,
    get_asset_linkage_status,
    get_transaction_list,
    get_monthly_expense_summary,
    get_weekly_expense_graph,
    get_monthly_expense_graph,
    get_onboarding_spending_analysis,
    get_weekly_spending_analysis,
    get_monthly_spending_analysis,
    get_budget_income,
    get_budget_dashboard,
    get_budget_home,
    get_budget_summary,
    get_weekly_savings_detail,
    get_weekly_free_spending_detail,
    get_weekly_essential_expenses,
    get_weekly_essential_expense_detail,
]


__all__ = [
    "TOOLS",
    "build_tool_response",
    "create_db_engine",
    "get_account_detail",
    "get_asset_linkage_status",
    "get_budget_dashboard",
    "get_budget_home",
    "get_budget_income",
    "get_budget_summary",
    "get_linked_accounts",
    "get_linked_cards",
    "get_monthly_expense_graph",
    "get_monthly_expense_summary",
    "get_monthly_spending_analysis",
    "get_onboarding_spending_analysis",
    "get_transaction_list",
    "get_weekly_essential_expense_detail",
    "get_weekly_essential_expenses",
    "get_weekly_expense_graph",
    "get_weekly_free_spending_detail",
    "get_weekly_savings_detail",
    "get_weekly_spending_analysis",
    "is_pure_expense",
    "parse_iso_year_week",
    "parse_transaction_year_week",
    "parse_year_month",
    "resolve_transaction_window",
    "summarize_asset_linkage",
    "summarize_budget_dashboard",
]
