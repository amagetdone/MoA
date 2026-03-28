# Chatbot No-Data Tool Fallback

## Summary

Implemented recoverable finance MCP tool fallback for "no data" cases.

## What Changed

- `_ToolTraceMiddleware` now converts recoverable `ToolException("User not found")`
  into a synthetic `ToolMessage` with:
  - `status = "no_data"`
  - a warning for the outer API contract
  - an observation that tells the model to continue with general coaching
- Non-recoverable tool failures still raise `ChatbotUpstreamUnavailableError`.
- The spendwise coach system prompt now explicitly says to continue coaching when
  current finance data is unavailable.

## Verification

- `uv run python -m pytest tests/app/domain/chatbot/agents/test_chatbot_graph.py::test_build_chatbot_graph_allows_general_coaching_when_tool_reports_missing_user_data tests/app/domain/chatbot/agents/test_chatbot_prompts.py::test_build_spendwise_coach_prompt_requires_actionable_markdown_coaching_sections -q`
- `uv run python -m pytest tests/app/domain/chatbot tests/app/test_chatbot_api.py tests/lab/test_chatbot_graph_visualization_notebook.py -q`

## Notes

- This change is intentionally scoped to recoverable no-data failures only.
- Real MCP connectivity or backend execution failures still surface through the
  existing controlled 503 path.
