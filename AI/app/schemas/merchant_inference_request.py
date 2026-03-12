from __future__ import annotations

from pydantic import BaseModel, field_validator

from app.schemas.text_validation import validate_non_blank_text


class MerchantInferenceRequest(BaseModel):
    merchant_text: str

    @field_validator("merchant_text")
    @classmethod
    def validate_merchant_text(cls, value: str) -> str:
        return validate_non_blank_text(value, "merchant_text")
