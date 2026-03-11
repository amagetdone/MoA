from __future__ import annotations

import re
import unicodedata
from dataclasses import asdict, dataclass

import torch


@dataclass(slots=True)
class ResolvedPrediction:
    raw_label: str
    final_label: str
    confidence: float
    fallback_applied: bool


@dataclass(slots=True)
class MerchantPrediction:
    merchant_text: str
    normalized_merchant_text: str
    raw_label: str
    final_label: str
    confidence: float
    fallback_applied: bool
    threshold_used: float

    def to_dict(self) -> dict[str, str | float | bool]:
        return asdict(self)


def _clean_raw_merchant_text(merchant_text: str) -> str:
    raw_text = unicodedata.normalize("NFKC", str(merchant_text or ""))
    raw_text = raw_text.replace("\r", " ").replace("\n", " ")
    raw_text = re.sub(r"\s+", " ", raw_text).strip()
    return raw_text


def normalize_service_merchant_text(merchant_text: str) -> str:
    text = _clean_raw_merchant_text(merchant_text)
    return text.casefold()


def build_model_input_text(merchant_text: str) -> str:
    raw_text = _clean_raw_merchant_text(merchant_text)
    normalized_text = normalize_service_merchant_text(raw_text)
    return f"{raw_text} [SEP] {normalized_text}"


def resolve_final_label(
    raw_label: str,
    confidence: float,
    fallback_label: str,
    fallback_threshold: float,
) -> ResolvedPrediction:
    final_label = fallback_label if confidence < fallback_threshold else raw_label
    return ResolvedPrediction(
        raw_label=raw_label,
        final_label=final_label,
        confidence=confidence,
        fallback_applied=final_label != raw_label,
    )


class MerchantClassifierService:
    def __init__(
        self,
        tokenizer,
        model,
        fallback_label: str,
        fallback_threshold: float,
        inference_batch_size: int = 128,
        device: str = "cpu",
    ) -> None:
        self.tokenizer = tokenizer
        self.model = model
        self.fallback_label = fallback_label
        self.fallback_threshold = fallback_threshold
        self.inference_batch_size = max(1, int(inference_batch_size))
        self.device = device

    def predict_one(self, merchant_text: str) -> MerchantPrediction:
        return self.predict_many([merchant_text])[0]

    def predict_many(self, merchant_texts: list[str]) -> list[MerchantPrediction]:
        if not merchant_texts:
            return []

        raw_texts = [_clean_raw_merchant_text(text) for text in merchant_texts]
        normalized_texts = [
            normalize_service_merchant_text(raw_text) for raw_text in raw_texts
        ]
        id2label = getattr(self.model.config, "id2label", {})

        predictions: list[MerchantPrediction] = []
        for start in range(0, len(raw_texts), self.inference_batch_size):
            raw_chunk = raw_texts[start : start + self.inference_batch_size]
            normalized_chunk = normalized_texts[start : start + self.inference_batch_size]
            input_chunk = [
                f"{raw_text} [SEP] {normalized_text}"
                for raw_text, normalized_text in zip(
                    raw_chunk, normalized_chunk, strict=True
                )
            ]
            encoded = self.tokenizer(
                input_chunk,
                padding=True,
                truncation=True,
                return_tensors="pt",
            )
            if hasattr(encoded, "to"):
                encoded = encoded.to(self.device)

            with torch.inference_mode():
                output = self.model(**encoded)

            probabilities = torch.softmax(output.logits, dim=-1)
            confidences, label_indices = torch.max(probabilities, dim=-1)

            for raw_text, normalized_text, confidence, label_index in zip(
                raw_chunk,
                normalized_chunk,
                confidences.tolist(),
                label_indices.tolist(),
                strict=True,
            ):
                raw_label = id2label[int(label_index)]
                resolved = resolve_final_label(
                    raw_label=raw_label,
                    confidence=float(confidence),
                    fallback_label=self.fallback_label,
                    fallback_threshold=self.fallback_threshold,
                )
                predictions.append(
                    MerchantPrediction(
                        merchant_text=raw_text,
                        normalized_merchant_text=normalized_text,
                        raw_label=resolved.raw_label,
                        final_label=resolved.final_label,
                        confidence=resolved.confidence,
                        fallback_applied=resolved.fallback_applied,
                        threshold_used=self.fallback_threshold,
                    )
                )

        return predictions
