from collections.abc import Callable

from app.core.config import Settings
from app.domain.image_generation.agents.states.state import ImageGenerationState

ImageGenerator = Callable[..., dict[str, str]]


def build_generate_image_node(
    *,
    settings: Settings,
    image_generator: ImageGenerator,
):
    def generate_image_node(state: ImageGenerationState) -> ImageGenerationState:
        image_result = image_generator(
            prompt=state["final_prompt"],
            model=settings.image_generation_model,
            size=settings.image_generation_size,
        )
        return {
            "image_base64": image_result["image_base64"],
            "mime_type": image_result["mime_type"],
            "model": image_result["model"],
        }

    return generate_image_node
