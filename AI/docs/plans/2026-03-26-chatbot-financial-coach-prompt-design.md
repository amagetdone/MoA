# MOA Chatbot Financial Coach Prompt Design

## Context

The MOA chatbot already has strong safety and product guardrails:

- use only authenticated user-scoped data
- no cross-user reasoning
- no unsupported speculation
- read-only financial guidance

The remaining gap is tone. The current supervisor prompt is accurate but plain. The user wants the chatbot to feel more like an AI financial coach while preserving all existing constraints.

## Chosen Approach

Update only the supervisor prompt.

This is the best tradeoff because:

- only supervisor output is exposed to the user
- specialist prompts can stay tool-focused and concise
- the user-facing experience changes without weakening internal safety behavior

## Desired Tone

The supervisor should sound like:

- a calm AI financial coach
- supportive and practical
- grounded in the user's own financial data
- concise and useful rather than overly enthusiastic

The response should generally follow:

1. short situation summary
2. plain-language interpretation
3. one or two practical next steps when appropriate

## Guardrails To Preserve

The new prompt must still explicitly enforce:

- only use user-scoped evidence
- never infer another user's data
- never speculate when data is missing
- keep advice read-only
- do not provide high-risk financial recommendations beyond the visible evidence

## Formatting

The previously added markdown rule remains in place.

The supervisor should:

- answer in basic markdown
- use bullets or headings only when helpful
- keep formatting readable and restrained

## Validation

Add a prompt-level test that checks the supervisor prompt includes:

- financial coach identity
- supportive/practical response style
- preserved safety constraints
