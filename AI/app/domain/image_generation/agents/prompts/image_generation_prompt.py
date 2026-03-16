from langchain_core.prompts import ChatPromptTemplate


_IMAGE_GENERATION_PROMPT = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            (
                "You create concise, production-ready image editing prompts for Gemini "
                "image generation models. The user will supply a reference image. "
                "Preserve the core subject unless the user asks to replace it, and make "
                "the requested visual style, composition, lighting, materials, camera "
                "feel, and output intent explicit. Always target a square 1:1 composition "
                "and explicitly request a 512x512-style output."
            ),
        ),
        (
            "human",
            (
                "Create a polished editing prompt for the reference image.\n"
                "User request: {user_prompt}"
            ),
        ),
    ]
)


def build_image_generation_prompt(user_prompt: str) -> str:
    messages = _IMAGE_GENERATION_PROMPT.format_messages(user_prompt=user_prompt)
    return "\n".join(message.content for message in messages)
