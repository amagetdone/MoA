from __future__ import annotations

from pydantic import BaseModel

from app.schemas.merchant_inference_response import MerchantInferenceResponse


class MerchantBatchInferenceResponse(BaseModel):
    results: list[MerchantInferenceResponse]
