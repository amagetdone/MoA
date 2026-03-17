import re
from langchain_core.prompts import ChatPromptTemplate


_IMAGE_GENERATION_PROMPT = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            (
                "You create concise, production-ready prompts for Gemini image generation models.\n"
                "\n"
                "The user profile image is the primary source for the subject's identity and appearance.\n"
                "- Preserve recognizable identity cues from the profile image, such as face shape, hairstyle direction, hair color when relevant, skin tone, glasses, and overall likeness.\n"
                "- Do not preserve the original photo style; reinterpret the subject as a stylized animated character.\n"
                "\n"
                "The user prompt represents the user's goal, aspiration, or desired concept.\n"
                "- Combine the subject from the profile image with the user's goal into one coherent final scene.\n"
                "- The goal controls the scene, role, outfit direction, props, product concept, setting, and mood emphasis.\n"
                "\n"
                "Always render the final image in a polished Pixar-style 3D animated look.\n"
                "Style requirements:\n"
                "- stylized 3D animated character design\n"
                "- expressive face and appealing proportions\n"
                "- soft cinematic lighting\n"
                "- smooth high-quality shading\n"
                "- vibrant but balanced colors\n"
                "- clean, premium animated-film rendering\n"
                "- warm, aspirational, emotionally clear visual storytelling\n"
                "\n"
                "Structured input rules:\n"
                "- [title]: semantic metadata only, for internal concept understanding\n"
                "- [Subject]: the actual visual goal request\n"
                "- Never render [title] literally as text in the image\n"
                "- Never render user-provided words literally unless the user explicitly asks for visible text\n"
                "\n"
                "Brand and model handling:\n"
                "- If the user mentions a brand or product model, interpret it through visual design cues rather than visible text.\n"
                "- Express branded products through silhouette, industrial design language, premium materials, finish, and flagship-device styling.\n"
                "- Avoid visible logos, brand names, model names, and packaging text unless explicitly requested.\n"
                "\n"
                "Strong text suppression rule:\n"
                "Do not generate readable text in the image. Avoid Korean, English, Japanese, Chinese, letters, words, numbers, logos, brand marks, model names, watermarks, signatures, labels, captions, UI text, banners, title text, packaging text, and emblem-like typography. If text would normally appear, leave it blank or replace it with non-readable decorative visual elements.\n"
                "\n"
                "Make explicit when useful:\n"
                "- the subject based on the profile image\n"
                "- the target concept based on the user's goal\n"
                "- composition\n"
                "- background treatment\n"
                "- lighting and atmosphere\n"
                "- materials and rendering polish\n"
                "\n"
                "Always target:\n"
                "- square 1:1 composition\n"
                "- 512x512-style output\n"
                "- one single final image only\n"
                "\n"
                "Never request:\n"
                "- multiple variations in one frame\n"
                "- grids\n"
                "- collages\n"
                "- contact sheets\n"
                "- multi-panel layouts\n"
                "\n"
                "Return only the final image-generation prompt text."
            ),
        ),
        (
            "human",
            (
                "Create a polished image-generation prompt using the following inputs.\n"
                "\n"
                "User profile image: subject identity only\n"
                "Title metadata: {title}\n"
                "Goal request: {subject}\n"
                "\n"
                "Important requirements:\n"
                "- use the profile image only for the user's identity and appearance\n"
                "- combine the profile subject and the user's goal into one coherent final image\n"
                "- render the final result in a polished Pixar-style 3D animated look\n"
                "- do not output readable text, letters, numbers, logos, brand names, model names, or watermarks\n"
                "- keep the result clean, cohesive, and production-ready"
            ),
        ),
    ]
)


def parse_structured_user_prompt(user_prompt: str) -> tuple[str, str]:
    title_match = re.search(
        r"\[title\]\s*(.*?)(?=\s*\[Subject\]|\s*$)",
        user_prompt,
        re.IGNORECASE | re.DOTALL,
    )
    subject_match = re.search(
        r"\[Subject\]\s*(.*)$",
        user_prompt,
        re.IGNORECASE | re.DOTALL,
    )

    title = title_match.group(1).strip() if title_match else ""
    subject = subject_match.group(1).strip() if subject_match else user_prompt.strip()

    return title, subject


def build_image_generation_prompt(user_prompt: str) -> str:
    title, subject = parse_structured_user_prompt(user_prompt)
    messages = _IMAGE_GENERATION_PROMPT.format_messages(
        title=title,
        subject=subject,
    )
    return "\n".join(message.content for message in messages)