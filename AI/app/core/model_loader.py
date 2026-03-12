from transformers import AutoModelForSequenceClassification, AutoTokenizer

from app.core.config import Settings
from app.services.merchant_classifier_service import MerchantClassifierService

EXPECTED_MODEL_TYPE = "electra"
EXPECTED_MODEL_ARCHITECTURE = "ElectraForSequenceClassification"


def _validate_loaded_sequence_classifier(model, model_source: str) -> None:
    config = getattr(model, "config", None)
    model_type = getattr(config, "model_type", None)
    architectures = list(getattr(config, "architectures", []) or [])

    if model_type != EXPECTED_MODEL_TYPE:
        raise ValueError(
            f"Model '{model_source}' resolved to model_type='{model_type}', "
            f"architectures={architectures}, expected model_type="
            f"'{EXPECTED_MODEL_TYPE}' and architecture "
            f"'{EXPECTED_MODEL_ARCHITECTURE}'."
        )

    if architectures and EXPECTED_MODEL_ARCHITECTURE not in architectures:
        raise ValueError(
            f"Model '{model_source}' resolved to architectures={architectures}, "
            f"expected '{EXPECTED_MODEL_ARCHITECTURE}'."
        )


def load_sequence_classifier(settings: Settings) -> dict[str, object]:
    model_source = settings.model_source
    tokenizer = AutoTokenizer.from_pretrained(
        model_source,
        revision=settings.model_revision,
        token=settings.hf_token,
    )
    model = AutoModelForSequenceClassification.from_pretrained(
        model_source,
        revision=settings.model_revision,
        token=settings.hf_token,
    )
    _validate_loaded_sequence_classifier(model, model_source)
    model = model.to(settings.model_device)
    model.eval()

    return {
        "tokenizer": tokenizer,
        "model": model,
    }


def build_merchant_classifier(settings: Settings) -> MerchantClassifierService:
    artifacts = load_sequence_classifier(settings)
    return MerchantClassifierService(
        tokenizer=artifacts["tokenizer"],
        model=artifacts["model"],
        fallback_label=settings.fallback_label,
        fallback_threshold=settings.fallback_threshold,
        inference_batch_size=settings.inference_batch_size,
        device=settings.model_device,
    )
