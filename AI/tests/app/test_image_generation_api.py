from __future__ import annotations

from fastapi.testclient import TestClient

from app.domain.image_generation.service.image_generation_service import (
    UpstreamImageGenerationError,
)
from app.main import create_app


class _FakeImageGenerationService:
    def __init__(self, result: dict[str, str] | None = None, error: Exception | None = None):
        self.result = result or {
            "image_base64": "encoded-image",
            "mime_type": "image/png",
            "model": "dall-e-3",
            "prompt": "rendered prompt",
        }
        self.error = error
        self.calls: list[str] = []

    def generate(self, prompt_goal: str) -> dict[str, str]:
        self.calls.append(prompt_goal)
        if self.error is not None:
            raise self.error
        return self.result


def test_image_generation_endpoint_returns_base64_payload() -> None:
    service = _FakeImageGenerationService()
    app = create_app(classifier=object(), image_generation_service=service)

    with TestClient(app) as client:
        response = client.post(
            "/v1/image-generation",
            json={"prompt_goal": "우주 배경의 초현실적인 고양이 일러스트"},
        )

    assert response.status_code == 200
    assert response.json() == {
        "image_base64": "encoded-image",
        "mime_type": "image/png",
        "model": "dall-e-3",
        "prompt": "rendered prompt",
    }
    assert service.calls == ["우주 배경의 초현실적인 고양이 일러스트"]


def test_image_generation_endpoint_rejects_blank_prompt_goal() -> None:
    app = create_app(
        classifier=object(),
        image_generation_service=_FakeImageGenerationService(),
    )

    with TestClient(app) as client:
        response = client.post(
            "/v1/image-generation",
            json={"prompt_goal": "   "},
        )

    assert response.status_code == 422


def test_image_generation_endpoint_maps_upstream_errors_to_bad_gateway() -> None:
    app = create_app(
        classifier=object(),
        image_generation_service=_FakeImageGenerationService(
            error=UpstreamImageGenerationError("missing b64_json")
        ),
    )

    with TestClient(app) as client:
        response = client.post(
            "/v1/image-generation",
            json={"prompt_goal": "우주 배경의 초현실적인 고양이 일러스트"},
        )

    assert response.status_code == 502
    assert response.json() == {"detail": "missing b64_json"}
