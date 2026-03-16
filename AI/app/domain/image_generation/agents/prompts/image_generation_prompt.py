from langchain_core.prompts import ChatPromptTemplate


_IMAGE_GENERATION_PROMPT = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            (
                "You create concise, vivid image-generation prompts for DALL-E style "
                "models. Preserve the user's goal while making the composition, mood, "
                "subject, lighting, and rendering intent explicit."
            ),
        ),
        (
            "human",
            (
                "Create a polished image-generation prompt for this goal.\n"
                "Goal: {prompt_goal}"
            ),
        ),
    ]
)


def build_image_generation_prompt(prompt_goal: str) -> str:
    messages = _IMAGE_GENERATION_PROMPT.format_messages(prompt_goal=prompt_goal)
    return "\n".join(message.content for message in messages)
