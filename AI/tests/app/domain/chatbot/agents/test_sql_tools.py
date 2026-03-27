from __future__ import annotations

import json
from types import SimpleNamespace

from langchain_core.messages import ToolMessage
from langgraph.prebuilt.tool_node import ToolCallRequest

from app.domain.chatbot.agents.tools.sql_tools import (
    build_sql_query_tool,
    build_sql_tool_middleware,
)


class _FakeDatabase:
    def __init__(self) -> None:
        self.calls: list[dict[str, object]] = []

    def execute_select(self, sql: str, *, parameters=None):
        self.calls.append(
            {
                "sql": sql,
                "parameters": parameters or {},
            }
        )
        return [{"total_spent": 120000}]


def _execute_request(request: ToolCallRequest) -> ToolMessage:
    payload = request.tool.invoke(request.tool_call["args"])
    return ToolMessage(
        content=json.dumps(payload, ensure_ascii=False),
        tool_call_id=request.tool_call["id"],
        name=request.tool_call["name"],
    )


def test_sql_tool_middleware_executes_user_scoped_read_only_query() -> None:
    database = _FakeDatabase()
    tool = build_sql_query_tool(database=database)
    middleware = build_sql_tool_middleware(user_id=7)
    request = ToolCallRequest(
        tool_call={
            "name": "run_guarded_sql_query",
            "args": {
                "sql": "SELECT id, title FROM plan WHERE users_id = :user_id",
                "parameters": {},
            },
            "id": "call-1",
            "type": "tool_call",
        },
        tool=tool,
        state={"messages": []},
        runtime=SimpleNamespace(),
    )

    result = middleware.wrap_tool_call(request, _execute_request)

    assert isinstance(result, ToolMessage)
    assert database.calls == [
        {
            "sql": "SELECT id, title FROM plan WHERE users_id = :user_id",
            "parameters": {"user_id": 7},
        }
    ]


def test_sql_tool_middleware_rejects_non_whitelisted_tables_before_execution() -> None:
    database = _FakeDatabase()
    tool = build_sql_query_tool(database=database)
    middleware = build_sql_tool_middleware(user_id=7)
    request = ToolCallRequest(
        tool_call={
            "name": "run_guarded_sql_query",
            "args": {
                "sql": "SELECT id FROM users WHERE users_id = :user_id",
                "parameters": {},
            },
            "id": "call-2",
            "type": "tool_call",
        },
        tool=tool,
        state={"messages": []},
        runtime=SimpleNamespace(),
    )

    result = middleware.wrap_tool_call(request, _execute_request)

    assert isinstance(result, ToolMessage)
    assert "Guardrail rejected SQL" in str(result.content)
    assert database.calls == []


def test_sql_tool_middleware_rejects_queries_without_user_scope_before_execution() -> None:
    database = _FakeDatabase()
    tool = build_sql_query_tool(database=database)
    middleware = build_sql_tool_middleware(user_id=7)
    request = ToolCallRequest(
        tool_call={
            "name": "run_guarded_sql_query",
            "args": {
                "sql": "SELECT id, title FROM plan",
                "parameters": {},
            },
            "id": "call-3",
            "type": "tool_call",
        },
        tool=tool,
        state={"messages": []},
        runtime=SimpleNamespace(),
    )

    result = middleware.wrap_tool_call(request, _execute_request)

    assert isinstance(result, ToolMessage)
    assert "Guardrail rejected SQL" in str(result.content)
    assert database.calls == []


def test_sql_tool_middleware_rejects_write_queries_before_execution() -> None:
    database = _FakeDatabase()
    tool = build_sql_query_tool(database=database)
    middleware = build_sql_tool_middleware(user_id=7)
    request = ToolCallRequest(
        tool_call={
            "name": "run_guarded_sql_query",
            "args": {
                "sql": "DELETE FROM plan WHERE users_id = :user_id",
                "parameters": {},
            },
            "id": "call-4",
            "type": "tool_call",
        },
        tool=tool,
        state={"messages": []},
        runtime=SimpleNamespace(),
    )

    result = middleware.wrap_tool_call(request, _execute_request)

    assert isinstance(result, ToolMessage)
    assert "Guardrail rejected SQL" in str(result.content)
    assert database.calls == []
