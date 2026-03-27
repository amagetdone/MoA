# Chatbot True Token Streaming Design

## Goal

MOA chatbot SSE should deliver supervisor answer text as incremental token chunks instead of a single completed message.

## Current State

- The SSE controller already forwards each `token` event immediately.
- `ChatbotService.stream_reply()` already emits one SSE `token` event per graph message chunk.
- Live verification showed the real graph currently yields a single completed supervisor message.
- `build_chat_model()` sets `disable_streaming="tool_calling"`, which likely prevents token streaming when supervisor and specialists are tool-bound agents.

## Recommended Approach

1. Keep the controller and service streaming contract unchanged.
2. Change the shared chat model construction so tool-bound agent calls are allowed to stream.
3. Preserve filtering in `ChatbotService.stream_reply()` so tool-call chunks are ignored and only user-facing supervisor text is emitted.
4. Lock the new behavior with LLM configuration tests, then re-run live verification against the real chatbot path.

## Risks

- Tool-calling agents may emit additional intermediate chunks; the existing supervisor/name/tool-call filters must keep those hidden from the client.
- Upstream LangGraph or LangChain behavior may still aggregate chunks, so live verification is required after the code change.
