from __future__ import annotations

from typing import Any

import httpx

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
        http_client: Any = None,
        tracer: Any = None,
    ) -> None:
        self._settings = settings
        self._http_client = http_client
        self._tracer = tracer or LangfuseTraceLogger.from_settings(settings)
        self._graph = build_image_generation_graph(
            settings=settings,
            image_generator=self._generate_image,
        )

    def generate(
        self,
        *,
        user_prompt: str,
        image_base64: str,
        mime_type: str,
    ) -> dict[str, str]:
        with self._tracer.start_as_current_span(
            name="image-generation",
            input={
                "user_prompt": user_prompt,
                "mime_type": mime_type,
            },
            metadata={
                "model": self._settings.image_generation_model,
            },
        ) as trace:
            result = self._graph.invoke(
                {
                    "user_prompt": user_prompt,
                    "input_image_base64": image_base64,
                    "input_mime_type": mime_type,
                }
            )
            response = {
                "image_base64": result["generated_image_base64"],
                "mime_type": result["output_mime_type"],
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

    def _generate_image(
        self,
        *,
        prompt: str,
        model: str,
        image_base64: str,
        mime_type: str,
    ) -> dict[str, str]:
        client = self._get_http_client()
        request_payload = self._build_generate_content_request(
            prompt=prompt,
            image_base64=image_base64,
            mime_type=mime_type,
        )

        with self._tracer.start_as_current_span(
            name="gms-gemini-generate-content",
            input={
                "prompt": prompt,
                "mime_type": mime_type,
            },
            metadata={
                "model": model,
            },
        ) as span:
            try:
                response = client.post(
                    f"/models/{model}:generateContent",
                    headers={
                        "Content-Type": "application/json",
                        "x-goog-api-key": self._settings.ai_api_key,
                    },
                    json=request_payload,
                    timeout=60.0,
                )
                response.raise_for_status()
            except httpx.HTTPError as exc:
                raise UpstreamImageGenerationError(
                    f"Upstream image generation request failed: {exc}"
                ) from exc

            response_payload = self._parse_response_payload(response)
            response_schema = self._describe_response_schema(response_payload)
            span.update(metadata=response_schema)

            generated_image = self._extract_generated_inline_data(
                response_payload,
                response_schema=response_schema,
            )
            span.update(
                output={
                    "mime_type": generated_image["mime_type"],
                    "model": model,
                },
                metadata={
                    "output_mime_type": generated_image["mime_type"],
                },
            )
            return {
                "generated_image_base64": generated_image["data"],
                "mime_type": generated_image["mime_type"],
                "model": model,
            }

    def _get_http_client(self) -> Any:
        if self._http_client is not None:
            return self._http_client

        if not self._settings.ai_api_key or not self._settings.openai_base_url:
            raise ImageGenerationConfigurationError(
                "AI_API_KEY and OPENAI_BASE_URL must be configured for image generation"
            )

        self._http_client = httpx.Client(
            base_url=self._settings.openai_base_url,
            follow_redirects=True,
        )
        return self._http_client

    @staticmethod
    def _build_generate_content_request(
        *,
        prompt: str,
        image_base64: str,
        mime_type: str,
    ) -> dict[str, Any]:
        return {
            "contents": [
                {
                    "parts": [
                        {"text": prompt},
                        {
                            "inline_data": {
                                "mime_type": mime_type,
                                "data": image_base64,
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
        }

    def _parse_response_payload(self, response: Any) -> dict[str, Any]:
        if isinstance(response, dict):
            return response
        if hasattr(response, "json"):
            payload = response.json()
            if isinstance(payload, dict):
                return payload
        raise UpstreamImageGenerationError(
            "Upstream image generation response could not be parsed as JSON"
        )

    def _extract_generated_inline_data(
        self,
        response_payload: dict[str, Any],
        *,
        response_schema: dict[str, Any] | None = None,
    ) -> dict[str, str]:
        candidates = self._get_response_value(response_payload, "candidates") or []
        if not candidates:
            raise UpstreamImageGenerationError(
                "Upstream image generation response did not include candidates"
            )

        for candidate in candidates:
            content = self._get_response_value(candidate, "content") or {}
            parts = self._get_response_value(content, "parts") or []
            for part in parts:
                inline_data = self._get_first_response_value(
                    part,
                    "inline_data",
                    "inlineData",
                )
                if not inline_data:
                    continue

                generated_image_base64 = self._get_response_value(inline_data, "data")
                generated_mime_type = self._get_first_response_value(
                    inline_data,
                    "mime_type",
                    "mimeType",
                )
                if generated_image_base64 and generated_mime_type:
                    return {
                        "data": generated_image_base64,
                        "mime_type": generated_mime_type,
                    }

        available_part_types: list[str] = []
        if response_schema is not None:
            available_part_types = response_schema.get("first_candidate_part_types", [])

        details = ""
        if available_part_types:
            details = f"; available part types: {available_part_types}"

        raise UpstreamImageGenerationError(
            "Upstream image generation response did not include generated inline_data"
            f"{details}"
        )

    @staticmethod
    def _get_response_value(payload: Any, key: str) -> Any:
        if isinstance(payload, dict):
            return payload.get(key)
        return getattr(payload, key, None)

    def _get_first_response_value(self, payload: Any, *keys: str) -> Any:
        for key in keys:
            value = self._get_response_value(payload, key)
            if value is not None:
                return value
        return None

    def _describe_response_schema(
        self,
        response_payload: dict[str, Any],
    ) -> dict[str, Any]:
        candidates = self._get_response_value(response_payload, "candidates") or []
        first_candidate = candidates[0] if candidates else None
        first_content = self._get_response_value(first_candidate, "content") or {}
        first_parts = self._get_response_value(first_content, "parts") or []

        return {
            "top_level_keys": self._list_payload_keys(response_payload),
            "candidate_count": len(candidates),
            "first_candidate_part_types": self._list_part_types(first_parts),
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

    def _list_part_types(self, parts: list[Any]) -> list[str]:
        part_types: list[str] = []
        for part in parts:
            part_keys = self._list_payload_keys(part)
            if "inline_data" in part_keys:
                part_types.append("inline_data")
                continue
            if "text" in part_keys:
                part_types.append("text")
                continue
            if part_keys:
                part_types.append(",".join(part_keys))
                continue
            part_types.append("unknown")
        return part_types
