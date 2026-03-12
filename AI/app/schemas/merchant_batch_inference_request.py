from __future__ import annotations

from pydantic import BaseModel, Field, field_validator

from app.schemas.text_validation import validate_non_blank_text


class MerchantBatchInferenceRequest(BaseModel):
    merchant_texts: list[str] = Field(min_length=1)

    @field_validator("merchant_texts")
    @classmethod
    def validate_merchant_texts(cls, value: list[str]) -> list[str]:
        return [validate_non_blank_text(item, "merchant_texts") for item in value]
