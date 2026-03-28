# Chatbot Professional Coaching Feedback Design

**Goal:** Strengthen the chatbot's user-facing coaching prompts so weekly spending feedback feels more professional, more grounded, and more immediately useful from the user's perspective.

**Scope:** Prompt and prompt-test changes only. No graph topology, API contract, tool-calling path, or storage behavior changes.

## Context

The current coaching prompt already enforces Korean markdown and a fixed high-level structure, but it does not yet define what "good coaching" means with enough specificity.

That leaves too much room for:

- generic comfort language
- shallow summaries without prioritization
- evidence-light explanations that sound confident
- action items that are technically polite but not operationally helpful

The product need is not only "nicer wording." The need is a tighter coaching contract that helps users understand:

- what matters most this week
- what is actually driving the problem
- what they can control now
- what concrete next step will reduce regret or overspending

## Approaches Considered

### 1. Tone-only rewrite

Pros:

- smallest change
- lowest risk

Cons:

- improves style more than usefulness
- still allows vague or repetitive guidance

### 2. Prompt contract hardening

Pros:

- directly targets coaching quality
- keeps the runtime architecture unchanged
- can be locked in with prompt tests

Cons:

- still depends on prompt adherence rather than a separate formatter

### 3. Add a post-generation coaching rewriter

Pros:

- could enforce a more polished final answer

Cons:

- adds latency and complexity
- risks rewriting grounded evidence into softer but less accurate advice
- unnecessary for the current scope

## Chosen Approach

Use prompt contract hardening.

This is the best fit because the problem is output quality, not missing runtime capability. The current graph already obtains the right data and routes the right tools more reliably than before. The next improvement is to tell the final coach and the response guard exactly what a "useful coaching answer" must contain.

## Output Contract

The final coaching answer should be guided toward these behaviors:

- Start with a one-line diagnosis before expanding details.
- Prioritize the one or two highest-impact drivers instead of listing every possible issue.
- Distinguish evidence-based claims from likely-pattern inferences when the tools do not fully prove something.
- Separate controllable spending pressure from unavoidable or essential burden when relevant.
- Give one to three actions only, but make each action concrete enough to execute immediately.
- Prefer advice that includes timing and condition, such as "today," "for the rest of this week," or "before the next purchase."
- Avoid vague lines like "spend carefully," "manage better," or "try to reduce expenses."
- Stay calm and supportive without becoming fluffy, apologetic, or repetitive.

## Prompt Changes

### `build_spendwise_coach_prompt()`

Strengthen the coach prompt so it explicitly requires:

- a concise diagnosis-first opening
- evidence-led explanation
- labeled inference when evidence is incomplete
- prioritization of the biggest drivers
- concrete action format
- a ban on generic filler coaching

### `build_response_guard_prompt()`

Strengthen the response guard so it rejects answers that are:

- generic but polite
- not grounded in evidence or clearly marked inference
- missing prioritization
- missing concrete next steps

## Non-Goals

- no new tool calls
- no new tool schemas
- no changes to streaming
- no new model role
- no scoring or rubric engine outside prompt text

## Verification

Add prompt tests first that require the new coaching contract to be stated explicitly, then update the prompts to satisfy them.

Verification should include:

- targeted prompt tests
- full chatbot prompt/graph/API regression suite
