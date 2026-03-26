from app.core.config import Settings


def test_settings_load_chatbot_values_from_env() -> None:
    settings = Settings(
        _env_file=None,
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gateway.example.com/v1",
        CHAT_MODEL="gpt-4.1-mini",
        DB_URL="jdbc:postgresql://postgres:5432/amagetdone",
        DB_USERNAME="d101",
        DB_PASSWORD="1234",
    )

    assert settings.ai_api_key == "relay-key"
    assert settings.openai_base_url == "https://gateway.example.com/v1"
    assert settings.chat_model == "gpt-4.1-mini"
    assert settings.db_url == "jdbc:postgresql://postgres:5432/amagetdone"
    assert settings.db_username == "d101"
    assert settings.db_password == "1234"
    assert (
        settings.database_dsn
        == "postgresql+psycopg://d101:1234@postgres:5432/amagetdone"
    )
    assert settings.chatbot_stream_status_events_enabled is True
    assert settings.chatbot_default_goal_limit == 5
