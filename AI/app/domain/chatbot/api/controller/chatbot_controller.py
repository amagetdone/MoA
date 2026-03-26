from __future__ import annotations

import json
from collections.abc import AsyncIterator
from typing import Annotated

from fastapi import APIRouter, Depends, Header
from fastapi.responses import StreamingResponse

from app.api.dependencies import get_chatbot_service
from app.domain.chatbot.api.dto.detail_dto import (
    ChatbotMessageRequest,
    ChatbotMessageResponse,
)

router = APIRouter(prefix="/v1/chatbot", tags=["chatbot"])
TrustedUserId = Annotated[int, Header(alias="X-User-Id", ge=1)]


@router.post("/messages", response_model=ChatbotMessageResponse)
async def create_chatbot_message(
    request: ChatbotMessageRequest,
    user_id: TrustedUserId,
    service=Depends(get_chatbot_service),
) -> dict[str, object]:
    return await service.reply(
        user_id=user_id,
        message=request.message,
        conversation_id=request.conversation_id,
    )


@router.post("/messages/stream")
async def stream_chatbot_message(
    request: ChatbotMessageRequest,
    user_id: TrustedUserId,
    service=Depends(get_chatbot_service),
) -> StreamingResponse:
    async def event_stream() -> AsyncIterator[str]:
        async for event in service.stream_reply(
            user_id=user_id,
            message=request.message,
            conversation_id=request.conversation_id,
        ):
            event_name = str(event["event"])
            payload = json.dumps(event["data"], ensure_ascii=False)
            yield f"event: {event_name}\ndata: {payload}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")
