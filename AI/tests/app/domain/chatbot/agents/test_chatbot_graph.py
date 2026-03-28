from __future__ import annotations

from datetime import date
from typing import Any, Sequence

import anyio
from langchain_core.language_models.fake_chat_models import FakeMessagesListChatModel
from langchain_core.messages import AIMessage, HumanMessage
from langchain_core.tools import BaseTool, StructuredTool
from pydantic import BaseModel, Field

from app.core.config import Settings
from app.domain.chatbot.agents.graph import ChatbotToolBundle, build_chatbot_graph
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
