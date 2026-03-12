from contextlib import asynccontextmanager
from typing import Callable, Any

from fastapi import FastAPI

from app.api.routes.inference import router as inference_router
from app.core.config import Settings
from app.core.model_loader import build_merchant_classifier


def create_app(
    *,
    classifier: Any = None,
    load_classifier: Callable[[], Any] | None = None,
    settings_override: dict[str, Any] | None = None,
) -> FastAPI:
    settings = Settings(**(settings_override or {}))

    def default_classifier_loader() -> Any:
        return build_merchant_classifier(settings)

    classifier_loader = load_classifier or default_classifier_loader

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        if app.state.classifier is None:
            app.state.classifier = classifier_loader()
        yield

    app = FastAPI(lifespan=lifespan, root_path=settings.api_root_path)
    app.state.classifier = classifier
    app.state.settings = settings

    @app.get("/health")
    def health() -> dict[str, str]:
        return {"status": "ok"}

    app.include_router(inference_router)

    return app


app = create_app()
