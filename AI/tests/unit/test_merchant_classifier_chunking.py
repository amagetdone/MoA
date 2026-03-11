import torch

from app.services.merchant_classifier import MerchantClassifierService


class FakeBatch(dict):
    def to(self, device: str):
        return self


class FakeTokenizer:
    def __init__(self) -> None:
        self.calls: list[list[str]] = []

    def __call__(
        self,
        inputs: list[str],
        *,
        padding: bool,
        truncation: bool,
        return_tensors: str,
    ) -> FakeBatch:
        self.calls.append(list(inputs))
        batch_size = len(inputs)
        return FakeBatch(
            {
                "input_ids": torch.ones((batch_size, 2), dtype=torch.long),
                "attention_mask": torch.ones((batch_size, 2), dtype=torch.long),
            }
        )


class FakeModel:
    def __init__(self) -> None:
        self.config = type("Config", (), {"id2label": {0: "기타서비스", 1: "식비"}})()

    def __call__(self, **encoded):
        batch_size = encoded["input_ids"].shape[0]
        logits = torch.tensor([[0.1, 2.0]] * batch_size)
        return type("Output", (), {"logits": logits})()


def test_predict_many_chunks_requests_and_preserves_order() -> None:
    tokenizer = FakeTokenizer()
    service = MerchantClassifierService(
        tokenizer=tokenizer,
        model=FakeModel(),
        fallback_label="기타서비스",
        fallback_threshold=0.7,
        inference_batch_size=2,
    )

    predictions = service.predict_many(["상점1", "상점2", "상점3", "상점4", "상점5"])

    assert [len(call) for call in tokenizer.calls] == [2, 2, 1]
    assert [prediction.merchant_text for prediction in predictions] == [
        "상점1",
        "상점2",
        "상점3",
        "상점4",
        "상점5",
    ]
    assert [prediction.raw_label for prediction in predictions] == ["식비"] * 5
