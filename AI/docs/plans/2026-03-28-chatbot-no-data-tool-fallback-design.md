# Chatbot No-Data Tool Fallback Design

## Goal

Keep the chatbot API and service contract unchanged while allowing the coaching agent
to continue answering when finance MCP tools fail with a recoverable "no data" style
error such as `User not found`.

## Problem

The current MCP tool execution middleware translates `ToolException("User not found")`
into `ChatbotUpstreamUnavailableError`, which becomes HTTP 503 at the controller
boundary. This makes the whole chat request fail even though the user asked for a
coaching answer that can still be generated without current finance records.

## Constraints

- Keep `/v1/chatbot/messages` and `/v1/chatbot/messages/stream` contracts unchanged.
- Preserve real upstream connectivity failures as controlled API failures.
- Do not leak internal tool names or orchestration details in the final answer.
- Keep `warnings` and `tool_summaries` extraction working with the current graph result
  normalization.

## Options

1. Keep translating all tool failures to API failures.
   - Simple, but blocks the LLM from giving fallback coaching.

2. Convert recoverable "no data" tool failures into `ToolMessage` observations and let
   the ReAct loop continue.
   - Recommended. Minimal change, matches LangChain tool error handling guidance, and
     preserves current API/service shells.

3. Catch the error after agent execution and synthesize an answer in the service layer.
   - More invasive and less faithful to the agent loop.

## Chosen Design

- Detect recoverable `ToolException` cases in `_ToolTraceMiddleware`.
- For `User not found`, return a synthetic `ToolMessage` instead of raising.
- Encode the tool result as JSON so existing `warnings` and `tool_summaries` extractors
  continue to work.
- Add an `observation` field that tells the model to continue with general coaching
  without unsupported numeric claims.
- Keep unrecoverable tool failures mapped to `ChatbotUpstreamUnavailableError`.
- Strengthen the coaching prompt so the model explicitly knows how to answer when
  tool results indicate that current finance data is unavailable.

## Expected Outcome

- Requests no longer fail with 503 for recoverable "data unavailable" tool outcomes.
- The model can still produce a calm Korean coaching answer.
- Clients still receive `warnings` and `tool_summaries` showing that data was missing.
