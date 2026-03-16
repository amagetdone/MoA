from app.core.config import Settings
from app.domain.image_generation.agents.graph import build_image_generation_graph
from app.domain.image_generation.agents.prompts.image_generation_prompt import (
    build_image_generation_prompt,
)


def test_build_image_generation_prompt_expands_goal_into_renderable_prompt() -> None:
    prompt = build_image_generation_prompt("우주 배경의 초현실적인 고양이 일러스트")

    assert "우주 배경의 초현실적인 고양이 일러스트" in prompt
    assert prompt != "우주 배경의 초현실적인 고양이 일러스트"


def test_image_generation_graph_builds_prompt_and_delegates_generation() -> None:
    settings = Settings(
        AI_API_KEY="relay-key",
        OPENAI_BASE_URL="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
    )
    upstream_calls: list[dict[str, str]] = []

    def fake_image_generator(*, prompt: str, model: str, size: str) -> dict[str, str]:
        upstream_calls.append(
            {
                "prompt": prompt,
                "model": model,
                "size": size,
            }
        )
        return {
            "image_base64": "base64-image",
            "mime_type": "image/png",
            "model": model,
        }

    graph = build_image_generation_graph(
        settings=settings,
        image_generator=fake_image_generator,
    )

    result = graph.invoke({"prompt_goal": "우주 배경의 초현실적인 고양이 일러스트"})

    assert result["image_base64"] == "base64-image"
    assert result["mime_type"] == "image/png"
    assert result["model"] == "dall-e-3"
    assert "우주 배경의 초현실적인 고양이 일러스트" in result["final_prompt"]
    assert upstream_calls == [
        {
            "prompt": result["final_prompt"],
            "model": "dall-e-3",
            "size": "1024x1024",
        }
    ]
