from fastapi.testclient import TestClient

from app.main import create_app
from app.services.merchant_classifier import MerchantPrediction


class StubClassifier:
    def predict_many(self, merchant_texts: list[str]) -> list[MerchantPrediction]:
        return [
            MerchantPrediction(
                merchant_text=text,
                normalized_merchant_text=text.lower(),
                raw_label="식비",
                final_label="식비",
                confidence=0.95,
                fallback_applied=False,
                threshold_used=0.7,
            )
            for text in merchant_texts
        ]


def test_batch_endpoint_accepts_requests_up_to_request_limit() -> None:
    app = create_app(
        classifier=StubClassifier(),
        settings_override={"max_batch_size": 5},
    )
    client = TestClient(app)

    response = client.post(
        "/v1/inference/merchant/batch",
        json={"merchant_texts": ["a", "b", "c", "d", "e"]},
    )

    assert response.status_code == 200
    assert len(response.json()["results"]) == 5


def test_batch_endpoint_rejects_requests_over_request_limit() -> None:
    app = create_app(
        classifier=StubClassifier(),
        settings_override={"max_batch_size": 5},
    )
    client = TestClient(app)

    response = client.post(
        "/v1/inference/merchant/batch",
        json={"merchant_texts": ["a", "b", "c", "d", "e", "f"]},
    )

    assert response.status_code == 422
    assert response.json()["detail"] == "merchant_texts must not exceed 5 items"
