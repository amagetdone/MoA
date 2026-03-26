from app.domain.chatbot.agents.prompts.chatbot_prompts import build_supervisor_prompt


def test_build_supervisor_prompt_includes_markdown_response_guidance() -> None:
    prompt = build_supervisor_prompt()

    assert "markdown" in prompt.casefold()
    assert "not excessive" in prompt.casefold()
    assert "financial coach" in prompt.casefold()
    assert "supportive" in prompt.casefold()
    assert "user-scoped evidence" in prompt.casefold()
    assert "read-only" in prompt.casefold()
    assert "never speculate" in prompt.casefold()
