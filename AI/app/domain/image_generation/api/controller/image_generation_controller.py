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


@router.post(
    "",
    response_model=ImageGenerationResponse,
    summary="참조 이미지를 기반으로 이미지를 생성합니다",
    description=(
        "사용자 프롬프트와 참조 이미지를 함께 받아 Gemini 이미지 생성 모델로 결과 이미지를 생성합니다.\n\n"
        "mime_type에는 입력 이미지의 MIME 타입을 넣습니다. 예: image/jpeg, image/png, image/webp"
    ),
)
def generate_image(
    request: ImageGenerationRequest,
    service=Depends(get_image_generation_service),
) -> dict[str, str]:
    try:
        return service.generate(
            user_prompt=request.user_prompt,
            image_base64=request.image_base64,
            mime_type=request.mime_type,
        )
    except UpstreamImageGenerationError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    except ImageGenerationConfigurationError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
