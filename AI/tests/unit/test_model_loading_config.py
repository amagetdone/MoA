from app.core.config import Settings
from app.core import model_loader


def test_blank_model_loading_settings_are_normalized_to_none() -> None:
    settings = Settings(
        _env_file=None,
        model_revision="",
        model_local_dir="",
        hf_token="",
    )

    assert settings.model_revision is None
    assert settings.model_local_dir is None
    assert settings.hf_token is None
    assert settings.model_source == settings.model_id


def test_loader_passes_normalized_revision_and_hf_token(monkeypatch) -> None:
    tokenizer_calls: list[dict[str, object]] = []
    model_calls: list[dict[str, object]] = []

    class FakeModel:
        def __init__(self) -> None:
            self.moved_to: str | None = None
            self.eval_called = False

        def to(self, device: str) -> "FakeModel":
            self.moved_to = device
            return self

        def eval(self) -> None:
            self.eval_called = True

    fake_model = FakeModel()

    def fake_tokenizer_from_pretrained(source: str, **kwargs):
        tokenizer_calls.append({"source": source, **kwargs})
        return object()

    def fake_model_from_pretrained(source: str, **kwargs):
        model_calls.append({"source": source, **kwargs})
        return fake_model

    monkeypatch.setattr(
        model_loader.AutoTokenizer,
        "from_pretrained",
        fake_tokenizer_from_pretrained,
    )
    monkeypatch.setattr(
        model_loader.AutoModelForSequenceClassification,
        "from_pretrained",
        fake_model_from_pretrained,
    )

    settings = Settings(
        _env_file=None,
        model_id="demo/model",
        model_revision="",
        hf_token="secret-token",
        model_device="cpu",
    )

    artifacts = model_loader.load_sequence_classifier(settings)

    assert artifacts["model"] is fake_model
    assert tokenizer_calls == [
        {
            "source": "demo/model",
            "revision": None,
            "token": "secret-token",
        }
    ]
    assert model_calls == [
        {
            "source": "demo/model",
            "revision": None,
            "token": "secret-token",
        }
    ]
    assert fake_model.moved_to == "cpu"
    assert fake_model.eval_called is True
