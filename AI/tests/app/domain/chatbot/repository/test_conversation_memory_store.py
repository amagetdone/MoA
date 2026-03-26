from __future__ import annotations

from datetime import UTC, datetime, timedelta

import pytest

from app.domain.chatbot.repository.conversation_memory_store import (
    ConversationExpiredError,
    ConversationMemoryStore,
)


def test_store_creates_conversation_and_appends_messages() -> None:
    now = datetime(2026, 3, 26, 9, 0, tzinfo=UTC)
    store = ConversationMemoryStore(
        ttl=timedelta(days=3),
        now_factory=lambda: now,
    )

    store.append_user_message(user_id=7, conversation_id="conv-1", content="hello")
    store.append_assistant_message(
        user_id=7,
        conversation_id="conv-1",
        content="world",
    )

    conversation = store.get_conversation(user_id=7, conversation_id="conv-1")

    assert conversation["conversation_id"] == "conv-1"
    assert [message["role"] for message in conversation["messages"]] == [
        "user",
        "assistant",
    ]
    assert conversation["expires_at"] == "2026-03-29T09:00:00+00:00"


def test_store_refreshes_expiration_on_new_activity() -> None:
    current = {"now": datetime(2026, 3, 26, 9, 0, tzinfo=UTC)}
    store = ConversationMemoryStore(
        ttl=timedelta(days=3),
        now_factory=lambda: current["now"],
    )

    store.append_user_message(user_id=7, conversation_id="conv-1", content="first")
    current["now"] = datetime(2026, 3, 27, 12, 0, tzinfo=UTC)
    store.append_assistant_message(
        user_id=7,
        conversation_id="conv-1",
        content="second",
    )

    conversation = store.get_conversation(user_id=7, conversation_id="conv-1")

    assert conversation["expires_at"] == "2026-03-30T12:00:00+00:00"


def test_store_rejects_expired_conversation() -> None:
    current = {"now": datetime(2026, 3, 26, 9, 0, tzinfo=UTC)}
    store = ConversationMemoryStore(
        ttl=timedelta(days=3),
        now_factory=lambda: current["now"],
    )

    store.append_user_message(user_id=7, conversation_id="conv-1", content="hello")
    current["now"] = datetime(2026, 3, 29, 9, 0, 1, tzinfo=UTC)

    with pytest.raises(ConversationExpiredError):
        store.get_conversation(user_id=7, conversation_id="conv-1")


def test_store_keeps_same_conversation_id_isolated_per_user() -> None:
    now = datetime(2026, 3, 26, 9, 0, tzinfo=UTC)
    store = ConversationMemoryStore(
        ttl=timedelta(days=3),
        now_factory=lambda: now,
    )

    store.append_user_message(user_id=7, conversation_id="same", content="u7")
    store.append_user_message(user_id=8, conversation_id="same", content="u8")

    first = store.get_conversation(user_id=7, conversation_id="same")
    second = store.get_conversation(user_id=8, conversation_id="same")

    assert first["messages"][0]["content"] == "u7"
    assert second["messages"][0]["content"] == "u8"
