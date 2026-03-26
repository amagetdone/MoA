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


def test_build_supervisor_prompt_blocks_unsupported_operational_promises() -> None:
    prompt = build_supervisor_prompt()
    normalized_prompt = prompt.casefold()

    assert "cannot sync accounts" in normalized_prompt
    assert "cannot upload receipts" in normalized_prompt
    assert "cannot change settings" in normalized_prompt
    assert "do not claim that you can perform" in normalized_prompt
    assert "only suggest follow-ups that this chatbot can actually support" in (
        normalized_prompt
    )
