from __future__ import annotations

from typing import Any, Sequence

import anyio
from langchain_core.language_models.fake_chat_models import FakeMessagesListChatModel
from langchain_core.messages import AIMessage, HumanMessage
from langchain_core.tools import BaseTool
from langchain_core.tools import tool
import pytest

from app.core.config import Settings
from app.domain.chatbot.agents.graph import (
    ChatbotToolBundle,
    build_chat_model,
    build_chatbot_graph,
    build_thread_id,
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


class _RecordingFakeChatModel(_BindableFakeChatModel):
    def __init__(self, *, responses: list[AIMessage], name: str) -> None:
        super().__init__(responses=responses)
        self.name = name
        self.seen_messages: list[list[Any]] = []

    def _generate(self, messages, stop=None, run_manager=None, **kwargs: Any):
        self.seen_messages.append(messages)
        return super()._generate(messages, stop=stop, run_manager=run_manager, **kwargs)


@tool
def _dummy_tool() -> str:
    """Return a dummy result."""
    return "dummy"


class _FakeChatOpenAI:
    def __init__(self, **kwargs: Any) -> None:
        self.kwargs = kwargs


def _build_tool_bundle() -> ChatbotToolBundle:
    return ChatbotToolBundle(
        budget=[_dummy_tool],
        goals=[_dummy_tool],
        spending=[_dummy_tool],
        anomalies=[_dummy_tool],
    )


def test_chatbot_graph_state_defines_required_fields() -> None:
    annotations = ChatbotGraphState.__annotations__

    assert "messages" in annotations
    assert "warnings" in annotations
    assert "tool_summaries" in annotations
    assert "final_answer" in annotations


def test_build_chatbot_graph_compiles_with_checkpointer() -> None:
    models = {
        "supervisor": _BindableFakeChatModel(
            responses=[AIMessage(content="final answer")],
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
    }

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=_build_tool_bundle(),
    )

    assert graph is not None
    assert getattr(graph, "checkpointer", None) is not None


def test_thread_id_is_user_scoped() -> None:
    assert build_thread_id(user_id=7, conversation_id="same") != build_thread_id(
        user_id=8,
        conversation_id="same",
    )


@pytest.mark.parametrize(
    ("question", "route_name"),
    [
        ("이번 달 지출이 너무 많아. 최근 소비를 보여줘.", "spending"),
        ("이번 주 예산이 얼마나 남았는지 알려줘.", "budget"),
    ],
)
def test_build_chatbot_graph_routes_to_expected_specialist(
    question: str,
    route_name: str,
) -> None:
    supervisor_responses = {
        "spending": [
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "transfer_to_spending_specialist",
                        "args": {},
                        "id": "route-spending",
                    }
                ],
            ),
            AIMessage(content="spending final"),
        ],
        "budget": [
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "transfer_to_budget_specialist",
                        "args": {},
                        "id": "route-budget",
                    }
                ],
            ),
            AIMessage(content="budget final"),
        ],
    }

    models = {
        "supervisor": _RecordingFakeChatModel(
            responses=supervisor_responses[route_name],
            name="supervisor",
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
    }

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=_build_tool_bundle(),
    )

    result = anyio.run(
        lambda: graph.ainvoke(
            {"messages": [HumanMessage(content=question)]},
            config={"configurable": {"thread_id": "moa:7:conv-1"}},
        )
    )

    if route_name == "spending":
        assert len(models["spending"].seen_messages) == 1
        assert len(models["budget"].seen_messages) == 0
        assert any(
            getattr(message, "name", None) == "spending_specialist"
            for message in result["messages"]
        )
    else:
        assert len(models["budget"].seen_messages) == 1
        assert len(models["spending"].seen_messages) == 0
        assert any(
            getattr(message, "name", None) == "budget_specialist"
            for message in result["messages"]
        )


def test_build_chat_model_attaches_langfuse_callbacks_when_configured(monkeypatch) -> None:
    captured: dict[str, Any] = {}

    def fake_chat_openai(**kwargs: Any) -> _FakeChatOpenAI:
        captured.update(kwargs)
        return _FakeChatOpenAI(**kwargs)

    monkeypatch.setattr(
        "app.domain.chatbot.agents.graph.ChatOpenAI",
        fake_chat_openai,
    )

    settings = Settings(
        _env_file=None,
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://example.com/v1",
        CHAT_MODEL="gpt-4.1-mini",
        LANGFUSE_PUBLIC_KEY="public-key",
        LANGFUSE_SECRET_KEY="secret-key",
        LANGFUSE_BASE_URL="https://langfuse.example.com",
        LANGFUSE_TRACING_ENABLED=True,
    )

    model = build_chat_model(settings=settings)

    assert isinstance(model, _FakeChatOpenAI)
    assert captured["model"] == "gpt-4.1-mini"
    assert captured["api_key"] == "relay-key"
    assert captured["base_url"] == "https://example.com/v1"
    assert captured["streaming"] is True
    assert "callbacks" in captured
    assert len(captured["callbacks"]) == 1


def test_build_chat_model_initializes_langfuse_before_callback_handler(
    monkeypatch,
) -> None:
    captured: dict[str, Any] = {}
    call_order: list[str] = []

    def fake_from_settings(settings: Settings):
        call_order.append("from_settings")

        class _FakeTracer:
            enabled = True

        return _FakeTracer()

    class _FakeCallbackHandler:
        def __init__(self, *, public_key: str, update_trace: bool = False) -> None:
            assert call_order == ["from_settings"]
            call_order.append("callback")
            self.public_key = public_key
            self.update_trace = update_trace

    def fake_chat_openai(**kwargs: Any) -> _FakeChatOpenAI:
        captured.update(kwargs)
        return _FakeChatOpenAI(**kwargs)

    monkeypatch.setattr(
        "app.domain.chatbot.agents.graph.LangfuseTraceLogger.from_settings",
        fake_from_settings,
    )
    monkeypatch.setattr(
        "app.domain.chatbot.agents.graph.LangfuseCallbackHandler",
        _FakeCallbackHandler,
    )
    monkeypatch.setattr(
        "app.domain.chatbot.agents.graph.ChatOpenAI",
        fake_chat_openai,
    )

    settings = Settings(
        _env_file=None,
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://example.com/v1",
        CHAT_MODEL="gpt-4.1-mini",
        LANGFUSE_PUBLIC_KEY="public-key",
        LANGFUSE_SECRET_KEY="secret-key",
        LANGFUSE_BASE_URL="https://langfuse.example.com",
        LANGFUSE_TRACING_ENABLED=True,
    )

    model = build_chat_model(settings=settings)

    assert isinstance(model, _FakeChatOpenAI)
    assert call_order == ["from_settings", "callback"]
    assert captured["callbacks"][0].public_key == "public-key"


def test_build_chat_model_omits_langfuse_callbacks_when_unconfigured(
    monkeypatch,
) -> None:
    captured: dict[str, Any] = {}

    def fake_chat_openai(**kwargs: Any) -> _FakeChatOpenAI:
        captured.update(kwargs)
        return _FakeChatOpenAI(**kwargs)

    monkeypatch.setattr(
        "app.domain.chatbot.agents.graph.ChatOpenAI",
        fake_chat_openai,
    )

    settings = Settings(
        _env_file=None,
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://example.com/v1",
        CHAT_MODEL="gpt-4.1-mini",
        LANGFUSE_PUBLIC_KEY="",
        LANGFUSE_SECRET_KEY="",
        LANGFUSE_BASE_URL="",
        LANGFUSE_TRACING_ENABLED=True,
    )

    model = build_chat_model(settings=settings)

    assert isinstance(model, _FakeChatOpenAI)
    assert captured["model"] == "gpt-4.1-mini"
    assert "callbacks" not in captured
