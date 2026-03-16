from __future__ import annotations

import pytest

from app.core.config import Settings
from app.domain.image_generation.service.image_generation_service import (
    ImageGenerationService,
    UpstreamImageGenerationError,
)


USER_PROMPT = "Turn the reference image into a polished cyberpunk banana poster."
INPUT_IMAGE_BASE64 = "reference-image"
INPUT_MIME_TYPE = "image/jpeg"


class _FakeResponse:
    def __init__(self, payload: dict) -> None:
        self.payload = payload

    def raise_for_status(self) -> None:
        return None

    def json(self) -> dict:
        return self.payload


class _FakeHttpClient:
    def __init__(self, payload: dict) -> None:
        self.payload = payload
        self.calls: list[dict[str, object]] = []

    def post(
        self,
        path: str,
        *,
        headers: dict[str, str] | None = None,
        json: dict[str, object] | None = None,
        timeout: float | None = None,
    ) -> _FakeResponse:
        self.calls.append(
            {
                "path": path,
                "headers": headers or {},
                "json": json or {},
                "timeout": timeout,
            }
        )
        return _FakeResponse(self.payload)


class _CapturingSpan:
    def __init__(
        self,
        recorder: list[dict[str, object]],
        *,
        name: str,
        input: object = None,
        metadata: dict[str, object] | None = None,
    ) -> None:
        self.recorder = recorder
        self.recorder.append(
            {
                "event": "start",
                "name": name,
                "input": input,
                "metadata": metadata or {},
            }
        )

    def __enter__(self) -> _CapturingSpan:
        return self

    def __exit__(self, exc_type, exc, tb) -> bool:
        return False

    def update(self, **kwargs) -> None:
        self.recorder.append({"event": "update", **kwargs})

    def start_as_current_span(
        self,
        *,
        name: str,
        input: object = None,
        metadata: dict[str, object] | None = None,
    ) -> _CapturingSpan:
        return _CapturingSpan(
            self.recorder,
            name=name,
            input=input,
            metadata=metadata,
        )


class _CapturingTracer:
    def __init__(self) -> None:
        self.events: list[dict[str, object]] = []

    def start_as_current_span(
        self,
        *,
        name: str,
        input: object = None,
        output: object = None,
        metadata: dict[str, object] | None = None,
    ) -> _CapturingSpan:
        del output
        return _CapturingSpan(
            self.events,
            name=name,
            input=input,
            metadata=metadata,
        )


def test_service_uses_configured_payload_and_extracts_generated_inline_data() -> None:
    settings = Settings(
        _env_file=None,
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta",
    )
    http_client = _FakeHttpClient(
        {
            "candidates": [
                {
                    "content": {
                        "parts": [
                            {"text": "Edited successfully."},
                            {
                                "inline_data": {
                                    "mime_type": "image/png",
                                    "data": "encoded-image",
                                }
                            },
                        ]
                    }
                }
            ]
        }
    )
    tracer = _CapturingTracer()

    service = ImageGenerationService(
        settings=settings,
        http_client=http_client,
        tracer=tracer,
    )

    result = service.generate(
        user_prompt=USER_PROMPT,
        image_base64=INPUT_IMAGE_BASE64,
        mime_type=INPUT_MIME_TYPE,
    )

    assert result == {
        "image_base64": "encoded-image",
        "mime_type": "image/png",
        "model": "gemini-2.5-flash-image",
        "prompt": result["prompt"],
    }
    assert USER_PROMPT in result["prompt"]
    assert "512x512" in result["prompt"]
    assert http_client.calls == [
        {
            "path": "/models/gemini-2.5-flash-image:generateContent",
            "headers": {
                "Content-Type": "application/json",
                "x-goog-api-key": "relay-key",
            },
            "json": {
                "contents": [
                    {
                        "parts": [
                            {"text": result["prompt"]},
                            {
                                "inline_data": {
                                    "mime_type": "image/jpeg",
                                    "data": "reference-image",
                                }
                            },
                        ]
                    }
                ],
                "generationConfig": {
                    "imageConfig": {
                        "aspectRatio": "1:1",
                    }
                },
            },
            "timeout": 60.0,
        }
    ]
    assert all(INPUT_IMAGE_BASE64 not in str(event) for event in tracer.events)
    assert all("encoded-image" not in str(event) for event in tracer.events)


def test_service_accepts_camel_case_inline_data_from_upstream() -> None:
    settings = Settings(
        _env_file=None,
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta",
    )
    service = ImageGenerationService(
        settings=settings,
        http_client=_FakeHttpClient(
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {"text": "Edited successfully."},
                                {
                                    "inlineData": {
                                        "mimeType": "image/png",
                                        "data": "camel-case-image",
                                    }
                                },
                            ]
                        }
                    }
                ]
            }
        ),
        tracer=_CapturingTracer(),
    )

    result = service.generate(
        user_prompt=USER_PROMPT,
        image_base64=INPUT_IMAGE_BASE64,
        mime_type=INPUT_MIME_TYPE,
    )

    assert result["image_base64"] == "camel-case-image"
    assert result["mime_type"] == "image/png"
    assert result["model"] == "gemini-2.5-flash-image"


def test_service_raises_when_generated_inline_data_is_missing() -> None:
    settings = Settings(
        _env_file=None,
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta",
    )
    service = ImageGenerationService(
        settings=settings,
        http_client=_FakeHttpClient(
            {"candidates": [{"content": {"parts": [{"text": "No image generated."}]}}]}
        ),
        tracer=_CapturingTracer(),
    )

    with pytest.raises(UpstreamImageGenerationError):
        service.generate(
            user_prompt=USER_PROMPT,
            image_base64=INPUT_IMAGE_BASE64,
            mime_type=INPUT_MIME_TYPE,
        )


def test_service_reports_available_upstream_part_types_for_schema_debugging() -> None:
    settings = Settings(
        _env_file=None,
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta",
    )
    tracer = _CapturingTracer()
    service = ImageGenerationService(
        settings=settings,
        http_client=_FakeHttpClient(
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {"text": "Model returned only a textual description."}
                            ]
                        }
                    }
                ],
                "usageMetadata": {"promptTokenCount": 42},
            }
        ),
        tracer=tracer,
    )

    with pytest.raises(UpstreamImageGenerationError) as exc_info:
        service.generate(
            user_prompt=USER_PROMPT,
            image_base64=INPUT_IMAGE_BASE64,
            mime_type=INPUT_MIME_TYPE,
        )

    assert "available part types: ['text']" in str(exc_info.value)
    assert any(
        event.get("metadata", {}).get("first_candidate_part_types") == ["text"]
        for event in tracer.events
        if event.get("event") == "update"
    )
