# Chatbot Markdown Output Contract Design

**Goal:** Strengthen the spending coach system prompt so user-facing answers are consistently returned as Korean markdown with a stable section structure.

**Scope:** Only prompt and prompt-test changes for the chatbot coaching runtime. No API, service, streaming, or tool-calling contract changes.

## Recommended Approach

Use prompt-level contract reinforcement instead of adding a formatter layer.

- Keep the current `create_agent` runtime unchanged.
- Strengthen the coaching prompt with explicit Korean markdown section names.
- Strengthen the response-guard prompt so it treats Korean markdown structure as the default user-facing contract.
- Lock the behavior with prompt tests first.

## Why This Approach

- It keeps the implementation small and low-risk.
- It preserves the current service and API surface.
- It directly addresses the user requirement: stronger markdown guidance in the prompt itself.
- It avoids introducing a second formatting layer that could conflict with streamed output.

## Output Contract

The coaching answer should be guided toward this shape:

- `## 이번 소비 한눈에`
- `## 왜 이렇게 됐는지`
- `## 이번 주 행동 제안`

Additional prompt rules:

- Answer in Korean markdown.
- Use short bullets where helpful.
- Do not return JSON or code blocks unless the user explicitly asks.
- Keep internal tool/context details hidden.

## Verification

- Prompt contract tests should fail before the prompt update.
- Relevant chatbot prompt and regression tests should pass after the change.
