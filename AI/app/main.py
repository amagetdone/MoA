from contextlib import asynccontextmanager
from typing import Callable, Any

from fastapi import FastAPI

from app.api.routes.inference import router as inference_router
from app.core.config import Settings
from app.core.model_loader import build_merchant_classifier
from app.domain.image_generation.api.controller.image_generation_controller import (
    router as image_generation_router,
)
from app.domain.image_generation.service import ImageGenerationService


def create_app(
    *,
    classifier: Any = None,
    load_classifier: Callable[[], Any] | None = None,
    image_generation_service: Any = None,
    load_image_generation_service: Callable[[], Any] | None = None,
    settings_override: dict[str, Any] | None = None,
) -> FastAPI:
    settings = Settings(**(settings_override or {}))

    def default_classifier_loader() -> Any:
        return build_merchant_classifier(settings)

    def default_image_generation_service_loader() -> Any:
        return ImageGenerationService(settings=settings)

    classifier_loader = load_classifier or default_classifier_loader
    image_generation_service_loader = (
        load_image_generation_service or default_image_generation_service_loader
    )

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        if app.state.classifier is None:
            app.state.classifier = classifier_loader()
        if app.state.image_generation_service is None:
            app.state.image_generation_service = image_generation_service_loader()
        try:
            yield
        finally:
            image_generation_service = getattr(
                app.state,
                "image_generation_service",
                None,
            )
            close = getattr(image_generation_service, "aclose", None)
            if callable(close):
                await close()

    app = FastAPI(lifespan=lifespan, root_path=settings.api_root_path)
    app.state.classifier = classifier
    app.state.image_generation_service = image_generation_service
    app.state.settings = settings

    @app.get("/health")
    def health() -> dict[str, str]:
        return {"status": "ok"}

    app.include_router(inference_router)
    app.include_router(image_generation_router)

    return app


app = create_app()
