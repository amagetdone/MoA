from __future__ import annotations

from pydantic import BaseModel


class MerchantInferenceResponse(BaseModel):
    merchant_text: str
    normalized_merchant_text: str
    raw_label: str
    final_label: str
    confidence: float
    fallback_applied: bool
    threshold_used: float
