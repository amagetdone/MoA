from __future__ import annotations

import json
from datetime import date
from types import SimpleNamespace
from typing import Any, Sequence

import anyio
import httpx
import pytest
from langchain_core.language_models.fake_chat_models import FakeMessagesListChatModel
from langchain_core.messages import AIMessage, HumanMessage
from langchain_core.tools import BaseTool, StructuredTool, ToolException
from pydantic import BaseModel, Field

from app.core.config import Settings
from app.domain.chatbot.errors import ChatbotUpstreamUnavailableError
from app.domain.chatbot.agents.graph import (
    ChatbotToolBundle,
    _build_recoverable_tool_error_message,
    build_chatbot_graph,
)
from app.domain.chatbot.agents.states.state import ChatbotGraphState


class _BindableFakeChatModel(FakeMessagesListChatModel):
    def bind_tools(
        self,
        tools: Sequence[BaseTool | dict | type | Any],
        *,
        tool_choice: str | None = None,
        **kwargs: Any,
    ) -> "_BindableFakeChatModel":
        return self


def _build_chat_model(*, response_text: str) -> _BindableFakeChatModel:
    return _BindableFakeChatModel(responses=[AIMessage(content=response_text)])


class _BudgetHomeToolInput(BaseModel):
    topic: str = Field(default="weekly coaching")
    user_id: int | None = Field(default=None)


class _WeeklySpendingAnalysisToolInput(BaseModel):
    yearWeek: str
    user_id: int | None = Field(default=None)


class _ToolRecorder:
    def __init__(self) -> None:
        self.calls: list[dict[str, object]] = []

    def build(self) -> StructuredTool:
        def _run(*, topic: str, user_id: int | None = None) -> dict[str, object]:
            self.calls.append({"topic": topic, "user_id": user_id})
            if user_id is None:
                raise AssertionError("user_id must be injected by the server")
            return {
                "tool_name": "get_budget_home",
                "status": "completed",
                "topic": topic,
                "user_id": user_id,
            }

        return StructuredTool.from_function(
            name="get_budget_home",
            description="Fake MCP-like tool for the spending coach.",
            func=_run,
            args_schema=_BudgetHomeToolInput,
        )


class _JsonSchemaToolRecorder:
    def __init__(self) -> None:
        self.calls: list[dict[str, object]] = []

    def build(self) -> StructuredTool:
        def _run(*, topic: str, user_id: int | None = None) -> dict[str, object]:
            self.calls.append({"topic": topic, "user_id": user_id})
            if user_id is None:
                raise AssertionError("user_id must be injected by the server")
            return {
                "tool_name": "get_budget_home",
                "status": "completed",
                "topic": topic,
                "user_id": user_id,
            }

        return StructuredTool(
            name="get_budget_home",
            description="Fake JSON-schema MCP-like tool for the spending coach.",
            func=_run,
            args_schema={
                "type": "object",
                "properties": {
                    "topic": {"type": "string"},
                    "user_id": {"type": "integer"},
                },
                "required": ["topic", "user_id"],
                "additionalProperties": False,
            },
        )


class _FailingToolRecorder:
    def build(self) -> StructuredTool:
        def _run(*, topic: str, user_id: int | None = None) -> dict[str, object]:
            if user_id is None:
                raise AssertionError("user_id must be injected by the server")
            raise ToolException("User not found")

        return StructuredTool.from_function(
            name="get_budget_home",
            description="Fake MCP-like tool that fails when finance context is missing.",
            func=_run,
            args_schema=_BudgetHomeToolInput,
        )


class _WeeklySpendingAnalysisRecorder:
    def __init__(self) -> None:
        self.calls: list[dict[str, object]] = []

    def build(self) -> StructuredTool:
        def _run(*, yearWeek: str, user_id: int | None = None) -> dict[str, object]:
            self.calls.append({"yearWeek": yearWeek, "user_id": user_id})
            if user_id is None:
                raise AssertionError("user_id must be injected by the server")
            return {
                "tool_name": "get_weekly_spending_analysis",
                "status": "completed",
                "yearWeek": yearWeek,
                "user_id": user_id,
            }

        return StructuredTool.from_function(
            name="get_weekly_spending_analysis",
            description="Fake weekly spending analysis tool for the spending coach.",
            func=_run,
            args_schema=_WeeklySpendingAnalysisToolInput,
        )


class _FakeSpan:
    def __init__(self, record: dict[str, object]) -> None:
        self.record = record

    def __enter__(self) -> "_FakeSpan":
        self.record["entered"] = True
        return self

    def __exit__(self, exc_type, exc, tb) -> bool:
        self.record["exited"] = True
        if exc is not None:
            self.update(
                metadata={
                    "error_type": exc.__class__.__name__,
                    "error_message": str(exc),
                }
            )
        return False

    def update(self, **kwargs: object) -> None:
        self.record.setdefault("updates", []).append(kwargs)


class _FakeTracer:
    def __init__(self) -> None:
        self.calls: list[dict[str, object]] = []

    @property
    def enabled(self) -> bool:
        return True

    def start_as_current_span(self, **kwargs):
        self.calls.append(kwargs)
        return _FakeSpan(kwargs)


def _build_settings() -> Settings:
    return Settings(
        _env_file=None,
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://example.com/v1",
        CHAT_MODEL="gpt-4.1-mini",
    )


def _build_graph(*, response_text: str = "This week looks a little tight, so start small.") -> Any:
    model = _build_chat_model(response_text=response_text)
    return build_chatbot_graph(
        model_factory=lambda _role: model,
        tools=ChatbotToolBundle.empty(),
        settings=_build_settings(),
        today=date(2026, 3, 27),
    )


def test_chatbot_graph_state_is_simplified_for_the_mcp_coach_runtime() -> None:
    annotations = ChatbotGraphState.__annotations__

    required_fields = {
        "messages",
        "user_id",
        "session_id",
        "user_query",
        "warnings",
        "tool_summaries",
        "final_response",
        "final_answer",
    }
    forbidden_fields = {
        "remaining_steps",
        "candidate_sql",
        "sql_validation_report",
        "approved_sql",
        "sql_result",
        "sql_execution_error",
        "coach_draft_markdown",
        "coach_validation_report",
        "sql_retry_count",
        "coach_retry_count",
        "failure_reason",
        "sql_retry_feedback",
        "sql_retry_context",
        "last_candidate_sql",
        "coach_retry_feedback",
    }

    for field in required_fields:
        assert field in annotations
    for field in forbidden_fields:
        assert field not in annotations


def test_build_chatbot_graph_returns_final_response_contract() -> None:
    graph = _build_graph(response_text="This week, reduce fixed expenses first.")

    result = anyio.run(
        lambda: graph.ainvoke(
            {
                "messages": [
                    HumanMessage(content="What should I watch this week?"),
                ],
                "user_id": 7,
                "session_id": "conv-1",
                "user_query": "What should I watch this week?",
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    assert isinstance(result, dict)
    assert result["final_response"].strip()
    assert result["final_answer"].strip()
    assert isinstance(result["warnings"], list)
    assert isinstance(result["tool_summaries"], list)
    assert "mcp" not in result["final_response"].casefold()
    assert "supervisor" not in result["final_response"].casefold()
    assert all(
        "supervisor" not in str(item).casefold()
        for item in result["tool_summaries"]
    )


def test_build_chatbot_graph_uses_env_backed_settings_for_mcp_autoload(monkeypatch) -> None:
    monkeypatch.setenv("MORE_FINANCE_MCP_URL", "http://backend:8080/api/mcp")

    graph = build_chatbot_graph(
        model_factory=lambda _role: _build_chat_model(response_text="Coach gently."),
        tools=ChatbotToolBundle.empty(),
        settings=None,
        today=date(2026, 3, 27),
    )

    assert graph._autoload_mcp_tools is True


def test_build_chatbot_graph_raises_domain_error_when_mcp_tool_loading_is_unauthorized(
    monkeypatch,
) -> None:
    class _UnauthorizedMcpClient:
        def __init__(self, *_args, **_kwargs) -> None:
            pass

        async def get_tools(self):
            request = httpx.Request("POST", "http://localhost:8080/api/mcp")
            response = httpx.Response(401, request=request)
            raise httpx.HTTPStatusError(
                "Client error '401 ' for url 'http://localhost:8080/api/mcp'",
                request=request,
                response=response,
            )

    monkeypatch.setattr(
        "app.domain.chatbot.agents.graph.MultiServerMCPClient",
        _UnauthorizedMcpClient,
    )
    graph = build_chatbot_graph(
        model_factory=lambda _role: _build_chat_model(response_text="Coach gently."),
        tools=ChatbotToolBundle.empty(),
        settings=Settings(
            _env_file=None,
            AI_API_KEY="relay-key",
            OPENAI_BASE_URL="https://example.com/v1",
            CHAT_MODEL="gpt-4.1-mini",
            MORE_FINANCE_MCP_URL="http://localhost:8080/api/mcp",
        ),
        today=date(2026, 3, 27),
    )

    with pytest.raises(
        ChatbotUpstreamUnavailableError,
        match="temporarily unavailable.*401",
    ):
        anyio.run(
            lambda: graph.ainvoke(
                {
                    "messages": [HumanMessage(content="Coach my spending habits.")],
                    "user_id": 7,
                    "session_id": "conv-1",
                    "user_query": "Coach my spending habits.",
                },
                config={"configurable": {"thread_id": "moa:7:conv-1"}},
            )
        )


def test_build_chatbot_graph_does_not_fabricate_a_fallback_answer_when_safety_scrubs_everything() -> None:
    graph = _build_graph(response_text="MCP supervisor")

    result = anyio.run(
        lambda: graph.ainvoke(
            {
                "messages": [
                    HumanMessage(content="What should I watch this week?"),
                ],
                "user_id": 7,
                "session_id": "conv-1",
                "user_query": "What should I watch this week?",
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    assert result["final_response"] == ""
    assert result["final_answer"] == ""


def test_build_chatbot_graph_drops_internal_leak_responses_without_mutating_them() -> None:
    graph = _build_graph(response_text="I used MCP with the supervisor.")

    result = anyio.run(
        lambda: graph.ainvoke(
            {
                "messages": [
                    AIMessage(content="Previous conversation summary:\nprior recap"),
                    HumanMessage(content="What should I watch this week?"),
                ],
                "user_id": 7,
                "session_id": "conv-1",
                "user_query": "What should I watch this week?",
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    assert result["final_response"] == ""
    assert result["final_answer"] == ""


def test_build_chatbot_graph_executes_a_fake_mcp_like_tool_call() -> None:
    recorder = _ToolRecorder()
    model = _BindableFakeChatModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "get_budget_home",
                        "args": {"topic": "weekly coaching"},
                        "id": "call-1",
                        "type": "tool_call",
                    }
                ],
            ),
            AIMessage(content="You are on track if you keep impulse buys small."),
        ]
    )
    graph = build_chatbot_graph(
        model_factory=lambda _role: model,
        tools=ChatbotToolBundle(schema=[recorder.build()]),
        settings=_build_settings(),
        today=date(2026, 3, 27),
    )

    result = anyio.run(
        lambda: graph.ainvoke(
            {
                "messages": [
                    HumanMessage(content="Coach my spending habits."),
                ],
                "user_id": 7,
                "session_id": "conv-1",
                "user_query": "Coach my spending habits.",
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    assert recorder.calls == [{"topic": "weekly coaching", "user_id": 7}]
    assert result["tool_summaries"] == [
        {
            "tool_name": "get_budget_home",
            "status": "completed",
        }
    ]
    assert result["final_answer"].strip()
    assert "supervisor" not in result["final_answer"].casefold()


def test_build_chatbot_graph_injects_user_id_for_json_schema_mcp_tools() -> None:
    recorder = _JsonSchemaToolRecorder()
    model = _BindableFakeChatModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "get_budget_home",
                        "args": {
                            "topic": "weekly coaching",
                            "user_id": 12345,
                        },
                        "id": "call-1",
                        "type": "tool_call",
                    }
                ],
            ),
            AIMessage(content="Keep this week's impulse spending small."),
        ]
    )
    graph = build_chatbot_graph(
        model_factory=lambda _role: model,
        tools=ChatbotToolBundle(schema=[recorder.build()]),
        settings=_build_settings(),
        today=date(2026, 3, 27),
    )

    anyio.run(
        lambda: graph.ainvoke(
            {
                "messages": [
                    HumanMessage(content="Coach my spending habits this week."),
                ],
                "user_id": 7,
                "session_id": "conv-1",
                "user_query": "Coach my spending habits this week.",
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    assert recorder.calls == [{"topic": "weekly coaching", "user_id": 7}]


def test_build_chatbot_graph_recovers_when_invalid_tool_call_is_missing_an_id() -> None:
    recorder = _ToolRecorder()
    model = _BindableFakeChatModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "get_budget_dashbord",
                        "args": {"topic": "weekly coaching"},
                        "id": None,
                        "type": "tool_call",
                    }
                ],
            ),
            AIMessage(content="I'll continue with the tools that are actually available."),
        ]
    )
    graph = build_chatbot_graph(
        model_factory=lambda _role: model,
        tools=ChatbotToolBundle(schema=[recorder.build()]),
        settings=_build_settings(),
        today=date(2026, 3, 27),
    )

    result = anyio.run(
        lambda: graph.ainvoke(
            {
                "messages": [
                    HumanMessage(content="Coach my spending habits."),
                ],
                "user_id": 7,
                "session_id": "conv-1",
                "user_query": "Coach my spending habits.",
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    assert recorder.calls == []
    assert result["tool_summaries"] == [
        {
            "tool_name": "get_budget_dashbord",
            "status": "completed",
        }
    ]
    assert result["final_answer"] == "I'll continue with the tools that are actually available."


def test_build_chatbot_graph_allows_general_coaching_when_tool_reports_missing_user_data() -> None:
    model = _BindableFakeChatModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "get_budget_home",
                        "args": {"topic": "weekly coaching"},
                        "id": "call-1",
                        "type": "tool_call",
                    }
                ],
            ),
            AIMessage(content="You are on track if you keep impulse buys small."),
        ]
    )
    graph = build_chatbot_graph(
        model_factory=lambda _role: model,
        tools=ChatbotToolBundle(schema=[_FailingToolRecorder().build()]),
        settings=_build_settings(),
        today=date(2026, 3, 27),
    )

    result = anyio.run(
        lambda: graph.ainvoke(
            {
                "messages": [
                    HumanMessage(content="Coach my spending habits."),
                ],
                "user_id": 7,
                "session_id": "conv-1",
                "user_query": "Coach my spending habits.",
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    assert result["final_answer"] == "You are on track if you keep impulse buys small."
    assert result["tool_summaries"] == [
        {
            "tool_name": "get_budget_home",
            "status": "no_data",
        }
    ]
    assert result["warnings"] == [
        "Current finance records were not available for this user context."
    ]


def test_recoverable_budget_no_data_message_suggests_weekly_spending_fallback() -> None:
    request = SimpleNamespace(
        tool=SimpleNamespace(name="get_budget_home"),
        tool_call={
            "id": "call-1",
            "name": "get_budget_home",
            "args": {"topic": "weekly coaching"},
        },
        runtime=SimpleNamespace(
            context=SimpleNamespace(
                user_id=7,
                session_id="conv-1",
                user_query="이번주 소비 내역 피드백 해줘",
            )
        ),
    )

    message = _build_recoverable_tool_error_message(
        request,
        ToolException("User not found"),
    )

    assert message is not None
    payload = message.content
    assert isinstance(payload, str)
    parsed = json.loads(payload)
    assert parsed["status"] == "no_data"
    assert parsed["suggested_next_tools"] == [
        "get_weekly_spending_analysis",
        "get_weekly_expense_graph",
        "get_transaction_list",
    ]
    assert "Before giving general coaching" in parsed["observation"]
    assert "get_weekly_spending_analysis" in parsed["observation"]


def test_build_chatbot_graph_can_continue_after_budget_no_data_with_weekly_spending_fallback() -> None:
    weekly_recorder = _WeeklySpendingAnalysisRecorder()
    model = _BindableFakeChatModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "get_budget_home",
                        "args": {"topic": "weekly coaching"},
                        "id": "call-1",
                        "type": "tool_call",
                    }
                ],
            ),
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "get_weekly_spending_analysis",
                        "args": {"yearWeek": "2026-03-4"},
                        "id": "call-2",
                        "type": "tool_call",
                    }
                ],
            ),
            AIMessage(content="이번 주는 소비 분석 기준으로 보면 주말 지출만 조심하면 돼요."),
        ]
    )
    graph = build_chatbot_graph(
        model_factory=lambda _role: model,
        tools=ChatbotToolBundle(
            schema=[
                _FailingToolRecorder().build(),
                weekly_recorder.build(),
            ]
        ),
        settings=_build_settings(),
        today=date(2026, 3, 27),
    )

    result = anyio.run(
        lambda: graph.ainvoke(
            {
                "messages": [
                    HumanMessage(content="이번주 소비 내역 피드백 해줘"),
                ],
                "user_id": 7,
                "session_id": "conv-1",
                "user_query": "이번주 소비 내역 피드백 해줘",
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    assert weekly_recorder.calls == [{"yearWeek": "2026-03-4", "user_id": 7}]
    assert result["tool_summaries"] == [
        {
            "tool_name": "get_budget_home",
            "status": "no_data",
        },
        {
            "tool_name": "get_weekly_spending_analysis",
            "status": "completed",
        },
    ]
    assert result["final_answer"] == "이번 주는 소비 분석 기준으로 보면 주말 지출만 조심하면 돼요."


def test_build_chatbot_graph_traces_tool_execution_with_langfuse_span() -> None:
    recorder = _ToolRecorder()
    tracer = _FakeTracer()
    model = _BindableFakeChatModel(
        responses=[
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "get_budget_home",
                        "args": {"topic": "weekly coaching"},
                        "id": "call-1",
                        "type": "tool_call",
                    }
                ],
            ),
            AIMessage(content="Keep impulse spending small this week."),
        ]
    )
    graph = build_chatbot_graph(
        model_factory=lambda _role: model,
        tools=ChatbotToolBundle(schema=[recorder.build()]),
        settings=_build_settings(),
        tracer=tracer,
        today=date(2026, 3, 27),
    )

    anyio.run(
        lambda: graph.ainvoke(
            {
                "messages": [
                    HumanMessage(content="Coach my spending habits."),
                ],
                "user_id": 7,
                "session_id": "conv-1",
                "user_query": "Coach my spending habits.",
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    tool_spans = [
        call for call in tracer.calls if call.get("name") == "chatbot.tool_call"
    ]
    assert len(tool_spans) == 1
    assert tool_spans[0]["input"] == {
        "tool_name": "get_budget_home",
        "arguments": {
            "topic": "weekly coaching",
            "user_id": 7,
        },
    }
    assert tool_spans[0]["metadata"] == {
        "tool_name": "get_budget_home",
        "tool_call_id": "call-1",
        "user_id": 7,
        "session_id": "conv-1",
    }
    assert any(
        update.get("metadata", {}).get("status") == "completed"
        for update in tool_spans[0].get("updates", [])
    )


def test_build_chatbot_graph_streams_message_chunks_without_exposing_internal_context() -> None:
    graph = _build_graph(response_text="It looks like you can pause impulse purchases.")

    async def run() -> list[tuple[tuple[str, ...], str, object]]:
        events: list[tuple[tuple[str, ...], str, object]] = []
        async for event in graph.astream(
            {
                "messages": [
                    HumanMessage(content="Coach my spending habits."),
                ],
                "user_id": 7,
                "session_id": "conv-1",
                "user_query": "Coach my spending habits.",
            },
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
            stream_mode=["updates", "messages"],
            subgraphs=True,
        ):
            events.append(event)
        return events

    events = anyio.run(run)

    assert events
    assert any(
        isinstance(event, tuple)
        and len(event) == 3
        and event[1] == "messages"
        for event in events
    )
