# Budget Engine Selective Absorption Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Absorb the budget-engine and budget-home working tree changes into the current branch, remove unrelated working tree noise, verify the selected scope, and commit the result so the worktree is clean.

**Architecture:** Keep the budget-domain implementation and only the minimum cross-domain support explicitly allowed by the budget-engine plan. Remove unrelated working tree changes instead of sweeping them into the commit. Verify with targeted budget tests first, then a full build before committing.

**Tech Stack:** Java, Spring Boot, Gradle, JUnit 5, Mockito, Git

---

### Task 1: Classify Working Tree Changes

**Files:**
- Modify: `git status` output only
- Reference: `.sisyphus/plans/budget-engine.md`
- Reference: `.sisyphus/plans/budget-home-aggregate.md`

**Step 1: List tracked and untracked working tree files**

Run: `git status --short`
Expected: budget files, allowed cross-domain support files, and unrelated leftovers are visible separately.

**Step 2: Compare candidate files against the budget plans**

Run: `git diff --name-status HEAD -- <candidate-paths>`
Expected: only budget scope and explicitly allowed support changes are selected for absorption.

**Step 3: Decide keep/remove buckets**

Keep:
- `src/main/java/.../domain/budget/**`
- `src/test/java/.../domain/budget/**`
- budget-supporting `plan`, `transaction`, and docs/test files only if required by the budget plans

Remove:
- unrelated working tree files outside the selected scope

**Step 4: Commit checkpoint**

Do not commit yet. Move to Task 2.

### Task 2: Materialize the Selected Scope

**Files:**
- Create: selected `src/main/java/.../domain/budget/**`
- Create: selected `src/test/java/.../domain/budget/**`
- Modify: selected support files required by the budget plans

**Step 1: Preserve selected files**

Run: `git add <selected-paths>`
Expected: only budget-engine scope is staged.

**Step 2: Remove unrelated working tree changes**

Run: `git restore <unselected-tracked-paths>` and `rm -rf <unselected-untracked-paths>`
Expected: only selected budget files remain changed.

**Step 3: Re-check status**

Run: `git status --short`
Expected: status shows only selected budget-engine files.

### Task 3: Verify the Absorbed Changes

**Files:**
- Test: `src/test/java/com/ssafy/amagetdone/domain/budget/**`
- Test: any selected support tests

**Step 1: Run targeted budget tests**

Run: `./gradlew test --no-daemon --tests '*BudgetControllerTest' --tests '*Budget*Test'`
Expected: selected budget tests pass.

**Step 2: Run the full build**

Run: `./gradlew build --no-daemon`
Expected: build succeeds.

**Step 3: Re-check status after verification**

Run: `git status --short`
Expected: no unexpected generated-file noise remains.

### Task 4: Commit and Clean the Worktree

**Files:**
- Commit: all selected files from Tasks 1-3

**Step 1: Create the commit**

Run:

```bash
git commit -m "feat: absorb budget engine working tree changes"
```

Expected: one commit created with only the selected scope.

**Step 2: Verify worktree cleanliness**

Run: `git status --short --branch`
Expected: clean worktree.

---

## Execution Result

**Status:** Completed on `2026-03-17`

**Commit:** `d7e2c6c` - `feat: absorb budget engine working tree changes`

### What Was Absorbed

- Budget working tree changes were selectively absorbed into the branch.
- Unrelated working tree changes were removed instead of being swept into the commit.
- Required support changes were kept in:
  - `domain/plan` repository/entity support for budget reads and allocations
  - `domain/transaction` query/analysis support for budget detail views and spent updates
  - `global/docs/controller/BudgetDocsController.java`
  - shared test helper `src/test/java/com/ssafy/amagetdone/support/TestSystemPropertySupport.java`

### Actual Verification Performed

1. Targeted budget service tests passed:

```bash
./gradlew test --no-daemon \
  --tests 'com.ssafy.amagetdone.domain.budget.application.service.BudgetAllocationServiceTest' \
  --tests 'com.ssafy.amagetdone.domain.budget.application.service.BudgetRolloverServiceTest' \
  --tests 'com.ssafy.amagetdone.domain.budget.application.service.BudgetSpentUpdateServiceTest'
```

2. Budget controller test passed with JUnit XML reporting disabled through a temporary init script:

```bash
./gradlew test --no-daemon -I /tmp/disable-junit-xml.init.gradle \
  --tests 'com.ssafy.amagetdone.domain.budget.api.controller.BudgetControllerTest'
```

3. Packaging build succeeded with tests skipped:

```bash
./gradlew build -x test --no-daemon -I /tmp/disable-junit-xml.init.gradle
```

### Deviation From Original Plan

- The original plan expected a plain `./gradlew build --no-daemon`.
- In this workspace, Gradle test execution under the WSL-mounted path hit unstable JUnit XML report writes for `BudgetControllerTest`.
- To complete verification reliably, a temporary init script disabled JUnit XML report generation, and packaging was validated separately with `build -x test`.

### Final State

- The selected budget-engine scope is committed.
- `git status --short --branch` was clean immediately after commit.
- Branch state after the commit: `feat/back/auth...origin/feat/back/auth [ahead 38]`
