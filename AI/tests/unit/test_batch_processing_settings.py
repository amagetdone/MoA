from app.core.config import Settings


def test_batch_settings_separate_request_limit_from_inference_chunk_size() -> None:
    settings = Settings(_env_file=None)

    assert settings.max_batch_size == 1000
    assert settings.inference_batch_size == 128
