# MOA Chatbot Conversation Memory Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a 3-day sliding in-memory conversation history store, block expired conversations, and expose a FastAPI endpoint to fetch chat history by `conversation_id`.

**Architecture:** Keep LangGraph `InMemorySaver` for agent context and add a separate in-memory transcript store for frontend-visible history. The chatbot service will record only user messages and final assistant replies, refresh TTL on each turn, and raise domain errors for expired or missing conversations. The controller will translate those domain errors into `404` and `410` responses and expose a new history retrieval endpoint.

**Tech Stack:** FastAPI, pytest, LangGraph, LangChain, Python dataclasses, in-memory repository pattern

---

### Task 1: Add an in-memory conversation store with sliding TTL

**Files:**
- Create: `app/domain/chatbot/repository/conversation_memory_store.py`
- Modify: `app/domain/chatbot/repository/__init__.py`
- Test: `tests/app/domain/chatbot/repository/test_conversation_memory_store.py`

**Step 1: Write the failing test**

Create `tests/app/domain/chatbot/repository/test_conversation_memory_store.py` with tests like:

```python
from datetime import UTC, datetime, timedelta

import pytest

from app.domain.chatbot.repository.conversation_memory_store import (
    ConversationExpiredError,
    ConversationMemoryStore,
    ConversationNotFoundError,
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
```

**Step 2: Run test to verify it fails**

Run: `python -m uv run pytest tests/app/domain/chatbot/repository/test_conversation_memory_store.py -v`

Expected: FAIL because the in-memory store does not exist yet.

**Step 3: Write minimal implementation**

Create `app/domain/chatbot/repository/conversation_memory_store.py` with:

- `ConversationNotFoundError`
- `ConversationExpiredError`
- `ConversationMemoryStore`
- injectable `ttl` and `now_factory`
- methods:
  - `append_user_message(...)`
  - `append_assistant_message(...)`
  - `get_conversation(...)`
  - `ensure_active_conversation(...)`
  - internal pruning helpers

Implementation rules:

- key entries by `(user_id, conversation_id)`
- store `created_at`, `last_activity_at`, `expires_at`
- update `last_activity_at` and `expires_at` on every append
- remove expired entries when they are accessed
- return ISO 8601 strings from public getters

Update `app/domain/chatbot/repository/__init__.py` to export the new store and errors.

**Step 4: Run test to verify it passes**

Run: `python -m uv run pytest tests/app/domain/chatbot/repository/test_conversation_memory_store.py -v`

Expected: PASS

**Step 5: Commit**

```bash
git add app/domain/chatbot/repository/conversation_memory_store.py app/domain/chatbot/repository/__init__.py tests/app/domain/chatbot/repository/test_conversation_memory_store.py
git commit -m "feat: add chatbot conversation memory store"
```

### Task 2: Record transcript history in the chatbot service and block expired sessions

**Files:**
- Modify: `app/domain/chatbot/service/chatbot_service.py`
- Modify: `app/domain/chatbot/service/__init__.py`
- Test: `tests/app/domain/chatbot/service/test_chatbot_service.py`

**Step 1: Write the failing test**

Extend `tests/app/domain/chatbot/service/test_chatbot_service.py` with tests like:

```python
from datetime import UTC, datetime, timedelta

import anyio
import pytest

from app.domain.chatbot.repository.conversation_memory_store import (
    ConversationExpiredError,
    ConversationMemoryStore,
)


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

    async def run() -> dict[str, object]:
        return await service.reply(
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
```

Add one more test for:

- `get_conversation_history(...)` raising `ConversationNotFoundError` for unknown ids

**Step 2: Run test to verify it fails**

Run: `python -m uv run pytest tests/app/domain/chatbot/service/test_chatbot_service.py -v`

Expected: FAIL because the service does not yet record transcript history or expose a history getter.

**Step 3: Write minimal implementation**

Update `app/domain/chatbot/service/chatbot_service.py` to:

- accept `conversation_store` in `ChatbotService.__init__`
- create a default `ConversationMemoryStore(timedelta(days=3))` when not supplied
- add `get_conversation_history(user_id, conversation_id)`
- append the user message before graph execution
- append the assistant message after final answer generation
- reject expired `conversation_id` values before graph execution

Important implementation detail for streaming:

- change `stream_reply(...)` from an async generator into an async function that returns an async iterator
- do conversation validation and store updates before returning the iterator
- accumulate visible supervisor tokens and append one assistant message after the stream completes

This structure lets the controller return `410` before the HTTP stream begins.

Update `app/domain/chatbot/service/__init__.py` exports if needed.

**Step 4: Run test to verify it passes**

Run: `python -m uv run pytest tests/app/domain/chatbot/service/test_chatbot_service.py -v`

Expected: PASS

**Step 5: Commit**

```bash
git add app/domain/chatbot/service/chatbot_service.py app/domain/chatbot/service/__init__.py tests/app/domain/chatbot/service/test_chatbot_service.py
git commit -m "feat: record chatbot conversation history in memory"
```

### Task 3: Expose the conversation history endpoint and map domain errors to HTTP responses

**Files:**
- Modify: `app/domain/chatbot/api/dto/detail_dto.py`
- Modify: `app/domain/chatbot/api/controller/chatbot_controller.py`
- Test: `tests/app/test_chatbot_api.py`

**Step 1: Write the failing test**

Extend `tests/app/test_chatbot_api.py` with tests like:

```python
def test_chatbot_conversation_history_endpoint_returns_conversation() -> None:
    service = _FakeChatbotService()
    service.conversation_result = {
        "conversation_id": "conv-1",
        "created_at": "2026-03-26T09:00:00+00:00",
        "last_activity_at": "2026-03-26T09:01:00+00:00",
        "expires_at": "2026-03-29T09:01:00+00:00",
        "messages": [
            {
                "role": "user",
                "content": "hello",
                "created_at": "2026-03-26T09:00:00+00:00",
            }
        ],
    }
    app = create_app(
        classifier=object(),
        image_generation_service=object(),
        chatbot_service=service,
    )

    with TestClient(app) as client:
        response = client.get(
            "/v1/chatbot/conversations/conv-1",
            headers={"X-User-Id": "7"},
        )

    assert response.status_code == 200
    assert response.json()["conversation_id"] == "conv-1"


def test_chatbot_conversation_history_endpoint_returns_404() -> None:
    service = _FakeChatbotService()
    service.history_error = "not_found"
    app = create_app(
        classifier=object(),
        image_generation_service=object(),
        chatbot_service=service,
    )

    with TestClient(app) as client:
        response = client.get(
            "/v1/chatbot/conversations/missing",
            headers={"X-User-Id": "7"},
        )

    assert response.status_code == 404


def test_chatbot_stream_endpoint_returns_410_for_expired_conversation() -> None:
    service = _FakeStreamingChatbotService()
    service.stream_error = "expired"
    app = create_app(
        classifier=object(),
        image_generation_service=object(),
        chatbot_service=service,
    )

    with TestClient(app) as client:
        response = client.post(
            "/v1/chatbot/messages/stream",
            headers={"X-User-Id": "7"},
            json={"message": "hello", "conversation_id": "conv-1"},
        )

    assert response.status_code == 410
```

Update the fake services to:

- support `get_conversation_history(...)`
- let `stream_reply(...)` return an async iterator from an awaited async function
- simulate not-found and expired domain errors

**Step 2: Run test to verify it fails**

Run: `python -m uv run pytest tests/app/test_chatbot_api.py -v`

Expected: FAIL because the new endpoint and error mapping do not exist yet.

**Step 3: Write minimal implementation**

Update `app/domain/chatbot/api/dto/detail_dto.py` to add:

- `ChatbotConversationMessage`
- `ChatbotConversationResponse`

Update `app/domain/chatbot/api/controller/chatbot_controller.py` to:

- add `GET /v1/chatbot/conversations/{conversation_id}`
- call `service.get_conversation_history(...)`
- catch domain exceptions and raise:
  - `HTTPException(status_code=404, ...)`
  - `HTTPException(status_code=410, ...)`
- await `service.stream_reply(...)` before constructing `StreamingResponse`

Keep `X-User-Id` validation unchanged.

**Step 4: Run test to verify it passes**

Run: `python -m uv run pytest tests/app/test_chatbot_api.py -v`

Expected: PASS

**Step 5: Commit**

```bash
git add app/domain/chatbot/api/dto/detail_dto.py app/domain/chatbot/api/controller/chatbot_controller.py tests/app/test_chatbot_api.py
git commit -m "feat: add chatbot conversation history endpoint"
```

### Task 4: Run focused verification and full app verification

**Files:**
- No new files

**Step 1: Run focused chatbot tests**

Run:

```bash
python -m uv run pytest tests/app/domain/chatbot/repository/test_conversation_memory_store.py -v
python -m uv run pytest tests/app/domain/chatbot/service/test_chatbot_service.py -v
python -m uv run pytest tests/app/test_chatbot_api.py -v
```

Expected: PASS

**Step 2: Run full app test suite**

Run:

```bash
python -m uv run pytest tests/app -q
```

Expected: PASS with no new failures introduced by conversation memory changes.

**Step 3: Sanity-check OpenAPI**

Run:

```bash
python -c "from app.main import create_app; app=create_app(classifier=object(), image_generation_service=object(), chatbot_service=object()); print(sorted(path for path in app.openapi()['paths'] if path.startswith('/v1/chatbot')))"
```

Expected output includes:

```python
[
    '/v1/chatbot/conversations/{conversation_id}',
    '/v1/chatbot/messages',
    '/v1/chatbot/messages/stream',
]
```

**Step 4: Commit**

```bash
git add app/domain/chatbot app/main.py tests/app
git commit -m "test: verify chatbot conversation memory flow"
```
