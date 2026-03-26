from __future__ import annotations

from fastapi import APIRouter, Depends, Header

from app.api.dependencies import get_chatbot_service
from app.domain.chatbot.api.dto.detail_dto import (
    ChatbotMessageRequest,
    ChatbotMessageResponse,
)

router = APIRouter(prefix="/v1/chatbot", tags=["chatbot"])


@router.post("/messages", response_model=ChatbotMessageResponse)
async def create_chatbot_message(
    request: ChatbotMessageRequest,
    user_id: int = Header(alias="X-User-Id"),
    service=Depends(get_chatbot_service),
) -> dict[str, object]:
    return await service.reply(
        user_id=user_id,
        message=request.message,
        conversation_id=request.conversation_id,
    )
