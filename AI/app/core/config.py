from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_id: str = "kakao1513/merchant-consumption-category-discriminator-v1"
    model_device: str = "cpu"
    fallback_label: str = "기타서비스"
    fallback_threshold: float = 0.70
    top_k: int = 3
    max_batch_size: int = 1000
    inference_batch_size: int = 128
    model_revision: str | None = None
    model_local_dir: str | None = None
    hf_token: str | None = None
    api_root_path: str = ""

    model_config = SettingsConfigDict(
        env_file=(".env", "app/.env"),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    @property
    def model_source(self) -> str:
        return self.model_local_dir or self.model_id

    @field_validator("model_revision", "model_local_dir", "hf_token", mode="before")
    @classmethod
    def blank_strings_to_none(cls, value: str | None) -> str | None:
        if value is None:
            return None
        if isinstance(value, str) and value.strip() == "":
            return None
        return value
