from pydantic import BaseModel, field_validator


class ImageGenerationRequest(BaseModel):
    prompt_goal: str

    @field_validator("prompt_goal")
    @classmethod
    def validate_prompt_goal(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("prompt_goal must not be blank")
        return normalized


class ImageGenerationResponse(BaseModel):
    image_base64: str
    mime_type: str
    model: str
    prompt: str
