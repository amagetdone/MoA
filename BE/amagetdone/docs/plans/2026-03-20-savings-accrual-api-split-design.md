# Savings Accrual API Split Design

## Goal

Split the ambiguous test endpoint for goal saving accrual into two explicit test/debug APIs:

- `backfill`: catch up cumulative accrual from plan start through cutoff
- `apply`: instantly add one day/week/month of planned saving

Production auto-accrual behavior remains unchanged.

## Problem

`POST /api/test/budget/savings-accrual/force` currently looks like an instant apply API, but its implementation behaves like a cutoff-based reconciliation API. That mismatch confuses QA and manual verification.

## Design

### 1. Backfill API

- Endpoint: `POST /api/test/budget/savings-accrual/backfill`
- Request: `planId` only, optional
- Meaning:
  - If `planId` is omitted, reconcile all ACTIVE plans for the user
  - If `planId` is present, reconcile only that owned plan
  - Compute expected cumulative accrual from plan start through cutoff and apply only the delta
- Scope is removed entirely from this API

### 2. Apply API

- Endpoint: `POST /api/test/budget/savings-accrual/apply`
- Request:
  - `planId` optional
  - `scope` optional, default `WEEK`
- Meaning:
  - `DAY`: immediately apply one daily slice from the current week allocation
  - `WEEK`: immediately apply the current week's full `finalAmount`
  - `MONTH`: immediately apply the current month's summed `finalAmount` across overlapping weekly allocations

### 3. Logging and Idempotency

- Backfill continues using auto-accrual adjustment types because it models historical catch-up
- Apply uses `PLAN_SAVED_ADD`, which is already distinct from auto-accrual types
- Apply no-op rule:
  - Same `user + plan + scope + period` should not be applied twice
  - Detect prior force-apply adjustments by standardized reason key stored in `BudgetAdjustment.reason`

### 4. Response

Keep one shared response DTO with explicit operation semantics:

- `operationType`: `BACKFILL` or `APPLY`
- `appliedScope`: nullable for backfill, set for apply
- `targetType`
- `planId`
- `appliedAmount`
- `executedDate`
- `cutoffDate`
- `periodStartDate`
- `periodEndDate`
- `periodLabel`
- `plans[]`

## Testing

- Backfill:
  - cumulative catch-up through cutoff
  - no scope in request
  - plan ownership validation
- Apply:
  - `DAY` adds one daily slice only
  - `WEEK` adds current week full `finalAmount`
  - `MONTH` adds current month full amount, including partial-month overlap cases
  - repeated apply on same period is no-op
- Regression:
  - production daily auto-accrual tests remain green
