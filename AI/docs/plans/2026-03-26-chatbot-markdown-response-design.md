# MOA Chatbot Markdown Response Design

## Context

The MOA chatbot currently returns plain text in the `answer` field. The final user-facing response comes from the supervisor agent prompt in `app/domain/chatbot/agents/prompts/chatbot_prompts.py`.

The requirement is to let the LLM generate markdown-formatted answers directly for user requests.

## Chosen Approach

Update only the supervisor system prompt.

This is the smallest and safest change because:

- only the supervisor response is exposed to the user
- specialist agent messages are internal coordination artifacts
- streaming already forwards only supervisor output tokens

## Behavior

The supervisor should:

- write the final answer in basic markdown
- use structure only when it helps readability
- prefer short paragraphs and flat bullet lists
- use tables only when comparing categories or summarized numeric values
- avoid decorative or excessive formatting

## Non-Goals

- no markdown post-processing layer
- no change to API schema
- no forced markdown on internal specialist prompts

## Validation

Add a prompt-level test that checks the supervisor prompt contains explicit markdown instructions.

This keeps the change minimal while making the output contract clearer for the model.
