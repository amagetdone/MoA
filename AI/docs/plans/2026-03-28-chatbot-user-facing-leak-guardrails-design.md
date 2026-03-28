# Chatbot User-Facing Leak Guardrails Design

**Goal:** Prevent internal finance-analysis logic from leaking into user-visible chatbot coaching while still explaining data quality issues in user-friendly language.

**Scope:** Prompt contract hardening, response-guard tightening, and fail-closed leak-pattern expansion for final user-facing text.

## Problem

The current chatbot can expose internal enum-like labels and field names such as `NON_ESSENTIAL`, `INCOME`, `OUT`, or `analysisIncluded: false` in the final coaching answer.

That is risky because:

- it leaks internal implementation details
- it is confusing to users
- it weakens trust even when the underlying analysis is correct

## Chosen Policy

Use the user's preferred policy level: user-friendly translation.

This means:

- Do not expose internal labels, flags, field names, or raw tool snippets.
- Do explain the meaning in plain Korean at the product-concept level.
- Do mention uncertainty or reduced accuracy when classification data is incomplete.

## Approach

1. Strengthen the coach prompt so it explicitly forbids raw internal labels and requires plain-language translation.
2. Strengthen the response guard prompt so it rejects answers containing enum values, field names, or backend-style snippets.
3. Expand the final-response leak detector so obviously internal labels are fail-closed scrubbed if the prompt still slips.

## User-Facing Translation Rules

Allowed style:

- “일부 거래는 가맹점 또는 용도 정보가 부족해 이번 주 분석 정확도가 낮을 수 있습니다.”
- “수입으로 분류된 거래는 소비 분석에서 제외되어 있습니다.”
- “일부 거래는 이번 소비 평가에 참고용으로만 반영되었습니다.”

Disallowed style:

- `NON_ESSENTIAL`
- `INCOME`
- `OUT`
- `analysisIncluded: false`
- raw tool names or backend field names

## Verification

- Add prompt tests for forbidden internal labels and required user-friendly translation guidance.
- Add graph leak tests for raw internal labels in final user-facing text.
- Run the chatbot prompt/graph/service/API regression suite.
