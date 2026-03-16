from fastapi import APIRouter, Depends, HTTPException

from app.api.dependencies import get_image_generation_service
from app.domain.image_generation.api.dto.detail_dto import (
    ImageGenerationRequest,
    ImageGenerationResponse,
)
from app.domain.image_generation.service import (
    ImageGenerationConfigurationError,
    UpstreamImageGenerationError,
)

router = APIRouter(prefix="/v1/image-generation", tags=["image-generation"])


@router.post("", response_model=ImageGenerationResponse)
def generate_image(
    request: ImageGenerationRequest,
    service=Depends(get_image_generation_service),
) -> dict[str, str]:
    try:
        return service.generate(request.prompt_goal)
    except UpstreamImageGenerationError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    except ImageGenerationConfigurationError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
