from typing import TypedDict


class ImageGenerationState(TypedDict, total=False):
    prompt_goal: str
    final_prompt: str
    image_base64: str
    mime_type: str
    model: str
