from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime, timezone
from typing import Any

from app.core.database import ReadOnlyDatabase, validate_user_id


def _as_iso_date(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value.date().isoformat()
    if isinstance(value, date):
        return value.isoformat()
    return str(value)


def _as_iso_datetime(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value.isoformat()
    if isinstance(value, date):
        return datetime.combine(value, datetime.min.time()).isoformat()
    return str(value)


def _build_standard_contract(
    *,
    as_of: str,
    window: dict[str, str | None],
    summary: dict[str, Any],
    evidence: list[dict[str, Any]],
    warnings: list[str],
) -> dict[str, Any]:
    return {
        "as_of": as_of,
        "window": window,
        "summary": summary,
        "evidence": evidence,
        "warnings": warnings,
    }


def _build_budget_week_evidence(row: dict[str, Any]) -> dict[str, Any]:
    return {
        "kind": "budget_week",
        "budget_week_id": row.get("id"),
        "week_start_date": _as_iso_date(row.get("week_start_date")),
        "week_end_date": _as_iso_date(row.get("week_end_date")),
        "planned_income_amount": row.get("planned_income_amount"),
        "planned_essential_amount": row.get("planned_essential_amount"),
        "planned_saving_amount": row.get("planned_saving_amount"),
        "rollover_in_amount": row.get("rollover_in_amount"),
        "manual_adjustment_amount": row.get("manual_adjustment_amount"),
        "final_free_amount": row.get("final_free_amount"),
        "actual_spent_amount": row.get("actual_spent_amount"),
        "status": row.get("status"),
    }


def _build_adjustment_evidence(
    *,
    budget_week_id: Any,
    rows: list[dict[str, Any]],
) -> dict[str, Any]:
    return {
        "kind": "budget_adjustments",
        "budget_week_id": budget_week_id,
        "adjustment_count": len(rows),
        "total_adjustment_amount": sum((row.get("adjustment_amount") or 0) for row in rows),
        "items": [
            {
                "adjustment_id": row.get("id"),
                "plan_id": row.get("plan_id"),
                "adjustment_amount": row.get("adjustment_amount"),
                "adjustment_type": row.get("adjustment_type"),
                "reason": row.get("reason"),
                "created_at": _as_iso_datetime(row.get("created_at")),
            }
            for row in rows
        ],
    }


def _build_rollover_evidence(
    *,
    budget_week_id: Any,
    rows: list[dict[str, Any]],
) -> dict[str, Any]:
    return {
        "kind": "budget_rollovers",
        "budget_week_id": budget_week_id,
        "rollover_count": len(rows),
        "total_rollover_amount": sum((row.get("rollover_amount") or 0) for row in rows),
        "items": [
            {
                "rollover_id": row.get("id"),
                "source_budget_week_id": row.get("source_budget_week_id"),
                "target_budget_week_id": row.get("target_budget_week_id"),
                "rollover_amount": row.get("rollover_amount"),
                "rollover_step": row.get("rollover_step"),
                "note": row.get("note"),
                "status": row.get("status"),
                "created_at": _as_iso_datetime(row.get("created_at")),
                "resolved_at": _as_iso_datetime(row.get("resolved_at")),
            }
            for row in rows
        ],
    }


@dataclass(slots=True)
class ChatbotRepository:
    runner: Any

    @classmethod
    def from_database(cls, database: ReadOnlyDatabase) -> "ChatbotRepository":
        return cls(runner=database)

    def get_weekly_budget_status(
        self,
        *,
        user_id: int,
        base_date: date,
    ) -> dict[str, Any]:
        validate_user_id(user_id)
        rows = self.runner.execute_select(
            """
            SELECT
                id,
                week_start_date,
                week_end_date,
                planned_income_amount,
                planned_essential_amount,
                planned_saving_amount,
                rollover_in_amount,
                manual_adjustment_amount,
                final_free_amount,
                actual_spent_amount,
                status
            FROM budget_week
            WHERE users_id = :user_id
              AND is_deleted = false
              AND week_start_date <= :base_date
              AND week_end_date >= :base_date
            ORDER BY week_start_date DESC
            LIMIT 1
            """,
            parameters={
                "user_id": user_id,
                "base_date": base_date,
            },
        )

        as_of = datetime.now(timezone.utc).astimezone().isoformat()
        if not rows:
            return _build_standard_contract(
                as_of=as_of,
                window={
                    "date_from": _as_iso_date(base_date),
                    "date_to": _as_iso_date(base_date),
                },
                summary={},
                evidence=[],
                warnings=["No weekly budget found for the requested date."],
            )

        row = rows[0]
        summary = {
            "budget_week_id": row.get("id"),
            "planned_income_amount": row.get("planned_income_amount"),
            "planned_essential_amount": row.get("planned_essential_amount"),
            "planned_saving_amount": row.get("planned_saving_amount"),
            "rollover_in_amount": row.get("rollover_in_amount"),
            "manual_adjustment_amount": row.get("manual_adjustment_amount"),
            "final_free_amount": row.get("final_free_amount"),
            "actual_spent_amount": row.get("actual_spent_amount"),
            "status": row.get("status"),
        }
        adjustment_rows = self.runner.execute_select(
            """
            SELECT
                ba.id,
                ba.plan_id,
                ba.adjustment_amount,
                ba.adjustment_type,
                ba.reason,
                ba.created_at
            FROM budget_adjustment ba
            JOIN budget_week bw
              ON bw.id = ba.budget_week_id
            WHERE bw.id = :budget_week_id
              AND bw.users_id = :user_id
              AND ba.is_deleted = false
            ORDER BY ba.created_at DESC, ba.id DESC
            LIMIT 5
            """,
            parameters={
                "user_id": user_id,
                "budget_week_id": row.get("id"),
            },
        )
        rollover_rows = self.runner.execute_select(
            """
            SELECT
                br.id,
                br.source_budget_week_id,
                br.target_budget_week_id,
                br.rollover_amount,
                br.rollover_step,
                br.note,
                br.status,
                br.created_at,
                br.resolved_at
            FROM budget_rollover br
            JOIN budget_week bw
              ON bw.id = br.target_budget_week_id
            WHERE bw.id = :budget_week_id
              AND bw.users_id = :user_id
              AND br.is_deleted = false
            ORDER BY br.created_at DESC, br.id DESC
            LIMIT 5
            """,
            parameters={
                "user_id": user_id,
                "budget_week_id": row.get("id"),
            },
        )
        evidence = [
            _build_budget_week_evidence(row),
            _build_adjustment_evidence(
                budget_week_id=row.get("id"),
                rows=adjustment_rows,
            ),
            _build_rollover_evidence(
                budget_week_id=row.get("id"),
                rows=rollover_rows,
            ),
        ]
        return _build_standard_contract(
            as_of=as_of,
            window={
                "date_from": _as_iso_date(row.get("week_start_date")),
                "date_to": _as_iso_date(row.get("week_end_date")),
            },
            summary=summary,
            evidence=evidence,
            warnings=[],
        )

    def get_goal_progress_overview(
        self,
        *,
        user_id: int,
        status_filter: str | None = None,
        limit: int = 5,
    ) -> dict[str, Any]:
        validate_user_id(user_id)
        if limit <= 0:
            raise ValueError("limit must be a positive integer")

        where_clauses = [
            "users_id = :user_id",
            "is_deleted = false",
        ]
        parameters: dict[str, Any] = {
            "user_id": user_id,
            "limit": limit,
        }
        if status_filter is not None:
            where_clauses.append("status = :status_filter")
            parameters["status_filter"] = status_filter

        sql = f"""
            SELECT
                id,
                title,
                description,
                target_amount,
                saved_amount,
                start_date,
                end_date,
                priority,
                status,
                auto_allocate_enabled
            FROM plan
            WHERE {" AND ".join(where_clauses)}
            ORDER BY priority DESC, id DESC
            LIMIT :limit
        """
        rows = self.runner.execute_select(sql, parameters=parameters)

        as_of = datetime.now(timezone.utc).astimezone().isoformat()
        plan_summaries: list[dict[str, Any]] = []
        for row in rows:
            target_amount = row.get("target_amount") or 0
            saved_amount = row.get("saved_amount") or 0
            progress_ratio = None
            if target_amount:
                progress_ratio = round(float(saved_amount) / float(target_amount), 4)

            plan_summaries.append(
                {
                    "plan_id": row.get("id"),
                    "title": row.get("title"),
                    "description": row.get("description"),
                    "target_amount": target_amount,
                    "saved_amount": saved_amount,
                    "progress_ratio": progress_ratio,
                    "start_date": _as_iso_date(row.get("start_date")),
                    "end_date": _as_iso_date(row.get("end_date")),
                    "priority": row.get("priority"),
                    "status": row.get("status"),
                    "auto_allocate_enabled": row.get("auto_allocate_enabled"),
                }
            )

        warnings: list[str] = []
        if not plan_summaries:
            warnings.append("No plans found for the requested filters.")

        return _build_standard_contract(
            as_of=as_of,
            window={
                "date_from": None,
                "date_to": None,
            },
            summary={
                "goal_count": len(plan_summaries),
                "plans": plan_summaries,
            },
            evidence=plan_summaries,
            warnings=warnings,
        )
