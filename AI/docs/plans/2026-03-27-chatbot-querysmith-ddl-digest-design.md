# Chatbot QuerySmith DDL Digest Design

## Goal

Reduce avoidable SQL rejections by giving QuerySmith a compact, domain-specific
schema digest derived from `ddl.md`, instead of relying only on generic
"use schema tools" instructions.

The desired outcome is that QuerySmith chooses safer table paths, user-scoping
predicates, and join patterns on the first draft more often.

## Current Limitation

QuerySmith currently knows:

- it must stay read-only
- it must stay user-scoped
- it may use schema tools

But it does not receive a compact domain map for the MOA spending tables.
That creates three recurring problems:

- it can start from the wrong ownership table
- it can over-join because the shortest safe path is not obvious
- it can miss the canonical join chain between analyzed transactions and
  user-owned account or card transaction records

`ddl.md` already documents the relevant table structure, but that information is
not being turned into prompt-time guidance.

## Approaches Considered

### Option 1: Prompt-only hardening

Strengthen the QuerySmith system prompt with more generic safety language.

Pros:

- smallest code change
- no new runtime state

Cons:

- still abstract
- does not encode MOA-specific join paths
- likely improves style more than approval rate

### Option 2: DDL-derived schema digest in QuerySmith input

Build a compact schema digest from the allowlisted portion of `ddl.md` and inject
it into QuerySmith's user message alongside retry context.

Pros:

- grounded in the real schema
- keeps runtime simple
- directly helps first-draft table selection and joins

Cons:

- requires curation so the digest stays concise
- needs tests to prevent prompt bloat or drift

### Option 3: Separate SQL planner layer

Add a deterministic planner that maps user intent to target tables before
QuerySmith drafts SQL.

Pros:

- strongest structural control

Cons:

- much larger change
- higher complexity than the current chatbot needs

## Recommendation

Choose Option 2.

It is the best fit for the current simplified chatbot graph:

`QuerySmith -> QueryGuard -> SQL executor -> SpendWise Coach -> deterministic finalizer`

This gives QuerySmith stronger domain context without re-introducing orchestration
complexity.

## Proposed Design

Add a small DDL-derived schema digest focused on the actual SQL allowlist tables
that matter for spending analysis.

The digest should describe:

- preferred starting tables
- ownership tables and user-scoping anchors
- canonical join paths
- tables that are context-only vs analysis-first
- tables that should usually be avoided unless the request explicitly needs them

The digest should not dump raw DDL. It should be a compact operational guide for
query generation.

## Digest Scope

Prioritize these table groups:

- ownership anchors: `account`, `card`
- transaction ownership bridges: `account_transaction_override`,
  `card_transaction_override`
- analyzed facts: `transaction_analysis_result`
- transaction base facts: `transaction_override`
- lookup tables: `category`, `merchant`
- planning/budget tables when the user asks about budget, plans, or weekly/monthly
  allocations: `budget_week`, `budget_month`, `budget_adjustment`, `plan`,
  `plan_week_allocation`, `essential_*`, `pocket`

The digest should explicitly call out canonical spending-analysis paths such as:

- `account -> account_transaction_override -> transaction_override -> transaction_analysis_result`
- `card -> card_transaction_override -> transaction_override -> transaction_analysis_result`

## Prompt and Input Changes

Strengthen `build_querysmith_prompt()` so it tells QuerySmith to:

- read the provided schema digest before drafting
- choose the smallest valid table path that answers the request
- prefer analyzed tables when the user asks for spending patterns or category
  insights
- use ownership anchors first when user scope is not already obvious
- avoid planning tables unless the request is about budget/plan context

Also extend `_prepare_querysmith_input()` so the digest is sent in the runtime
message, not only buried in the static system prompt.

## Success Criteria

The change is successful when:

- QuerySmith receives a concise schema digest on every SQL drafting turn
- prompts mention canonical user-scoped join strategy
- tests lock the digest and prompt contract
- approval rate improves without weakening any deterministic guardrails

## Testing Strategy

Add tests that verify:

- the QuerySmith prompt references DDL-derived schema guidance and canonical join
  behavior
- QuerySmith input includes the schema digest
- the digest includes the key ownership and analyzed-fact table relationships
- retry guidance still coexists with the new digest
