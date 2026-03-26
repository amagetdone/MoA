from __future__ import annotations

from datetime import UTC, datetime, timedelta
from uuid import UUID

import anyio
import pytest
from langchain_core.messages import AIMessage, HumanMessage

from app.core.config import Settings
from app.domain.chatbot.repository.conversation_memory_store import (
    ConversationExpiredError,
    ConversationMemoryStore,
    ConversationNotFoundError,
)
from app.domain.chatbot.service.chatbot_service import ChatbotService


class _FakeSpan:
    def __init__(self, record: dict[str, object]) -> None:
        self.record = record

    def __enter__(self) -> "_FakeSpan":
        self.record["entered"] = True
        return self

    def __exit__(self, exc_type, exc, tb) -> bool:
        self.record["exited"] = True
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


class _FakeGraph:
    def __init__(
        self,
        *,
        reply_messages: list[object] | None = None,
        reply_final_answer: str | None = None,
        stream_chunks: list[tuple[str, object]] | None = None,
    ) -> None:
        self.reply_messages = reply_messages or [
            HumanMessage(content="How is my budget?"),
            AIMessage(content="Weekly budget is healthy.", name="supervisor"),
        ]
        self.reply_final_answer = reply_final_answer
        self.stream_chunks = stream_chunks or [
            (
                "messages",
                (
                    AIMessage(content="Internal specialist note", name="spending_specialist"),
                    {"langgraph_node": "spending_specialist"},
                ),
            ),
            (
                "messages",
                (
                    AIMessage(content="Budget", name="supervisor"),
                    {"langgraph_node": "supervisor"},
                ),
            ),
            (
                "messages",
                (
                    AIMessage(content=" is healthy.", name="supervisor"),
                    {"langgraph_node": "supervisor"},
                ),
            ),
        ]
        self.reply_calls: list[dict[str, object]] = []
        self.stream_calls: list[dict[str, object]] = []

    async def ainvoke(
        self,
        input: dict[str, object],
        config: dict[str, object] | None = None,
    ):
        self.reply_calls.append({"input": input, "config": config})
        return {
            "messages": self.reply_messages,
            "warnings": ["watch budget"],
            "tool_summaries": [
                {
                    "tool_name": "get_weekly_budget_status",
                    "as_of": "2026-03-26",
                }
            ],
            "final_answer": self.reply_final_answer,
        }

    async def astream(
        self,
        input: dict[str, object],
        config: dict[str, object] | None = None,
        stream_mode: object | None = None,
    ):
        self.stream_calls.append(
            {"input": input, "config": config, "stream_mode": stream_mode}
        )
        for chunk in self.stream_chunks:
            yield chunk


class _FakeGraphFactory:
    def __init__(self) -> None:
        self.calls: list[int] = []
        self.graphs: dict[int, _FakeGraph] = {}

    def __call__(self, user_id: int) -> _FakeGraph:
        self.calls.append(user_id)
        graph = self.graphs.get(user_id)
        if graph is None:
            graph = _FakeGraph()
            self.graphs[user_id] = graph
        return graph


def _build_settings() -> Settings:
    return Settings(
        _env_file=None,
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://example.com/v1",
        CHAT_MODEL="gpt-4.1-mini",
    )


def test_reply_uses_user_scoped_thread_id_and_caches_graph_per_user() -> None:
    factory = _FakeGraphFactory()
    tracer = _FakeTracer()
    service = ChatbotService(
        settings=_build_settings(),
        graph_factory=factory,
        tracer=tracer,
    )

    async def run() -> tuple[dict[str, object], dict[str, object], dict[str, object]]:
        first = await service.reply(
            user_id=7,
            message="How is my budget?",
            conversation_id="conv-1",
        )
        second = await service.reply(
            user_id=7,
            message="How is my budget now?",
            conversation_id="conv-2",
        )
        third = await service.reply(
            user_id=8,
            message="How is my budget?",
            conversation_id="conv-3",
        )
        return first, second, third

    first, second, third = anyio.run(run)

    assert factory.calls == [7, 8]
    assert factory.graphs[7].reply_calls[0]["config"] == {
        "configurable": {"thread_id": "moa:7:conv-1"}
    }
    assert factory.graphs[7].reply_calls[1]["config"] == {
        "configurable": {"thread_id": "moa:7:conv-2"}
    }
    assert factory.graphs[8].reply_calls[0]["config"] == {
        "configurable": {"thread_id": "moa:8:conv-3"}
    }
    assert first["conversation_id"] == "conv-1"
    assert second["conversation_id"] == "conv-2"
    assert third["conversation_id"] == "conv-3"
    assert first["answer"] == "Weekly budget is healthy."
    assert first["warnings"] == ["watch budget"]
    assert first["tool_summaries"] == [
        {
            "tool_name": "get_weekly_budget_status",
            "as_of": "2026-03-26",
        }
    ]
    assert tracer.calls[0]["name"] == "chatbot.reply"


def test_reply_generates_conversation_id_when_missing() -> None:
    factory = _FakeGraphFactory()
    tracer = _FakeTracer()
    service = ChatbotService(
        settings=_build_settings(),
        graph_factory=factory,
        tracer=tracer,
    )

    async def run() -> dict[str, object]:
        return await service.reply(
            user_id=7,
            message="How is my budget?",
            conversation_id=None,
        )

    result = anyio.run(run)

    generated_conversation_id = result["conversation_id"]
    assert isinstance(generated_conversation_id, str)
    UUID(generated_conversation_id)
    assert factory.graphs[7].reply_calls[0]["config"] == {
        "configurable": {
            "thread_id": f"moa:7:{generated_conversation_id}",
        }
    }


def test_reply_ignores_non_supervisor_messages_when_final_answer_missing() -> None:
    graph = _FakeGraph(
        reply_messages=[
            HumanMessage(content="How is my budget?"),
            AIMessage(content="Weekly budget is healthy.", name="supervisor"),
            AIMessage(content="Internal specialist note", name="spending_specialist"),
        ]
    )
    tracer = _FakeTracer()
    service = ChatbotService(
        settings=_build_settings(),
        graph_factory=lambda _user_id: graph,
        tracer=tracer,
    )

    async def run() -> dict[str, object]:
        return await service.reply(
            user_id=7,
            message="How is my budget?",
            conversation_id="conv-1",
        )

    result = anyio.run(run)

    assert result["answer"] == "Weekly budget is healthy."


def test_reply_rejects_non_positive_user_id() -> None:
    service = ChatbotService(
        settings=_build_settings(),
        graph_factory=_FakeGraphFactory(),
        tracer=_FakeTracer(),
    )

    async def run() -> None:
        await service.reply(
            user_id=0,
            message="How is my budget?",
            conversation_id="conv-1",
        )

    with pytest.raises(ValueError, match="positive integer"):
        anyio.run(run)


def test_stream_reply_emits_only_supervisor_tokens_and_done() -> None:
    factory = _FakeGraphFactory()
    tracer = _FakeTracer()
    service = ChatbotService(
        settings=_build_settings(),
        graph_factory=factory,
        tracer=tracer,
    )

    async def run() -> list[dict[str, object]]:
        events: list[dict[str, object]] = []
        stream = await service.stream_reply(
            user_id=7,
            message="How is my budget?",
            conversation_id="conv-1",
        )
        async for event in stream:
            events.append(event)
        return events

    events = anyio.run(run)

    assert events[0] == {"event": "status", "data": {"message": "routing"}}
    assert events[1] == {"event": "token", "data": {"text": "Budget"}}
    assert events[2] == {"event": "token", "data": {"text": " is healthy."}}
    assert events[-1] == {
        "event": "done",
        "data": {"conversation_id": "conv-1"},
    }
    assert len(events) == 4
    assert factory.graphs[7].stream_calls[0]["config"] == {
        "configurable": {"thread_id": "moa:7:conv-1"}
    }
    assert tracer.calls[-1]["name"] == "chatbot.stream_reply"


def test_reply_records_user_and_assistant_messages() -> None:
    store = ConversationMemoryStore(
        ttl=timedelta(days=3),
        now_factory=lambda: datetime(2026, 3, 26, 9, 0, tzinfo=UTC),
    )
    service = ChatbotService(
        settings=_build_settings(),
        graph_factory=lambda _user_id: _FakeGraph(reply_final_answer="answer"),
        tracer=_FakeTracer(),
        conversation_store=store,
    )

    async def run() -> None:
        await service.reply(
            user_id=7,
            message="hello",
            conversation_id="conv-1",
        )

    anyio.run(run)
    conversation = service.get_conversation_history(user_id=7, conversation_id="conv-1")

    assert [message["role"] for message in conversation["messages"]] == [
        "user",
        "assistant",
    ]
    assert [message["content"] for message in conversation["messages"]] == [
        "hello",
        "answer",
    ]


def test_stream_reply_records_final_assistant_message_only() -> None:
    store = ConversationMemoryStore(
        ttl=timedelta(days=3),
        now_factory=lambda: datetime(2026, 3, 26, 9, 0, tzinfo=UTC),
    )
    service = ChatbotService(
        settings=_build_settings(),
        graph_factory=lambda _user_id: _FakeGraph(),
        tracer=_FakeTracer(),
        conversation_store=store,
    )

    async def run() -> None:
        stream = await service.stream_reply(
            user_id=7,
            message="hello",
            conversation_id="conv-1",
        )
        async for _ in stream:
            pass

    anyio.run(run)
    conversation = service.get_conversation_history(user_id=7, conversation_id="conv-1")

    assert [message["content"] for message in conversation["messages"]] == [
        "hello",
        "Budget is healthy.",
    ]


def test_reply_rejects_expired_conversation_id() -> None:
    current = {"now": datetime(2026, 3, 26, 9, 0, tzinfo=UTC)}
    store = ConversationMemoryStore(
        ttl=timedelta(days=3),
        now_factory=lambda: current["now"],
    )
    store.append_user_message(user_id=7, conversation_id="conv-1", content="old")
    current["now"] = datetime(2026, 3, 29, 9, 0, 1, tzinfo=UTC)

    service = ChatbotService(
        settings=_build_settings(),
        graph_factory=lambda _user_id: _FakeGraph(),
        tracer=_FakeTracer(),
        conversation_store=store,
    )

    async def run() -> None:
        await service.reply(user_id=7, message="new", conversation_id="conv-1")

    with pytest.raises(ConversationExpiredError):
        anyio.run(run)


def test_get_conversation_history_raises_for_unknown_id() -> None:
    service = ChatbotService(
        settings=_build_settings(),
        graph_factory=_FakeGraphFactory(),
        tracer=_FakeTracer(),
        conversation_store=ConversationMemoryStore(
            ttl=timedelta(days=3),
            now_factory=lambda: datetime(2026, 3, 26, 9, 0, tzinfo=UTC),
        ),
    )

    with pytest.raises(ConversationNotFoundError):
        service.get_conversation_history(user_id=7, conversation_id="missing")
