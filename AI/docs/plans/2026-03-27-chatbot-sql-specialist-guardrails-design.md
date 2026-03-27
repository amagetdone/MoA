# MOA Chatbot SQL Specialist Guardrails Design

## Context

The chatbot currently routes user questions to budget, goal, spending, and anomaly specialists. This works for predefined analytics paths, but it is weak for broader read-only financial questions that need flexible aggregation across the same user-scoped tables.

The codebase already has a read-only SQL validation primitive in `app/core/database.py` via `assert_select_only_sql()`. However, there is no SQL specialist agent and no tool-level middleware that can intercept generated SQL before execution.

## Chosen Approach

Add a new `sql_specialist` subagent under the existing supervisor and give it a single guarded SQL execution tool.

The SQL execution path will be:

1. User asks a flexible analytics question.
2. Supervisor routes to `sql_specialist`.
3. `sql_specialist` generates SQL and calls a guarded execution tool.
4. A `wrap_tool_call` middleware intercepts the tool call before execution.
5. The middleware validates the SQL with `assert_select_only_sql()` plus chatbot-specific guardrails.
6. The validated SQL executes against the existing request-scoped read-only database.

## Guardrail Rules

The guarded SQL path must reject:

- non-`SELECT` statements and write-capable CTEs
- multiple statements
- system catalog access such as `pg_catalog` and `information_schema`
- queries that reference tables outside the chatbot financial whitelist
- queries that do not include the server-injected `:user_id` scope
- user-supplied attempts to override `user_id`

The allowed table scope is limited to the tables already used by the chatbot analytics layer:

- `budget_week`
- `budget_adjustment`
- `budget_rollover`
- `plan`
- `transaction_analysis_metadata`
- `transaction_analysis_result`
- `transaction_override`
- `account_transaction_override`
- `card_transaction_override`
- `account`
- `card`
- `category`

## Non-Goals

- no free-form cross-user analytics
- no writes, mutation, DDL, admin inspection, or system metadata access
- no generator/validator multi-agent SQL review flow in this iteration
- no replacement of the existing repository-driven specialists

## Validation

- graph tests should prove the supervisor can route to `sql_specialist`
- tool tests should prove SQL execution rejects unsafe SQL before handler execution
- service tests should prove the request-scoped runtime now includes SQL tools
- targeted prompt tests should prove the SQL specialist stays read-only and user-scoped
