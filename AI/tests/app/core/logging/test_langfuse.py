from app.core.config import Settings
from app.core.logging.langfuse import LangfuseTraceLogger, sanitize_langfuse_metadata


def test_sanitize_langfuse_metadata_removes_sensitive_fields() -> None:
    metadata = sanitize_langfuse_metadata(
        {
            "prompt_goal": "space cat",
            "image_base64": "secret-image",
            "authorization": "Bearer secret",
            "api_key": "secret-key",
            "nested_api_token": "secret-token",
            "model": "dall-e-3",
        }
    )

    assert metadata == {
        "prompt_goal": "space cat",
        "model": "dall-e-3",
    }


def test_langfuse_logger_is_noop_when_unconfigured() -> None:
    settings = Settings(
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
    )

    logger = LangfuseTraceLogger.from_settings(settings)

    assert logger.enabled is False


def test_langfuse_logger_disabled_mode_allows_safe_calls() -> None:
    settings = Settings(
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
        LANGFUSE_TRACING_ENABLED=False,
    )

    logger = LangfuseTraceLogger.from_settings(settings)

    with logger.start_as_current_span(
        name="image-generation",
        input={"prompt_goal": "space cat"},
        metadata={"image_base64": "secret-image"},
    ) as trace:
        trace.update(output={"image_base64": "secret-image", "model": "dall-e-3"})

        with trace.start_as_current_span(
            name="relay-call",
            metadata={"authorization": "Bearer secret", "size": "1024x1024"},
        ) as span:
            span.update(metadata={"api_key": "secret-key", "mime_type": "image/png"})
