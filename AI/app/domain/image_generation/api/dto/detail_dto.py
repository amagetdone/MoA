from pydantic import BaseModel, Field, field_validator


class ImageGenerationRequest(BaseModel):
    user_prompt: str
    image_base64: str
    mime_type: str = Field(
        description=(
            "입력 이미지의 MIME 타입입니다. 예: image/jpeg, image/png, image/webp"
        ),
        examples=["image/jpeg"],
    )

    @field_validator("user_prompt", "image_base64", "mime_type")
    @classmethod
    def validate_non_blank_field(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("field must not be blank")
        return normalized


class ImageGenerationResponse(BaseModel):
    image_base64: str
    mime_type: str
    model: str
    prompt: str
