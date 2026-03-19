# Transaction Seed Extension Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Extend the existing account and card transaction seed scenarios from the current 2025-12 dataset through 2026-03-20 without changing the initialization contract.

**Architecture:** Keep the raw `List.of(...)` seed structure intact in the scenario classes, preserve `baseMonth` as `2025-12`, and extend the tests so they verify the new multi-month coverage and monthly surplus sanity checks. Account seeds remain the ledger and cash-flow source of truth, while card seeds continue to represent merchant-level spending context.

**Tech Stack:** Java, JUnit 5, Mockito, Gradle

---

### Task 1: Lock the desired behavior with tests

**Files:**
- Modify: `src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeAccountTransactionOverridesUseCaseTest.java`
- Modify: `src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeCardTransactionOverridesUseCaseTest.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/account/support/util/TransactionSeedScenarioSanityTest.java`

**Step 1: Write the failing tests**

- Update the account/card initialize use case tests so they still expect `baseMonth()` to be `2025-12`, but now assert that initialized transactions span `2025-12`, `2026-01`, `2026-02`, and `2026-03`.
- Add a dedicated scenario sanity test that:
  - verifies account seeds contain rows for `2026-01`, `2026-02`, and `2026-03`
  - verifies card seeds contain rows for `2026-01`, `2026-02`, and `2026-03`
  - verifies monthly account inflow totals remain slightly above account outflow totals for `2026-01`, `2026-02`, and `2026-03`

**Step 2: Run the targeted tests and confirm failure**

Run: `./gradlew test --tests '*InitializeAccountTransactionOverridesUseCaseTest' --tests '*InitializeCardTransactionOverridesUseCaseTest' --tests '*TransactionSeedScenarioSanityTest'`

Expected: tests fail because the current seed data only contains `2025-12`.

### Task 2: Extend account transaction seeds

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/support/util/AccountRealTransactionScenario.java`

**Step 1: Add 2026-01 account ledger entries**

- Append January seeds that preserve the current memo/summary style:
  - one or more salary/support-style deposits
  - a few peer transfers and refund-like inflows
  - wallet/card settlement outflows
  - more food-related withdrawals than the December baseline, using existing `우아한형제들_TOS`, `쿠팡`, `토스페이`, `카카오페이`, and similar strings

**Step 2: Add 2026-02 account ledger entries**

- Repeat the same style with slightly varied dates and amounts, keeping monthly net positive but modest.

**Step 3: Add 2026-03 account ledger entries through 2026-03-20**

- Add partial-month seeds only through March 20 while preserving plausible balance progression.

### Task 3: Extend card transaction seeds

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/support/util/CardRealTransactionScenario.java`

**Step 1: Add 2026-01 merchant transactions**

- Reuse the existing merchant mix and keep food/coffee/transit merchants dominant.
- Add a small number of cancel/pending-style rows so the card pattern still looks realistic.

**Step 2: Add 2026-02 merchant transactions**

- Keep the same merchant character with slightly more food spending than December.

**Step 3: Add 2026-03 merchant transactions through 2026-03-20**

- Keep the dataset partial for March and align some spending dates with account outflows.

### Task 4: Verify and tune the dataset

**Files:**
- Modify if needed: `src/main/java/com/ssafy/amagetdone/domain/account/support/util/AccountRealTransactionScenario.java`
- Modify if needed: `src/main/java/com/ssafy/amagetdone/domain/account/support/util/CardRealTransactionScenario.java`

**Step 1: Run targeted tests**

Run: `./gradlew test --tests '*InitializeAccountTransactionOverridesUseCaseTest' --tests '*InitializeCardTransactionOverridesUseCaseTest' --tests '*TransactionSeedScenarioSanityTest'`

Expected: PASS

**Step 2: Inspect monthly totals**

Run a small one-off script or shell command to summarize the monthly account inflow/outflow totals and ensure each new month stays slightly net positive.

**Step 3: Commit**

```bash
git add docs/plans/2026-03-20-transaction-seed-extension.md \
  src/main/java/com/ssafy/amagetdone/domain/account/support/util/AccountRealTransactionScenario.java \
  src/main/java/com/ssafy/amagetdone/domain/account/support/util/CardRealTransactionScenario.java \
  src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeAccountTransactionOverridesUseCaseTest.java \
  src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeCardTransactionOverridesUseCaseTest.java \
  src/test/java/com/ssafy/amagetdone/domain/account/support/util/TransactionSeedScenarioSanityTest.java
git commit -m "test: extend transaction seed scenarios through march"
```
