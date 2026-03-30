# Savings Accrual API Split Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Split the ambiguous test savings-accrual endpoint into explicit backfill and instant-apply APIs without changing production auto-accrual behavior.

**Architecture:** Keep `DailyGoalSavingAccrualService` as the production reconciliation engine. Add a separate instant apply service for test/debug usage, rewire the controller to expose `/backfill` and `/apply`, and keep a shared response DTO with explicit operation metadata.

**Tech Stack:** Spring Boot, JPA, JUnit 5, Mockito

---

### Task 1: Red Tests For Split API Contract

**Files:**
- Modify: `src/test/java/com/ssafy/amagetdone/domain/budget/api/controller/BudgetTestControllerTest.java`
- Modify: `src/test/java/com/ssafy/amagetdone/domain/budget/application/service/DailyGoalSavingAccrualServiceTest.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/budget/application/service/GoalSavingAccrualApplyTestServiceTest.java`

**Steps:**
1. Add failing controller tests for:
   - `/savings-accrual/backfill`
   - `/savings-accrual/apply`
   - backfill request without scope
   - apply request with `DAY/WEEK/MONTH`
2. Update current service tests so old `force` semantics become backfill semantics.
3. Add failing apply service tests for day/week/month instant application and duplicate-call no-op.

### Task 2: Shared DTO And Controller Split

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/budget/api/dto/GoalSavingAccrualForceDto.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/budget/api/controller/BudgetTestController.java`

**Steps:**
1. Add `OperationType` and separate request records for backfill/apply.
2. Make `appliedScope` nullable for backfill responses.
3. Add two controller methods and distinct success messages.

### Task 3: Backfill Path

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/budget/application/service/DailyGoalSavingAccrualService.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/budget/application/usecase/ApplyDailyGoalSavingAccrualUseCase.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/budget/application/service/GoalSavingAccrualForceTestService.java`

**Steps:**
1. Replace `forceAccrual` semantics with `backfillAccrual`.
2. Remove scope from backfill request handling.
3. Preserve production `applyDailyAccrual()` behavior.

### Task 4: Instant Apply Path

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/application/service/GoalSavingAccrualApplyTestService.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/application/usecase/ApplyGoalSavingAccrualForceUseCase.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/budget/model/repository/BudgetAdjustmentRepository.java`

**Steps:**
1. Implement current-period force apply for `DAY/WEEK/MONTH`.
2. Use `PLAN_SAVED_ADD` plus a deterministic reason key for duplicate detection.
3. Support single-plan and all-active-plan modes.

### Task 5: Verification

**Files:**
- None

**Steps:**
1. Run focused controller/service tests.
2. Run existing daily auto-accrual regression tests.
3. Summarize behavior changes, updated URLs, and any deployment caveats.
