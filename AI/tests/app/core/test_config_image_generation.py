from app.core.config import Settings


def test_settings_load_image_generation_values_from_env() -> None:
    settings = Settings(
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
    )

    assert settings.ai_api_key == "relay-key"
    assert settings.openai_base_url == "https://gms.ssafy.io/gmsapi/api.openai.com/v1"
    assert settings.image_generation_model == "dall-e-3"
    assert settings.image_generation_size == "1024x1024"


def test_settings_normalize_blank_optional_langfuse_values() -> None:
    settings = Settings(
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
        LANGFUSE_PUBLIC_KEY="",
        LANGFUSE_SECRET_KEY="   ",
        LANGFUSE_BASE_URL="",
    )

    assert settings.langfuse_public_key is None
    assert settings.langfuse_secret_key is None
    assert settings.langfuse_base_url is None


def test_settings_allow_disabling_langfuse_tracing() -> None:
    settings = Settings(
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
        LANGFUSE_TRACING_ENABLED=False,
    )

    assert settings.langfuse_tracing_enabled is False
