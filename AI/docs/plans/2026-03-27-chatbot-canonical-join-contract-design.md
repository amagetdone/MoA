# Chatbot Canonical Join Contract Design

## Goal

Stop false SQL rejections for common spending-feedback requests by aligning
`QuerySmith` and `QueryGuard` with the repository's already trusted ownership
join contract.

The desired outcome is that generic prompts such as "이번주 소비 피드백 해줘."
can produce an approved spending query more often, without weakening the current
read-only and user-scoping rules.

## Problem

The chatbot currently falls back even when `QuerySmith` drafts a plausible query.

The reproduced failure shows this query pattern being generated:

- `account_transaction_override.account_id = account.id`
- `transaction_override.id = account_transaction_override.id`
- `transaction_analysis_result.transaction_override_id = transaction_override.id`

That pattern is rejected by `QueryGuard` as an unsafe or unverified ownership
bridge, even though the repository already uses the same primary-key bridge
contract in its trusted user-scoped transaction CTE.

As a result, the system has multiple sources of truth for the same ownership
relationship.

## Root Cause

Three layers are not using the same contract:

- repository SQL uses the verified bridge
  - `account_transaction_override.id = transaction_override.id`
  - `card_transaction_override.id = transaction_override.id`
- the QuerySmith schema digest describes the path too loosely
- QueryGuard does not explicitly know that these joins are canonical and trusted

This mismatch causes valid query drafts to be treated as unsafe.

## Approaches Considered

### Option 1: Keep current join guidance and only increase retries

Pros:

- smallest change
- no prompt contract expansion

Cons:

- repeats the same false reject more times
- does not fix the actual disagreement between layers

### Option 2: Promote repository join rules into the SQL specialist contract

Pros:

- fixes the root-cause mismatch
- small, prompt-level change
- keeps current simplified graph intact
- improves approval rate without relaxing deterministic safety rules

Cons:

- requires tighter prompt tests so the contract does not drift again

### Option 3: Replace these requests with a deterministic query builder

Pros:

- strongest control for weekly spending summaries
- minimizes LLM join invention

Cons:

- larger product decision
- narrows flexibility for other spending-analysis questions
- more change than needed for the current false reject

## Recommendation

Choose Option 2.

The repository already contains the safest operational truth for ownership
bridging. The most effective fix is to make that same truth explicit in the
QuerySmith schema digest and in QueryGuard's validation instructions.

This keeps the current architecture simple while removing the mismatch that is
causing repeated fallback responses.

## Proposed Design

### Canonical ownership contract

Treat these joins as verified and canonical when user scope is also proven:

- account spending path
  - `account_transaction_override.id = transaction_override.id`
  - `account_transaction_override.account_id = account.id`
- card spending path
  - `card_transaction_override.id = transaction_override.id`
  - `card_transaction_override.card_unique_no = card.card_unique_no`
  - `card.withdrawal_account_no = account.id`
- analyzed transaction path
  - `transaction_analysis_result.transaction_override_id = transaction_override.id`

### QuerySmith changes

Strengthen the schema digest so it no longer describes ownership bridges in an
abstract way. It should explicitly state the exact bridge keys and present the
canonical join chains in near-SQL form.

Also strengthen the QuerySmith prompt so it prefers these exact joins instead of
inventing alternative bridge predicates.

### QueryGuard changes

Strengthen the QueryGuard prompt so it does not reject the canonical bridge joins
merely because the relationship is represented as shared primary-key identity
across override tables.

The validator should still reject:

- missing `:user_id`
- missing explicit ownership path
- unapproved tables
- sensitive columns
- DML/DDL

The change is about clarifying trusted joins, not weakening safety.

## Scope Boundaries

This change intentionally does not:

- relax the deterministic ban on `UNION`
- introduce a deterministic SQL builder
- change the simplified graph structure
- widen the SQL allowlist

If fallback still happens after this contract alignment, the next decision point
should be whether to introduce a deterministic query builder for recurring
summary flows.

## Testing Strategy

Add prompt-contract tests that fail until the exact canonical join rules are
present.

Verify:

- QuerySmith prompt mentions exact canonical join behavior
- QueryGuard prompt mentions the trusted ownership joins
- schema digest includes the exact bridge predicates
- existing QuerySmith input tests still confirm the digest is injected at runtime

## Success Criteria

The change is successful when:

- the prompt/digest contract explicitly matches repository join behavior
- tests lock that contract
- the known false-reject pattern is no longer caused by prompt ambiguity
- no deterministic SQL safety rule is weakened in this step
