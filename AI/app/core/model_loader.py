from transformers import AutoModelForSequenceClassification, AutoTokenizer

from app.core.config import Settings
from app.services.merchant_classifier import MerchantClassifierService


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
