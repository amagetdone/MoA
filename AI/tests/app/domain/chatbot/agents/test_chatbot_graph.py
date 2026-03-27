from __future__ import annotations

from datetime import date
from typing import Any, Sequence

import anyio
from langchain_core.language_models.fake_chat_models import FakeMessagesListChatModel
from langchain_core.messages import AIMessage, HumanMessage
from langchain_core.tools import BaseTool
from langchain_core.tools import tool
import pytest

from app.core.database import build_thread_id
from app.domain.chatbot.agents.graph import (
    ChatbotToolBundle,
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


def test_build_chatbot_graph_uses_explicit_today_in_supervisor_prompt() -> None:
    models = {
        "supervisor": _RecordingFakeChatModel(
            responses=[AIMessage(content="final answer")],
            name="supervisor",
        ),
        "budget": _RecordingFakeChatModel(
            responses=[AIMessage(content="budget answer")],
            name="budget_specialist",
        ),
        "goal": _RecordingFakeChatModel(
            responses=[AIMessage(content="goal answer")],
            name="goal_specialist",
        ),
        "spending": _RecordingFakeChatModel(
            responses=[AIMessage(content="spending answer")],
            name="spending_specialist",
        ),
        "anomaly": _RecordingFakeChatModel(
            responses=[AIMessage(content="anomaly answer")],
            name="anomaly_specialist",
        ),
    }

    graph = build_chatbot_graph(
        model_factory=models.__getitem__,
        tools=_build_tool_bundle(),
        today=date(2026, 3, 27),
    )

    anyio.run(
        lambda: graph.ainvoke(
            {"messages": [HumanMessage(content="이번 주 소비 보여줘")]},
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
