from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_id: str = Field(
        default="kakao1513/merchant-consumption-category-discriminator-v1",
        alias="MODEL_ID",
    )
    model_device: str = Field(default="cpu", alias="MODEL_DEVICE")
    fallback_label: str = Field(default="기타서비스", alias="FALLBACK_LABEL")
    fallback_threshold: float = Field(default=0.70, alias="FALLBACK_THRESHOLD")
    top_k: int = Field(default=3, alias="TOP_K")
    max_batch_size: int = Field(default=1000, alias="MAX_BATCH_SIZE")
    inference_batch_size: int = Field(default=128, alias="INFERENCE_BATCH_SIZE")
    model_revision: str | None = Field(default=None, alias="MODEL_REVISION")
    model_local_dir: str | None = Field(default=None, alias="MODEL_LOCAL_DIR")
    hf_token: str | None = Field(default=None, alias="HF_TOKEN")
    api_root_path: str = Field(default="", alias="API_ROOT_PATH")
    ai_api_key: str = Field(default="", alias="AI_API_KEY")
    openai_base_url: str = Field(default="", alias="OPENAI_BASE_URL")
    image_generation_model: str = Field(
        default="gemini-2.5-flash-image",
        alias="IMAGE_GENERATION_MODEL",
    )
    langfuse_public_key: str | None = Field(default=None, alias="LANGFUSE_PUBLIC_KEY")
    langfuse_secret_key: str | None = Field(default=None, alias="LANGFUSE_SECRET_KEY")
    langfuse_base_url: str | None = Field(default=None, alias="LANGFUSE_BASE_URL")
    langfuse_tracing_enabled: bool = Field(
        default=True,
        alias="LANGFUSE_TRACING_ENABLED",
    )

    model_config = SettingsConfigDict(
        env_file=(".env", "app/.env"),
        env_file_encoding="utf-8",
        extra="ignore",
        populate_by_name=True,
    )

    @property
    def model_source(self) -> str:
        return self.model_local_dir or self.model_id

    @field_validator(
        "model_revision",
        "model_local_dir",
        "hf_token",
        "langfuse_public_key",
        "langfuse_secret_key",
        "langfuse_base_url",
        mode="before",
    )
    @classmethod
    def blank_strings_to_none(cls, value: str | None) -> str | None:
        if value is None:
            return None
        if isinstance(value, str) and value.strip() == "":
            return None
        return value
