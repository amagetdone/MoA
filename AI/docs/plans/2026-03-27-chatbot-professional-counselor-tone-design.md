# MOA Chatbot Professional Counselor Tone Design

## Context

The MOA chatbot already enforces strong user-scoped, read-only, and markdown-oriented response rules through the supervisor prompt in `app/domain/chatbot/agents/prompts/chatbot_prompts.py`.

The remaining gap is response tone consistency. The product requirement is to guarantee a stable tone and manner regardless of how casually, emotionally, bluntly, or inconsistently the user writes.

The approved target style is:

- professional
- composed
- counselor-like
- consistent even for missing-data, limitation, and follow-up responses

## Approaches Considered

### 1. Update only the supervisor prompt

Pros:

- the supervisor is the final user-facing speaker
- the change is minimal and low-risk
- existing graph, API, and tool contracts remain unchanged

Cons:

- tone remains prompt-defined rather than externally configurable

### 2. Add a post-processing response rewriter

Pros:

- could normalize tone after generation

Cons:

- adds extra complexity and latency
- risks distorting grounded financial answers
- introduces another model-behavior layer to maintain

### 3. Add a configurable tone profile in settings

Pros:

- easier future runtime tuning

Cons:

- unnecessary for the current single approved tone
- expands configuration surface before there is a real product need

## Chosen Approach

Update only the supervisor prompt.

This is the safest and most direct option because the supervisor is the only agent whose final answer is exposed to the user. Reinforcing the supervisor prompt is sufficient to keep the chatbot's visible tone consistent without changing internal specialist behavior.

## Behavior

The supervisor should explicitly:

- maintain a professional and composed counselor tone
- avoid mirroring the user's casual, rude, playful, or emotional phrasing
- keep the same tone for normal answers, missing-data explanations, limitations, refusals, and follow-up questions
- stay polite, direct, and measured
- avoid slang, excessive enthusiasm, cheerleading, emojis, and decorative warmth

The existing guardrails must remain in place:

- user-scoped evidence only
- read-only guidance only
- no unsupported operational promises
- no speculation when evidence is missing
- concise markdown structure when helpful

## Non-Goals

- no API schema changes
- no graph topology changes
- no specialist prompt rewrite unless future behavior shows leakage into user-facing output
- no runtime tone configuration for this task

## Validation

Add prompt-level tests that require the supervisor prompt to include:

- the professional counselor identity
- explicit non-mirroring guidance
- consistent tone rules for missing data, limitations, and follow-up situations

This keeps the change narrow while making the output contract clearer and more durable.
