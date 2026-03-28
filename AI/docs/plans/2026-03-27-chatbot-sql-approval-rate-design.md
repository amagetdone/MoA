# Chatbot SQL Approval Rate Design

## Goal

Increase the chatbot SQL path's approval rate without weakening any safety guardrails.
The desired outcome is that more user requests reach a safe SQL execution path and
produce grounded spending insights instead of the current safe fallback response.

## Current Limitation

The SQL retry loop already exists, but it is too weak to converge reliably:

- `QueryGuard` rejection feedback is mostly treated as a flat string.
- `QuerySmith` is asked to generate again, but it is not given a durable remediation
  plan or the exact previous SQL draft to repair.
- Deterministic SQL guardrails run only at execution time, which means some drafts
  can be "approved" by `QueryGuard` and still fail too late to help the retry loop.

This causes the system to consume retries without turning the rejection into an
actionable SQL repair instruction.

## Recommended Architecture

Keep the current `create_supervisor(...)` graph structure and improve the retry loop
inside the SQL team.

The approval-rate improvement should come from three changes:

1. Expand `QueryGuardReport` into a structured remediation contract.
2. Feed the previous SQL draft plus remediation plan back into `QuerySmith`.
3. Run deterministic SQL validation earlier, during the `QueryGuard` capture step,
   so local guardrail failures also become retry feedback instead of late executor
   failures.

The loop remains:

`QuerySmith -> QueryGuard -> QuerySmith`

but the handoff content becomes "repair this exact draft under these exact
constraints" instead of "try again."

## State Changes

Extend chatbot SQL-team state with fields that make remediation explicit:

- `sql_retry_context`: structured dict carrying the last remediation plan
- `last_candidate_sql`: previous SQL draft that should be repaired

These fields complement the existing:

- `candidate_sql`
- `sql_validation_report`
- `sql_retry_feedback`
- `sql_retry_count`
- `approved_sql`

## QueryGuard Contract

Add structured remediation fields to `QueryGuardReport`:

- `failed_checks`
- `required_predicates`
- `forbidden_patterns`
- `allowed_tables_hint`
- `rewrite_strategy`

These fields let `QuerySmith` repair the prior draft more mechanically:

- which checks failed
- which predicates must appear
- which patterns must be removed
- which tables are still acceptable
- what rewrite direction to take

## Deterministic Guardrail Integration

When `QueryGuard` says a query is valid, run the same deterministic SQL validator
that the executor middleware would later run.

If local validation fails:

- do not set `approved_sql`
- convert that failure into a structured remediation plan
- increment `sql_retry_count`
- send the remediation back to `QuerySmith`

This keeps execution safety unchanged while giving the retry loop one more useful
chance to repair the SQL before the final fallback path.

## QuerySmith Repair Prompt

`QuerySmith` should no longer operate as a pure "new query generator" after the
first failure. On retries, it should receive:

- the user's original request
- the previous candidate SQL
- the structured remediation plan
- a requirement to revise the existing draft rather than invent a totally new one

This should make retries more targeted and reduce repeated failure modes.

## Success Criteria

The change is successful when:

- more requests reach `sql_executor` with safe approved SQL
- deterministic guardrail failures are converted into retry feedback before execution
- the chatbot still never executes unsafe SQL
- the fallback path remains intact for genuinely unrepairable requests

## Testing Strategy

Add tests that verify:

- `QuerySmith` receives previous SQL plus structured remediation fields on retry
- a `QueryGuard` approval that fails deterministic validation is turned into another
  SQL-team retry instead of immediate execution
- a repaired second draft can be approved and executed successfully
- prompt contracts mention the structured remediation fields
