# Chatbot LangGraph Internal Removal Design

## Goal

Remove the LangGraph-based internal chatbot agent implementation while keeping the chatbot API layer, DTOs, controller surface, and service method signatures in place.

## Constraints

- Touch only the chatbot domain.
- Do not modify the image generation domain.
- Keep the existing Python files in place.
- Keep service-layer entry points such as `reply`, `stream_reply`, and conversation history access.
- Keep API-level request and response contracts, including SSE event framing.
- Remove the actual LangGraph workflow behavior and related agent internals.
- Existing agent-focused tests are expected to break after the removal.

## Chosen Direction

Use a compatibility-shell approach:

1. Keep `app/domain/chatbot/agents/**` files and exported symbols.
2. Replace their internal behavior with explicit placeholders that signal the runtime has been removed.
3. Preserve imports used by the service layer so startup wiring and module structure remain intact.
4. Keep controller and service call paths unchanged.

## What Stays

- `app/domain/chatbot/api/**`
- `app/domain/chatbot/api/dto/**`
- `app/domain/chatbot/service/chatbot_service.py`
- Chatbot conversation memory and repository modules
- Public symbols such as `build_chatbot_graph`, `ChatbotToolBundle`, prompt builders, state type, and tool builders

## What Gets Removed

- LangGraph workflow assembly in `graph.py`
- Agent prompt content
- SQL tool and finance tool real behavior
- Agent contract parsing logic
- State annotations that depend on LangGraph internals

## Placeholder Behavior

- `build_chatbot_graph(...)` returns a runtime-compatible placeholder object.
- Placeholder runtime exposes `ainvoke`, `astream`, and graph-inspection methods, but raises a dedicated runtime-removed error when used.
- Prompt builders return minimal placeholder text instead of real orchestration prompts.
- SQL and finance tool builders return placeholder tools or middleware that fail when invoked.
- Contract parsers raise the same runtime-removed error.

## Expected Test Impact

- Agent implementation tests should fail because old behavior no longer exists.
- Service tests that use fake graph factories should continue to pass.
- API tests that inject fake services should continue to pass.
- Real end-to-end chatbot execution through the removed runtime is expected to fail until the new implementation is introduced.

## Risk Management

- Files and import paths remain stable, which reduces downstream churn during the later reimplementation.
- Service and API layers stay available for the next implementation phase.
- The removed-runtime error makes the absence of internals explicit instead of silently returning misleading output.
