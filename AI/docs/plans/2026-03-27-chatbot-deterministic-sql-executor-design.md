# Chatbot Deterministic SQL Executor Design

## Goal

Prevent the chatbot from treating an unexecuted SQL step as a successful
analysis path.

The desired outcome is that once `approved_sql` exists, the system either:

- deterministically executes the guarded SQL and passes a non-empty serialized
  `sql_result` into coaching, or
- fails closed and returns a safe fallback instead of allowing coaching to
  hallucinate around an empty result.

## Problem

The latest trace shows:

- `sql_team` finished with `completed`
- `coaching_team` also finished with `completed`
- but the response itself says `sql_result` was empty

The trace also shows that the `sql_executor` model had the
`run_guarded_sql_query` tool available but did not actually emit a tool call.
That means the graph allowed a "tool not called" state to pass through as if SQL
execution had succeeded.

## Root Cause

The current `sql_executor` is implemented as an LLM worker with a tool prompt.

That creates a structural gap:

- `approved_sql` exists
- the executor model may still choose not to call the SQL tool
- `_capture_sql_result()` then sees no tool message and stores `sql_result=""`
- the graph still routes unconditionally into `SpendWise Coach`
- finalization treats any non-empty coach draft as a successful answer

So the bug is not just "empty data." It is "missing execution treated as
successful execution."

## Approaches Considered

### Option 1: Keep the LLM executor and just fail when `sql_result` is empty

Pros:

- smallest patch
- minimal architecture change

Cons:

- still leaves execution dependent on model tool-use reliability
- preserves avoidable latency and complexity

### Option 2: Replace the LLM executor with a deterministic execution node

Pros:

- removes the unreliable tool-choice step entirely
- makes SQL execution behavior match the approved SQL contract exactly
- simplifies traces and reduces runtime ambiguity

Cons:

- requires touching graph wiring and tests
- needs a reusable guarded execution helper outside middleware

### Option 3: Keep the LLM executor and force tool_choice

Pros:

- smaller than a full deterministic rewrite

Cons:

- still leaves execution wrapped in an unnecessary model turn
- harder to reason about than direct execution
- does not improve debuggability as much as a deterministic node

## Recommendation

Choose Option 2, and also add fail-closed routing as a safety net.

The SQL is already fully approved by `QueryGuard` before this step. Once that
contract exists, there is no product value in asking another model to decide
whether to call the execution tool. Deterministic execution is the cleanest
match for the current simplified chatbot architecture.

## Proposed Design

### Deterministic executor

Replace the current LLM-based SQL executor subgraph with a deterministic node
that:

1. reads `approved_sql`
2. validates it again through the same guarded SQL rules
3. injects `user_id`
4. executes the `run_guarded_sql_query` tool directly
5. serializes the result into `sql_result`

This node should not depend on LLM tool-calling at all.

### Shared guarded execution helper

Move the core guarded execution logic into a reusable helper in
`sql_tools.py`, so both middleware-based tool execution and deterministic graph
execution use the same validation and parameter-sanitization rules.

### Fail-closed routing

After SQL execution, route conditionally:

- if `sql_result` is present, continue to `SpendWise Coach`
- if `sql_result` is empty or execution raised an error, finalize immediately
  with a safe fallback

This makes it impossible for coaching to be marked completed when SQL execution
never really happened.

### Finalization behavior

Add an explicit `sql_execution_failed` failure reason to state and finalization.

That keeps this case distinct from:

- `sql_team_failed` (approval never happened)
- `coaching_team_failed` (coaching output was empty or invalid)

## Scope Boundaries

This change intentionally does not:

- change the SQL approval loop
- change the QuerySmith/QueryGuard prompts
- redesign the coaching prompt for true 0-row SQL results
- alter the public API response shape

## Testing Strategy

Add tests that prove:

- successful requests no longer depend on the `sql_executor` model
- approved SQL executes deterministically via the guarded SQL tool
- if SQL execution cannot produce `sql_result`, the graph fails closed and does
  not run coaching

## Success Criteria

The change is successful when:

- a missing SQL tool call can no longer silently produce `sql_result=""`
- coaching never runs on an unexecuted SQL step
- approved SQL is executed deterministically
- targeted and broader chatbot tests remain green
