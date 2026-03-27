# Chatbot SQL Supervisor Orchestration Design

## Goal

Replace the chatbot's SQL handling path with an explicit multi-agent workflow:

1. A SQL query generator agent proposes a read-only query.
2. A SQL validator agent reviews that proposal and either approves it or rejects it.
3. A supervisor SQL executor agent performs the only real SQL tool call.
4. The supervisor finalizer writes the user-facing markdown answer.

## Why Change

The current `sql_specialist` agent owns both SQL generation and execution. That is
safe enough with middleware, but it does not match the intended responsibility split.
The desired architecture makes each responsibility visible and testable:

- query generation
- query review
- query execution
- answer synthesis

## Recommended Architecture

Build a custom `StateGraph` for the chatbot instead of relying on
`langgraph_supervisor.create_supervisor(...)`.

The graph keeps the current non-SQL specialist flow mostly unchanged while giving the
SQL path a fixed orchestration:

`START -> supervisor_router -> sql_query_generator -> sql_query_validator -> supervisor_sql_executor -> supervisor_finalizer -> END`

For non-SQL questions:

`START -> supervisor_router -> domain_specialist -> supervisor_finalizer -> END`

## State Additions

The chatbot state needs SQL workflow fields in addition to the existing message list:

- `active_route`
- `specialist_answer`
- `candidate_sql`
- `validator_decision`
- `validator_feedback`
- `validated_sql`
- `sql_result`

These fields allow the graph to move deterministically without parsing free-form text
out of past messages every time.

## Agent Roles

### Supervisor Router

Classifies the request into one of:

- `budget`
- `goal`
- `spending`
- `anomaly`
- `sql`

It should only decide the route and not call tools.

### SQL Query Generator

Produces a single SQL proposal that aims to answer the question using the allowed
finance tables. It must not call tools.

### SQL Validator

Reviews the generated SQL and returns a structured decision:

- approve
- reject

When approving, it may keep the SQL or revise it. When rejecting, it must explain why.

### Supervisor SQL Executor

The only agent that owns the SQL tool. It receives the validated SQL and is expected
to run exactly that query. Its tool middleware re-checks the query before execution.

### Supervisor Finalizer

Creates the final markdown answer from either:

- specialist evidence, or
- SQL result rows and validator metadata

## Guardrails

Execution-time guardrails remain deterministic and should not depend on LLM behavior.

The supervisor execution tool path should enforce:

- `assert_select_only_sql`
- allowed finance-table whitelist
- `users_id = :user_id` requirement
- schema restriction
- no caller override of `user_id`

Even if the generator and validator both fail, the executor middleware must still stop
unsafe SQL before any database call is made.

## Testing Strategy

Add graph tests that verify:

- SQL questions traverse generator -> validator -> executor -> finalizer
- non-SQL questions skip the SQL pipeline
- rejected SQL never reaches `execute_select`
- approved SQL is executed only by the supervisor executor node

Add service tests that verify the external chatbot contract stays intact.
