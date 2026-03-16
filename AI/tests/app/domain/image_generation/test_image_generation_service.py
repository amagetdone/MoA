from __future__ import annotations

import pytest

from app.core.config import Settings
from app.domain.image_generation.service.image_generation_service import (
    ImageGenerationService,
    UpstreamImageGenerationError,
)


PROMPT_GOAL = "surreal cat in space illustration"


class _FakeImagesClient:
    def __init__(self, response: dict) -> None:
        self.response = response
        self.calls: list[dict[str, str]] = []

    def generate(self, **kwargs) -> dict:
        self.calls.append(kwargs)
        return self.response


class _FakeOpenAIClient:
    def __init__(self, response: dict) -> None:
        self.images = _FakeImagesClient(response)


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


def test_service_uses_configured_payload_and_extracts_b64_json() -> None:
    settings = Settings(
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
    )
    openai_client = _FakeOpenAIClient({"data": [{"b64_json": "encoded-image"}]})
    tracer = _CapturingTracer()

    service = ImageGenerationService(
        settings=settings,
        openai_client=openai_client,
        tracer=tracer,
    )

    result = service.generate(PROMPT_GOAL)

    assert result["image_base64"] == "encoded-image"
    assert result["mime_type"] == "image/png"
    assert result["model"] == "dall-e-3"
    assert PROMPT_GOAL in result["prompt"]
    assert openai_client.images.calls == [
        {
            "model": "dall-e-3",
            "prompt": result["prompt"],
            "response_format": "b64_json",
            "size": "1024x1024",
        }
    ]
    assert all("image_base64" not in str(event) for event in tracer.events)


def test_service_raises_when_upstream_b64_json_is_missing() -> None:
    settings = Settings(
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
    )
    service = ImageGenerationService(
        settings=settings,
        openai_client=_FakeOpenAIClient({"data": [{}]}),
        tracer=_CapturingTracer(),
    )

    with pytest.raises(UpstreamImageGenerationError):
        service.generate(PROMPT_GOAL)


def test_service_reports_available_upstream_keys_for_schema_debugging() -> None:
    settings = Settings(
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
    )
    tracer = _CapturingTracer()
    service = ImageGenerationService(
        settings=settings,
        openai_client=_FakeOpenAIClient(
            {"created": 123, "data": [{"url": "https://example.com/image.png"}]}
        ),
        tracer=tracer,
    )

    with pytest.raises(UpstreamImageGenerationError) as exc_info:
        service.generate(PROMPT_GOAL)

    assert "available keys: ['url']" in str(exc_info.value)
    assert any(
        event.get("metadata", {}).get("first_data_item_keys") == ["url"]
        for event in tracer.events
        if event.get("event") == "update"
    )
