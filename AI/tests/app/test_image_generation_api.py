from __future__ import annotations

from fastapi.testclient import TestClient

from app.domain.image_generation.service.image_generation_service import (
    UpstreamImageGenerationError,
)
from app.main import create_app


class _FakeImageGenerationService:
    def __init__(
        self,
        result: dict[str, str] | None = None,
        error: Exception | None = None,
    ) -> None:
        self.result = result or {
            "image_base64": "encoded-image",
            "mime_type": "image/png",
            "model": "gemini-2.5-flash-image",
            "prompt": "rendered prompt",
        }
        self.error = error
        self.calls: list[dict[str, str]] = []

    def generate(
        self,
        *,
        user_prompt: str,
        image_base64: str,
        mime_type: str,
    ) -> dict[str, str]:
        self.calls.append(
            {
                "user_prompt": user_prompt,
                "image_base64": image_base64,
                "mime_type": mime_type,
            }
        )
        if self.error is not None:
            raise self.error
        return self.result


def test_image_generation_endpoint_returns_base64_payload() -> None:
    service = _FakeImageGenerationService()
    app = create_app(classifier=object(), image_generation_service=service)

    with TestClient(app) as client:
        response = client.post(
            "/v1/image-generation",
            json={
                "user_prompt": "Turn the reference image into a polished cyberpunk banana poster.",
                "image_base64": "reference-image",
                "mime_type": "image/jpeg",
            },
        )

    assert response.status_code == 200
    assert response.json() == {
        "image_base64": "encoded-image",
        "mime_type": "image/png",
        "model": "gemini-2.5-flash-image",
        "prompt": "rendered prompt",
    }
    assert service.calls == [
        {
            "user_prompt": "Turn the reference image into a polished cyberpunk banana poster.",
            "image_base64": "reference-image",
            "mime_type": "image/jpeg",
        }
    ]


def test_image_generation_endpoint_rejects_blank_fields() -> None:
    app = create_app(
        classifier=object(),
        image_generation_service=_FakeImageGenerationService(),
    )

    with TestClient(app) as client:
        response = client.post(
            "/v1/image-generation",
            json={
                "user_prompt": "   ",
                "image_base64": "   ",
                "mime_type": "   ",
            },
        )

    assert response.status_code == 422


def test_image_generation_endpoint_maps_upstream_errors_to_bad_gateway() -> None:
    app = create_app(
        classifier=object(),
        image_generation_service=_FakeImageGenerationService(
            error=UpstreamImageGenerationError("missing generated image")
        ),
    )

    with TestClient(app) as client:
        response = client.post(
            "/v1/image-generation",
            json={
                "user_prompt": "Turn the reference image into a polished cyberpunk banana poster.",
                "image_base64": "reference-image",
                "mime_type": "image/jpeg",
            },
        )

    assert response.status_code == 502
    assert response.json() == {"detail": "missing generated image"}


def test_image_generation_openapi_describes_supported_mime_types_in_korean() -> None:
    app = create_app(
        classifier=object(),
        image_generation_service=_FakeImageGenerationService(),
    )

    with TestClient(app) as client:
        response = client.get("/openapi.json")

    assert response.status_code == 200
    request_schema = response.json()["components"]["schemas"]["ImageGenerationRequest"]
    mime_type_schema = request_schema["properties"]["mime_type"]
    operation = response.json()["paths"]["/v1/image-generation"]["post"]

    assert "입력 이미지의 MIME 타입" in mime_type_schema["description"]
    assert "image/jpeg" in mime_type_schema["description"]
    assert "image/png" in mime_type_schema["description"]
    assert "image/webp" in mime_type_schema["description"]
    assert "mime_type에는 입력 이미지의 MIME 타입을 넣습니다" in operation["description"]
    assert "image/jpeg" in operation["description"]
    assert "image/png" in operation["description"]
    assert "image/webp" in operation["description"]
