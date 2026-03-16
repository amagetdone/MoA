from __future__ import annotations

from typing import Any

from openai import OpenAI

from app.core.config import Settings
from app.core.logging import LangfuseTraceLogger
from app.domain.image_generation.agents.graph import build_image_generation_graph


class ImageGenerationConfigurationError(RuntimeError):
    """Raised when required image-generation configuration is missing."""


class UpstreamImageGenerationError(RuntimeError):
    """Raised when the relay returns a malformed image-generation payload."""


class ImageGenerationService:
    def __init__(
        self,
        *,
        settings: Settings,
        openai_client: Any = None,
        tracer: Any = None,
    ) -> None:
        self._settings = settings
        self._openai_client = openai_client
        self._tracer = tracer or LangfuseTraceLogger.from_settings(settings)
        self._graph = build_image_generation_graph(
            settings=settings,
            image_generator=self._generate_image,
        )

    def generate(self, prompt_goal: str) -> dict[str, str]:
        with self._tracer.start_as_current_span(
            name="image-generation",
            input={"prompt_goal": prompt_goal},
            metadata={
                "model": self._settings.image_generation_model,
                "size": self._settings.image_generation_size,
            },
        ) as trace:
            result = self._graph.invoke({"prompt_goal": prompt_goal})
            response = {
                "image_base64": result["image_base64"],
                "mime_type": result["mime_type"],
                "model": result["model"],
                "prompt": result["final_prompt"],
            }
            trace.update(
                output={
                    "mime_type": response["mime_type"],
                    "model": response["model"],
                    "prompt": response["prompt"],
                },
                metadata={
                    "final_prompt": response["prompt"],
                },
            )
            return response

    def _generate_image(self, *, prompt: str, model: str, size: str) -> dict[str, str]:
        client = self._get_openai_client()

        with self._tracer.start_as_current_span(
            name="gms-image-generation",
            input={"prompt": prompt},
            metadata={
                "model": model,
                "size": size,
            },
        ) as span:
            response = client.images.generate(
                model=model,
                prompt=prompt,
                response_format="b64_json",
                size=size,
            )
            response_schema = self._describe_response_schema(response)
            span.update(metadata=response_schema)
            image_base64 = self._extract_b64_json(
                response,
                response_schema=response_schema,
            )
            span.update(
                output={
                    "mime_type": "image/png",
                    "model": model,
                }
            )
            return {
                "image_base64": image_base64,
                "mime_type": "image/png",
                "model": model,
            }

    def _get_openai_client(self) -> Any:
        if self._openai_client is not None:
            return self._openai_client

        if not self._settings.ai_api_key or not self._settings.openai_base_url:
            raise ImageGenerationConfigurationError(
                "AI_API_KEY and OPENAI_BASE_URL must be configured for image generation"
            )

        self._openai_client = OpenAI(
            api_key=self._settings.ai_api_key,
            base_url=self._settings.openai_base_url,
        )
        return self._openai_client

    def _extract_b64_json(
        self,
        response: Any,
        *,
        response_schema: dict[str, Any] | None = None,
    ) -> str:
        data = self._get_response_value(response, "data")
        if not data:
            raise UpstreamImageGenerationError(
                "Upstream image generation response did not include data"
            )

        first_item = data[0]
        image_base64 = self._get_response_value(first_item, "b64_json")
        if not image_base64:
            available_keys = []
            if response_schema is not None:
                available_keys = response_schema.get("first_data_item_keys", [])
            details = ""
            if available_keys:
                details = f"; available keys: {available_keys}"
            raise UpstreamImageGenerationError(
                f"Upstream image generation response did not include b64_json{details}"
            )
        return image_base64

    @staticmethod
    def _get_response_value(payload: Any, key: str) -> Any:
        if isinstance(payload, dict):
            return payload.get(key)
        return getattr(payload, key, None)

    def _describe_response_schema(self, response: Any) -> dict[str, Any]:
        data = self._get_response_value(response, "data")
        first_item = data[0] if data else None
        return {
            "top_level_keys": self._list_payload_keys(response),
            "first_data_item_keys": self._list_payload_keys(first_item),
        }

    @staticmethod
    def _list_payload_keys(payload: Any) -> list[str]:
        if payload is None:
            return []
        if isinstance(payload, dict):
            return sorted(str(key) for key in payload.keys())
        if hasattr(payload, "model_dump"):
            dumped = payload.model_dump()
            if isinstance(dumped, dict):
                return sorted(str(key) for key in dumped.keys())
        if hasattr(payload, "__dict__"):
            return sorted(
                str(key)
                for key in payload.__dict__.keys()
                if not str(key).startswith("_")
            )
        return []
