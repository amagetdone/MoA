from __future__ import annotations

from fastapi.testclient import TestClient

from app.main import create_app


class _FakeChatbotService:
    def __init__(self) -> None:
        self.reply_calls: list[dict[str, object]] = []

    async def reply(
        self,
        *,
        user_id: int,
        message: str,
        conversation_id: str | None,
    ) -> dict[str, object]:
        self.reply_calls.append(
            {
                "user_id": user_id,
                "message": message,
                "conversation_id": conversation_id,
            }
        )
        return {
            "conversation_id": conversation_id or "generated-id",
            "answer": "Weekly budget is healthy.",
            "warnings": [],
            "tool_summaries": [],
        }


def test_chatbot_message_endpoint_returns_json_response() -> None:
    service = _FakeChatbotService()
    app = create_app(
        classifier=object(),
        image_generation_service=object(),
        chatbot_service=service,
    )

    with TestClient(app) as client:
        response = client.post(
            "/v1/chatbot/messages",
            headers={"X-User-Id": "7"},
            json={"message": "Is my budget okay this week?"},
        )

    assert response.status_code == 200
    assert response.json() == {
        "conversation_id": "generated-id",
        "answer": "Weekly budget is healthy.",
        "warnings": [],
        "tool_summaries": [],
    }
    assert service.reply_calls == [
        {
            "user_id": 7,
            "message": "Is my budget okay this week?",
            "conversation_id": None,
        }
    ]


def test_chatbot_message_endpoint_requires_user_id_header() -> None:
    app = create_app(
        classifier=object(),
        image_generation_service=object(),
        chatbot_service=_FakeChatbotService(),
    )

    with TestClient(app) as client:
        response = client.post(
            "/v1/chatbot/messages",
            json={"message": "Is my budget okay this week?"},
        )

    assert response.status_code == 422


def test_chatbot_message_endpoint_rejects_blank_message() -> None:
    app = create_app(
        classifier=object(),
        image_generation_service=object(),
        chatbot_service=_FakeChatbotService(),
    )

    with TestClient(app) as client:
        response = client.post(
            "/v1/chatbot/messages",
            headers={"X-User-Id": "7"},
            json={"message": "   "},
        )

    assert response.status_code == 422


def test_chatbot_message_endpoint_preserves_provided_conversation_id() -> None:
    service = _FakeChatbotService()
    app = create_app(
        classifier=object(),
        image_generation_service=object(),
        chatbot_service=service,
    )

    with TestClient(app) as client:
        response = client.post(
            "/v1/chatbot/messages",
            headers={"X-User-Id": "7"},
            json={
                "message": "Is my budget okay this week?",
                "conversation_id": "conv-1",
            },
        )

    assert response.status_code == 200
    assert response.json()["conversation_id"] == "conv-1"
    assert service.reply_calls == [
        {
            "user_id": 7,
            "message": "Is my budget okay this week?",
            "conversation_id": "conv-1",
        }
    ]


def test_chatbot_message_endpoint_rejects_blank_conversation_id() -> None:
    app = create_app(
        classifier=object(),
        image_generation_service=object(),
        chatbot_service=_FakeChatbotService(),
    )

    with TestClient(app) as client:
        response = client.post(
            "/v1/chatbot/messages",
            headers={"X-User-Id": "7"},
            json={"message": "Hello", "conversation_id": "   "},
        )

    assert response.status_code == 422


def test_chatbot_service_aclose_is_called_when_app_shuts_down() -> None:
    class _ClosableChatbotService(_FakeChatbotService):
        def __init__(self) -> None:
            super().__init__()
            self.aclose_calls = 0

        async def aclose(self) -> None:
            self.aclose_calls += 1

    service = _ClosableChatbotService()
    app = create_app(
        classifier=object(),
        image_generation_service=object(),
        chatbot_service=service,
    )

    with TestClient(app):
        pass

    assert service.aclose_calls == 1


def test_chatbot_service_close_is_called_when_app_shuts_down() -> None:
    class _ClosableChatbotService(_FakeChatbotService):
        def __init__(self) -> None:
            super().__init__()
            self.close_calls = 0

        def close(self) -> None:
            self.close_calls += 1

    service = _ClosableChatbotService()
    app = create_app(
        classifier=object(),
        image_generation_service=object(),
        chatbot_service=service,
    )

    with TestClient(app):
        pass

    assert service.close_calls == 1
