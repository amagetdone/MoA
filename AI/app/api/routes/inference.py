from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Request

from app.schemas.inference import (
    MerchantBatchInferenceRequest,
    MerchantBatchInferenceResponse,
    MerchantInferenceRequest,
    MerchantInferenceResponse,
)

router = APIRouter(prefix="/v1/inference", tags=["inference"])


def _serialize_prediction(prediction: Any) -> dict[str, Any]:
    if hasattr(prediction, "to_dict"):
        return prediction.to_dict()
    return prediction


def get_classifier(request: Request) -> Any:
    classifier = getattr(request.app.state, "classifier", None)
    if classifier is None:
        raise HTTPException(status_code=503, detail="classifier is not initialized")
    return classifier


def get_settings(request: Request) -> Any:
    return request.app.state.settings


@router.post("/merchant", response_model=MerchantInferenceResponse)
def infer_merchant(
    request: MerchantInferenceRequest,
    classifier: Any = Depends(get_classifier),
) -> dict[str, Any]:
    return _serialize_prediction(classifier.predict_one(request.merchant_text))


@router.post("/merchant/batch", response_model=MerchantBatchInferenceResponse)
def infer_merchant_batch(
    request: MerchantBatchInferenceRequest,
    classifier: Any = Depends(get_classifier),
    settings: Any = Depends(get_settings),
) -> dict[str, list[dict[str, Any]]]:
    if len(request.merchant_texts) > settings.max_batch_size:
        raise HTTPException(
            status_code=422,
            detail=f"merchant_texts must not exceed {settings.max_batch_size} items",
        )
    return {
        "results": [
            _serialize_prediction(result)
            for result in classifier.predict_many(request.merchant_texts)
        ]
    }
