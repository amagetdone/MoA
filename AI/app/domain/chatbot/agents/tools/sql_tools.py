from __future__ import annotations

import re
from typing import Any

from langchain.agents.middleware import AgentMiddleware
from langchain_core.messages import ToolMessage
from langchain_core.tools import StructuredTool
from langgraph.prebuilt.tool_node import ToolCallRequest
from pydantic import BaseModel, ConfigDict, Field

from app.core.database import assert_select_only_sql


_SQL_BLOCK_COMMENT_RE = re.compile(r"/\*.*?\*/", re.DOTALL)
_SQL_LINE_COMMENT_RE = re.compile(r"--[^\n]*")
_SQL_SINGLE_QUOTED_STRING_RE = re.compile(r"'(?:''|[^'])*'")
_SQL_CTE_NAME_RE = re.compile(r"(?:with|,)\s*([a-zA-Z_][\w]*)\s+as\s*\(", re.IGNORECASE)
_SQL_TABLE_REFERENCE_RE = re.compile(
    r"\b(?:from|join)\s+([a-zA-Z_][\w]*(?:\.[a-zA-Z_][\w]*)?)\b",
    re.IGNORECASE,
)
_USER_SCOPE_RE = re.compile(r"(?:\b[a-z_][\w]*\.)?users_id\s*=\s*:user_id\b")

ALLOWED_SQL_TABLES = frozenset(
    {
        "account",
        "account_transaction_override",
        "budget_adjustment",
        "budget_rollover",
        "budget_week",
        "card",
        "card_transaction_override",
        "category",
        "plan",
        "transaction_analysis_metadata",
        "transaction_analysis_result",
        "transaction_override",
    }
)
SQL_QUERY_TOOL_NAME = "run_guarded_sql_query"


class _SQLQueryToolInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    sql: str = Field(
        ...,
        description=(
            "A single read-only SQL query over the chatbot finance tables. "
            "Every query must stay user-scoped with users_id = :user_id."
        ),
    )
    parameters: dict[str, Any] = Field(
        default_factory=dict,
        description="Named SQL parameters excluding user_id, which is injected by middleware.",
    )


def build_sql_query_tool(*, database: Any) -> StructuredTool:
    def _run(*, sql: str, parameters: dict[str, Any] | None = None) -> dict[str, Any]:
        rows = database.execute_select(sql, parameters=parameters or {})
        return {
            "row_count": len(rows),
            "rows": rows,
        }

    return StructuredTool.from_function(
        name=SQL_QUERY_TOOL_NAME,
        description=(
            "Execute a guarded read-only SQL query against the authenticated user's "
            "allowed chatbot finance tables."
        ),
        func=_run,
        args_schema=_SQLQueryToolInput,
    )


def build_sql_tool_middleware(*, user_id: int) -> AgentMiddleware:
    class SQLGuardrailMiddleware(AgentMiddleware):
        def wrap_tool_call(self, request: ToolCallRequest, handler):
            if request.tool_call["name"] != SQL_QUERY_TOOL_NAME:
                return handler(request)

            try:
                guarded_request = _build_guarded_request(request=request, user_id=user_id)
                return handler(guarded_request)
            except ValueError as exc:
                return _build_guardrail_rejection_message(request=request, error=exc)

        async def awrap_tool_call(self, request: ToolCallRequest, handler):
            if request.tool_call["name"] != SQL_QUERY_TOOL_NAME:
                return await handler(request)

            try:
                guarded_request = _build_guarded_request(request=request, user_id=user_id)
                return await handler(guarded_request)
            except ValueError as exc:
                return _build_guardrail_rejection_message(request=request, error=exc)

    return SQLGuardrailMiddleware()


def _build_guarded_request(
    *,
    request: ToolCallRequest,
    user_id: int,
) -> ToolCallRequest:
    args = dict(request.tool_call.get("args") or {})
    sql_from_state = _resolve_validated_sql_from_state(request.state)
    sql_to_execute = sql_from_state or str(args.get("sql") or "")
    validated_sql = validate_guarded_sql(sql_to_execute)
    sanitized_parameters = _sanitize_sql_parameters(
        args.get("parameters"),
        user_id=user_id,
    )
    return request.override(
        tool_call={
            **request.tool_call,
            "args": {
                **args,
                "sql": validated_sql,
                "parameters": sanitized_parameters,
            },
        }
    )


def validate_guarded_sql(sql: str) -> str:
    validated_sql = assert_select_only_sql(sql)
    normalized_sql = _normalize_sql_for_guardrail(validated_sql)
    referenced_tables = _extract_referenced_tables(normalized_sql)
    if not referenced_tables:
        raise ValueError("SQL must reference at least one allowed table")

    for table_name in referenced_tables:
        if table_name not in ALLOWED_SQL_TABLES:
            raise ValueError(f"table '{table_name}' is not in the allowed tables only list")

    lowered_sql = normalized_sql.casefold()
    if ":user_id" not in lowered_sql:
        raise ValueError("SQL must include the :user_id parameter")
    if _USER_SCOPE_RE.search(lowered_sql) is None:
        raise ValueError("SQL must keep users_id = :user_id user scoping")
    return validated_sql


def _sanitize_sql_parameters(
    parameters: Any,
    *,
    user_id: int,
) -> dict[str, Any]:
    if parameters is None:
        sanitized_parameters: dict[str, Any] = {}
    elif isinstance(parameters, dict):
        sanitized_parameters = dict(parameters)
    else:
        raise ValueError("parameters must be an object of named SQL parameters")

    if "user_id" in sanitized_parameters:
        raise ValueError("parameters must not override user_id")
    sanitized_parameters["user_id"] = user_id
    return sanitized_parameters


def _resolve_validated_sql_from_state(state: Any) -> str | None:
    if not isinstance(state, dict):
        return None
    validated_sql = state.get("validated_sql")
    if not isinstance(validated_sql, str):
        return None
    normalized_sql = validated_sql.strip()
    return normalized_sql or None


def _extract_referenced_tables(normalized_sql: str) -> set[str]:
    cte_names = {
        match.group(1).casefold()
        for match in _SQL_CTE_NAME_RE.finditer(normalized_sql)
    }
    referenced_tables: set[str] = set()
    for match in _SQL_TABLE_REFERENCE_RE.finditer(normalized_sql):
        identifier = match.group(1).casefold()
        if identifier in cte_names:
            continue
        if "." in identifier:
            schema_name, table_name = identifier.split(".", 1)
            if schema_name != "public":
                raise ValueError(f"schema '{schema_name}' is not allowed")
        else:
            table_name = identifier
        referenced_tables.add(table_name)
    return referenced_tables


def _normalize_sql_for_guardrail(sql: str) -> str:
    without_block_comments = _SQL_BLOCK_COMMENT_RE.sub(" ", sql)
    without_line_comments = _SQL_LINE_COMMENT_RE.sub(" ", without_block_comments)
    without_strings = _SQL_SINGLE_QUOTED_STRING_RE.sub("''", without_line_comments)
    return without_strings.strip()


def _build_guardrail_rejection_message(
    *,
    request: ToolCallRequest,
    error: ValueError,
) -> ToolMessage:
    return ToolMessage(
        content=f"Guardrail rejected SQL: {error}",
        tool_call_id=request.tool_call["id"],
        name=request.tool_call["name"],
    )
