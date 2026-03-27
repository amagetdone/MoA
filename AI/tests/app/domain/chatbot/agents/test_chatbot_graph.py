from __future__ import annotations

from datetime import date
from typing import Any, Sequence

import anyio
from langchain_core.language_models.fake_chat_models import FakeMessagesListChatModel
from langchain_core.messages import AIMessage, HumanMessage, ToolMessage
from langchain_core.tools import BaseTool, StructuredTool, tool

from app.core.database import build_thread_id
from app.domain.chatbot.agents.graph import (
    ChatbotToolBundle,
    _tools_for_route,
    build_chatbot_graph,
)
from app.domain.chatbot.agents.states.state import ChatbotGraphState
from app.domain.chatbot.agents.tools.sql_tools import build_sql_tool_middleware


class _BindableFakeChatModel(FakeMessagesListChatModel):
    def bind_tools(
        self,
        tools: Sequence[BaseTool | dict | type | Any],
        *,
        tool_choice: str | None = None,
        **kwargs: Any,
    ) -> "_BindableFakeChatModel":
        return self


class _RecordingFakeChatModel(_BindableFakeChatModel):
    def __init__(self, *, responses: list[AIMessage], name: str) -> None:
        super().__init__(responses=responses)
        self.name = name
        object.__setattr__(self, "seen_messages", [])

    def _generate(self, messages, stop=None, run_manager=None, **kwargs: Any):
        self.seen_messages.append(messages)
        return super()._generate(messages, stop=stop, run_manager=run_manager, **kwargs)


@tool
def _dummy_tool() -> str:
    """Return a dummy result."""

    return "dummy"


class _RecordingSQLToolFactory:
    def __init__(self) -> None:
        self.calls: list[dict[str, object]] = []

    def build(self) -> StructuredTool:
        def _run(sql: str, parameters: dict[str, object] | None = None) -> dict[str, object]:
            self.calls.append(
                {
                    "sql": sql,
                    "parameters": parameters or {},
                }
            )
            return {"row_count": 1, "rows": [{"category_name": "food", "average_spent": 120000}]}

        return StructuredTool.from_function(
            name="run_guarded_sql_query",
            description="Run a guarded SQL query.",
            func=_run,
        )


def _build_tool_bundle(*, sql_tool: BaseTool | None = None) -> ChatbotToolBundle:
    return ChatbotToolBundle(
        budget=[_dummy_tool],
        goals=[_dummy_tool],
        spending=[_dummy_tool],
        anomalies=[_dummy_tool],
        sql=[sql_tool] if sql_tool is not None else [],
        sql_middleware=[build_sql_tool_middleware(user_id=7)] if sql_tool is not None else [],
    )


def _build_models(
    *,
    route: str = "budget",
    validator_response: str = "APPROVE\nSELECT id, title FROM plan WHERE users_id = :user_id",
) -> dict[str, Any]:
    return {
        "supervisor_router": _RecordingFakeChatModel(
            responses=[AIMessage(content=route)],
            name="supervisor_router",
        ),
        "budget": _RecordingFakeChatModel(
            responses=[AIMessage(content="budget specialist reply")],
            name="budget_specialist",
        ),
        "goal": _RecordingFakeChatModel(
            responses=[AIMessage(content="goal specialist reply")],
            name="goal_specialist",
        ),
        "spending": _RecordingFakeChatModel(
            responses=[AIMessage(content="spending specialist reply")],
            name="spending_specialist",
        ),
        "anomaly": _RecordingFakeChatModel(
            responses=[AIMessage(content="anomaly specialist reply")],
            name="anomaly_specialist",
        ),
        "sql_generator": _RecordingFakeChatModel(
            responses=[AIMessage(content="SELECT id, title FROM plan WHERE users_id = :user_id")],
            name="sql_query_generator",
        ),
        "sql_validator": _RecordingFakeChatModel(
            responses=[AIMessage(content=validator_response)],
            name="sql_query_validator",
        ),
        "supervisor_sql_executor": _RecordingFakeChatModel(
            responses=[
                AIMessage(
                    content="",
                    tool_calls=[
                        {
                            "name": "run_guarded_sql_query",
                            "args": {
                                "sql": "SELECT 1",
                                "parameters": {},
                            },
                            "id": "sql-exec-1",
                        }
                    ],
                ),
                AIMessage(content="sql execution evidence"),
            ],
            name="supervisor_sql_executor",
        ),
        "supervisor": _RecordingFakeChatModel(
            responses=[AIMessage(content=f"{route} final")],
            name="supervisor",
        ),
    }


def _build_named_tool(*, name: str):
    def _run(**kwargs: Any) -> dict[str, object]:
        return {
            "tool_name": name,
            "args": kwargs,
        }

    return StructuredTool.from_function(
        name=name,
        description=f"{name} tool",
        func=_run,
    )


def test_chatbot_graph_state_defines_required_fields() -> None:
    annotations = ChatbotGraphState.__annotations__

    assert "messages" in annotations
    assert "warnings" in annotations
    assert "tool_summaries" in annotations
    assert "final_answer" in annotations
    assert "active_route" in annotations
    assert "candidate_sql" in annotations
    assert "validated_sql" in annotations
    assert "sql_result" in annotations


def test_build_chatbot_graph_compiles_with_checkpointer() -> None:
    models = {
        "supervisor_router": _BindableFakeChatModel(
            responses=[AIMessage(content="budget")],
        ),
        "budget": _BindableFakeChatModel(
            responses=[AIMessage(content="budget answer")],
        ),
        "goal": _BindableFakeChatModel(
            responses=[AIMessage(content="goal answer")],
        ),
        "spending": _BindableFakeChatModel(
            responses=[AIMessage(content="spending answer")],
        ),
        "anomaly": _BindableFakeChatModel(
            responses=[AIMessage(content="anomaly answer")],
        ),
        "sql_generator": _BindableFakeChatModel(
            responses=[AIMessage(content="SELECT id FROM plan WHERE users_id = :user_id")],
        ),
        "sql_validator": _BindableFakeChatModel(
            responses=[AIMessage(content="APPROVE\nSELECT id FROM plan WHERE users_id = :user_id")],
        ),
        "supervisor_sql_executor": _BindableFakeChatModel(
            responses=[
                AIMessage(
                    content="",
                    tool_calls=[
                        {
                            "name": "run_guarded_sql_query",
                            "args": {"sql": "SELECT 1", "parameters": {}},
                            "id": "sql-exec-1",
                        }
                    ],
                ),
                AIMessage(content="sql execution evidence"),
            ],
        ),
        "supervisor": _BindableFakeChatModel(
            responses=[AIMessage(content="final answer")],
        ),
    }

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=_build_tool_bundle(sql_tool=_RecordingSQLToolFactory().build()),
    )

    assert graph is not None
    assert getattr(graph, "checkpointer", None) is not None


def test_spending_route_includes_anomaly_tool_for_feedback_requests() -> None:
    tool_bundle = ChatbotToolBundle(
        spending=[
            _build_named_tool(name="get_spending_summary"),
            _build_named_tool(name="get_spending_by_category"),
        ],
        anomalies=[
            _build_named_tool(name="get_recent_spending_anomalies"),
        ],
    )

    resolved_tools = _tools_for_route(tool_bundle, "spending")

    assert [tool.name for tool in resolved_tools] == [
        "get_spending_summary",
        "get_spending_by_category",
        "get_recent_spending_anomalies",
    ]


def test_build_chatbot_graph_uses_explicit_today_in_supervisor_prompt() -> None:
    models = _build_models(route="budget")

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=_build_tool_bundle(sql_tool=_RecordingSQLToolFactory().build()),
        today=date(2026, 3, 27),
    )

    anyio.run(
        lambda: graph.ainvoke(
            {"messages": [HumanMessage(content="show me this week's budget")]},
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    supervisor_prompt = " ".join(
        str(getattr(message, "content", ""))
        for message in models["supervisor"].seen_messages[0]
    )
    assert "Today is 2026-03-27 in the user's local timezone." in supervisor_prompt


def test_thread_id_is_user_scoped() -> None:
    assert build_thread_id(user_id=7, conversation_id="same") != build_thread_id(
        user_id=8,
        conversation_id="same",
    )


def test_sql_questions_follow_generator_validator_executor_sequence() -> None:
    sql_tool_factory = _RecordingSQLToolFactory()
    models = _build_models(route="sql")

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=_build_tool_bundle(sql_tool=sql_tool_factory.build()),
    )

    result = anyio.run(
        lambda: graph.ainvoke(
            {
                "messages": [
                    HumanMessage(
                        content="show average monthly spending by category for the last 3 months"
                    )
                ]
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    assert len(models["supervisor_router"].seen_messages) == 1
    assert len(models["sql_generator"].seen_messages) == 1
    assert len(models["sql_validator"].seen_messages) == 1
    assert len(models["supervisor_sql_executor"].seen_messages) == 2
    assert len(models["supervisor"].seen_messages) == 1
    assert len(models["budget"].seen_messages) == 0
    assert len(models["spending"].seen_messages) == 0
    assert sql_tool_factory.calls == [
        {
            "sql": "SELECT id, title FROM plan WHERE users_id = :user_id",
            "parameters": {"user_id": 7},
        }
    ]
    assert result["validated_sql"] == "SELECT id, title FROM plan WHERE users_id = :user_id"
    assert result["active_route"] == "sql"
    assert result["final_answer"] == "sql final"


def test_supervisor_receives_clean_specialist_evidence_message() -> None:
    models = _build_models(route="budget")

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=_build_tool_bundle(sql_tool=_RecordingSQLToolFactory().build()),
    )

    anyio.run(
        lambda: graph.ainvoke(
            {"messages": [HumanMessage(content="how much budget is left this week")]},
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    supervisor_input = models["supervisor"].seen_messages[0]
    assert any(
        isinstance(message, AIMessage)
        and getattr(message, "name", None) == "budget_specialist"
        and message.content == "budget specialist reply"
        for message in supervisor_input
    )


def test_spending_specialist_tool_calls_are_recorded_in_tool_summaries() -> None:
    models = _build_models(route="spending")
    models["spending"] = _RecordingFakeChatModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "get_spending_summary",
                        "args": {"period": "this_week"},
                        "id": "spending-call-1",
                    }
                ],
            ),
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "get_spending_by_category",
                        "args": {
                            "period": "this_week",
                            "include_comparison": False,
                        },
                        "id": "spending-call-2",
                    }
                ],
            ),
            AIMessage(content="spending specialist reply"),
        ],
        name="spending_specialist",
    )

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=ChatbotToolBundle(
            spending=[
                _build_named_tool(name="get_spending_summary"),
                _build_named_tool(name="get_spending_by_category"),
            ]
        ),
    )

    result = anyio.run(
        lambda: graph.ainvoke(
            {"messages": [HumanMessage(content="give me this week's spending feedback")]},
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    assert result["tool_summaries"] == [
        {
            "tool_name": "get_spending_summary",
            "status": "completed",
        },
        {
            "tool_name": "get_spending_by_category",
            "status": "completed",
        },
    ]


def test_spending_specialist_normalizes_missing_tool_call_ids_before_follow_up_model_call() -> None:
    models = _build_models(route="spending")
    models["spending"] = _RecordingFakeChatModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "get_spending_summary",
                        "args": {"period": "this_week"},
                        "id": None,
                    },
                    {
                        "name": "get_spending_by_category",
                        "args": {
                            "period": "this_week",
                            "include_comparison": False,
                        },
                        "id": None,
                    },
                ],
            ),
            AIMessage(content="spending specialist reply"),
        ],
        name="spending_specialist",
    )

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=ChatbotToolBundle(
            spending=[
                _build_named_tool(name="get_spending_summary"),
                _build_named_tool(name="get_spending_by_category"),
            ]
        ),
    )

    anyio.run(
        lambda: graph.ainvoke(
            {"messages": [HumanMessage(content="give me this week's spending feedback")]},
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    follow_up_input = models["spending"].seen_messages[1]
    tool_call_message = next(
        message
        for message in follow_up_input
        if isinstance(message, AIMessage)
        and getattr(message, "tool_calls", None)
    )
    tool_call_ids = [tool_call["id"] for tool_call in tool_call_message.tool_calls]
    assert all(isinstance(tool_call_id, str) and tool_call_id for tool_call_id in tool_call_ids)
    assert [
        message.tool_call_id
        for message in follow_up_input
        if isinstance(message, ToolMessage)
    ] == tool_call_ids


def test_spending_specialist_removes_invalid_tool_calls_before_follow_up_model_call() -> None:
    models = _build_models(route="spending")
    models["spending"] = _RecordingFakeChatModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "get_spending_summary",
                        "args": {"period": "this_week"},
                        "id": "spending-call-1",
                    }
                ],
                invalid_tool_calls=[
                    {
                        "name": None,
                        "args": None,
                        "id": None,
                        "error": "tool name missing",
                        "type": "invalid_tool_call",
                    }
                ],
            ),
            AIMessage(content="spending specialist reply"),
        ],
        name="spending_specialist",
    )

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=ChatbotToolBundle(
            spending=[
                _build_named_tool(name="get_spending_summary"),
            ]
        ),
    )

    anyio.run(
        lambda: graph.ainvoke(
            {"messages": [HumanMessage(content="give me this week's spending feedback")]},
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    follow_up_input = models["spending"].seen_messages[1]
    tool_call_message = next(
        message
        for message in follow_up_input
        if isinstance(message, AIMessage)
        and getattr(message, "tool_calls", None)
    )
    assert tool_call_message.tool_calls == [
        {
            "name": "get_spending_summary",
            "args": {"period": "this_week"},
            "id": "spending-call-1",
            "type": "tool_call",
        }
    ]
    assert tool_call_message.invalid_tool_calls == []


def test_supervisor_receives_clean_sql_execution_evidence_message() -> None:
    models = _build_models(route="sql")

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=_build_tool_bundle(sql_tool=_RecordingSQLToolFactory().build()),
    )

    anyio.run(
        lambda: graph.ainvoke(
            {
                "messages": [
                    HumanMessage(
                        content="show average monthly spending by category for the last 3 months"
                    )
                ]
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    supervisor_input = models["supervisor"].seen_messages[0]
    assert any(
        isinstance(message, AIMessage)
        and getattr(message, "name", None) == "supervisor_sql_executor"
        and message.content == "sql execution evidence"
        for message in supervisor_input
    )


def test_sql_top_level_history_keeps_only_clean_execution_evidence() -> None:
    sql_tool_factory = _RecordingSQLToolFactory()
    models = _build_models(route="sql")

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=_build_tool_bundle(sql_tool=sql_tool_factory.build()),
    )

    result = anyio.run(
        lambda: graph.ainvoke(
            {
                "messages": [
                    HumanMessage(
                        content="show average monthly spending by category for the last 3 months"
                    )
                ]
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    top_level_names = [
        getattr(message, "name", None)
        for message in result["messages"]
        if isinstance(message, AIMessage)
    ]
    assert "sql_query_generator" not in top_level_names
    assert "sql_query_validator" not in top_level_names
    assert top_level_names.count("supervisor_sql_executor") == 1
    assert top_level_names.count("supervisor") == 1
    executor_message = next(
        message
        for message in result["messages"]
        if isinstance(message, AIMessage)
        and getattr(message, "name", None) == "supervisor_sql_executor"
    )
    assert executor_message.tool_calls == []
    assert executor_message.content == "sql execution evidence"


def test_prune_history_removes_stale_internal_messages_before_router() -> None:
    sql_tool_factory = _RecordingSQLToolFactory()
    models = _build_models(route="budget")

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=_build_tool_bundle(sql_tool=sql_tool_factory.build()),
    )

    result = anyio.run(
        lambda: graph.ainvoke(
            {
                "messages": [
                    HumanMessage(content="show me last week's summary"),
                    AIMessage(content="previous final", name="supervisor"),
                    AIMessage(
                        content="",
                        name="supervisor_sql_executor",
                        tool_calls=[
                            {
                                "name": "run_guarded_sql_query",
                                "args": {"sql": "SELECT 1", "parameters": {}},
                                "id": None,
                            }
                        ],
                    ),
                    ToolMessage(
                        content='{"row_count": 1, "rows": []}',
                        name="run_guarded_sql_query",
                        tool_call_id="stale-call-1",
                    ),
                    HumanMessage(content="how much budget is left this week"),
                ]
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    router_input = models["supervisor_router"].seen_messages[0]
    assert not any(isinstance(message, ToolMessage) for message in router_input)
    assert not any(
        isinstance(message, AIMessage)
        and getattr(message, "name", None) == "supervisor_sql_executor"
        for message in router_input
    )
    top_level_names = [
        getattr(message, "name", None)
        for message in result["messages"]
        if isinstance(message, AIMessage)
    ]
    assert "supervisor_sql_executor" not in top_level_names
    assert result["final_answer"] == "budget final"


def test_non_sql_questions_skip_sql_pipeline_and_use_domain_specialist() -> None:
    sql_tool_factory = _RecordingSQLToolFactory()
    models = _build_models(route="budget")

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=_build_tool_bundle(sql_tool=sql_tool_factory.build()),
    )

    result = anyio.run(
        lambda: graph.ainvoke(
            {"messages": [HumanMessage(content="how much budget is left this week")]},
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    assert len(models["supervisor_router"].seen_messages) == 1
    assert len(models["budget"].seen_messages) == 1
    assert len(models["sql_generator"].seen_messages) == 0
    assert len(models["sql_validator"].seen_messages) == 0
    assert len(models["supervisor_sql_executor"].seen_messages) == 0
    assert len(models["supervisor"].seen_messages) == 1
    assert sql_tool_factory.calls == []
    assert result["active_route"] == "budget"
    assert result["final_answer"] == "budget final"


def test_validator_rejection_skips_sql_execution_and_surfaces_rejection_context() -> None:
    sql_tool_factory = _RecordingSQLToolFactory()
    models = _build_models(
        route="sql",
        validator_response="REJECT\nThe query is missing user scoping.",
    )

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=_build_tool_bundle(sql_tool=sql_tool_factory.build()),
    )

    result = anyio.run(
        lambda: graph.ainvoke(
            {"messages": [HumanMessage(content="show me every user's spending")]},
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    assert len(models["sql_generator"].seen_messages) == 1
    assert len(models["sql_validator"].seen_messages) == 1
    assert len(models["supervisor_sql_executor"].seen_messages) == 0
    assert len(models["supervisor"].seen_messages) == 1
    assert sql_tool_factory.calls == []
    assert result["validator_decision"] == "reject"
    assert result["validator_feedback"] == "The query is missing user scoping."
