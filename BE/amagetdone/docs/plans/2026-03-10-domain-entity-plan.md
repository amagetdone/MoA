# Domain Entity Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement ERD-aligned domain packages, enums, and JPA entities using the agreed domain split of `member`, `asset`, `transaction`, `plan`, and `budget`.

**Architecture:** The ERD is the source of truth. Domain boundaries are fixed first, then entities are added in dependency order with minimal repository scaffolding and verification after each slice. Package structure follows the approved `domain/<domain>/{api,application,model,infra,support}` layout.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring JDBC, PostgreSQL, JUnit 5

**Implementation Sources:**
- `/mnt/c/ssafy/특화/docs/erd/erd.png`
- `/mnt/c/ssafy/특화/docs/erd/2026-03-06-예산편성-erd-설계안.md`
- `docs/plans/2026-03-10-domain-entity-erd-spec.md`

**Execution Rules:**
- Entity implementation must follow `2026-03-10-domain-entity-erd-spec.md` table-by-table.
- Columns, defaults, nullability, and relationships in the spec document must not be omitted.
- JPA-based entity work must not start before the project has JPA dependencies and baseline tests still pass.

---

### Task 0: Lock ERD spec and JPA baseline

**Files:**
- Verify: `docs/plans/2026-03-10-domain-entity-erd-spec.md`
- Modify: `build.gradle`

**Step 1: Verify written ERD transcription exists**

Run: `sed -n '1,260p' docs/plans/2026-03-10-domain-entity-erd-spec.md`
Expected: all target tables are documented.

**Step 2: Add JPA dependency with a failing baseline check**

Add `spring-boot-starter-data-jpa` and any minimal test support required for entity scanning.

**Step 3: Run baseline verification**

Run: `./gradlew test`
Expected: PASS after the dependency change.

**Step 4: Commit**

```bash
git add build.gradle
git add -f docs/plans/2026-03-10-domain-entity-erd-spec.md docs/plans/2026-03-10-domain-entity-design.md docs/plans/2026-03-10-domain-entity-plan.md
git commit -m "docs: ERD 엔티티 구현 기준 정리"
```

### Task 1: Create domain package skeleton

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/member/.gitkeep`
- Create: `src/main/java/com/ssafy/amagetdone/domain/asset/.gitkeep`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/.gitkeep`
- Create: `src/main/java/com/ssafy/amagetdone/domain/plan/.gitkeep`
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/.gitkeep`

**Step 1: Write the failing check**

Verify the domain package roots do not exist yet.

**Step 2: Run check to verify it fails**

Run: `find src/main/java/com/ssafy/amagetdone/domain -maxdepth 2 -type d`
Expected: domain roots are missing or incomplete.

**Step 3: Write minimal implementation**

Create the five root domain packages and their approved subpackage structure.

**Step 4: Run check to verify it passes**

Run: `find src/main/java/com/ssafy/amagetdone/domain -maxdepth 3 -type d | sort`
Expected: `member`, `asset`, `transaction`, `plan`, `budget` and their subpackages exist.

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain
git commit -m "feat: 도메인 패키지 구조 추가"
```

### Task 2: Implement member entities from the ERD

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/member/model/entity/User.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/member/model/entity/SocialAccount.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/member/model/entity/Setting.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/member/model/entity/Agreement.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/member/model/entity/Policy.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/member/model/repository/*.java`
- Test: `src/test/java/com/ssafy/amagetdone/...`

**Step 1: Write the failing test**

Add a package-level persistence or mapping test that verifies member entities compile and Spring can scan them.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests '*member*'`
Expected: FAIL because member entities and repositories do not exist yet.

**Step 3: Write minimal implementation**

Create ERD-aligned member entities with fields and relationships taken directly from the ERD spec document.
Do not omit `users`, `social_account`, `setting`, `user_agreements`, `policy` columns recorded in the spec.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests '*member*'`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/member src/test/java
git commit -m "feat: member 엔티티 추가"
```

### Task 3: Implement asset and transaction entities from the ERD

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/asset/model/entity/*.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/entity/*.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/asset/model/repository/*.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/*.java`

**Step 1: Write the failing test**

Add tests that verify asset and transaction entities can be loaded and that override entities reflect the ERD shape.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests '*asset*' --tests '*transaction*'`
Expected: FAIL because entities are missing.

**Step 3: Write minimal implementation**

Create:

- `Account`
- `Card`
- `Pocket`
- `Category`
- `Merchant`
- `TransactionOverride`
- `CardTransactionOverride`
- `AccountTransactionOverride`

Add any supporting enums/value objects defined by the ERD.
Implement all columns and FK relations documented for `account`, `card`, `pocket`, `category`, `merchant`, `transaction_override`, `card_transaction_override`, and `account_transaction_override`.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests '*asset*' --tests '*transaction*'`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/asset src/main/java/com/ssafy/amagetdone/domain/transaction src/test/java
git commit -m "feat: 자산 및 거래 엔티티 추가"
```

### Task 4: Implement plan and budget entities from the ERD

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/plan/model/entity/*.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/model/entity/*.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/model/vo/BudgetAdjustmentType.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/plan/model/repository/*.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/model/repository/*.java`

**Step 1: Write the failing test**

Add tests covering the existence of plan and budget entities plus the `BudgetAdjustmentType` enum values.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests '*plan*' --tests '*budget*'`
Expected: FAIL because entities and enum are missing.

**Step 3: Write minimal implementation**

Create:

- `Plan`
- `PlanWeekAllocation`
- `BudgetMonth`
- `EssentialExpenseMonth`
- `BudgetWeek`
- `EssentialWeekAllocation`
- `BudgetAdjustment`
- `BudgetRollover`

Use the exact adjustment enum values already approved.
Implement all columns and FK relations documented for `plan`, `plan_week_allocation`, `budget_month`, `essential_expense_month`, `budget_week`, `essential_week_allocation`, `budget_adjustment`, and `budget_rollover`.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests '*plan*' --tests '*budget*'`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/plan src/main/java/com/ssafy/amagetdone/domain/budget src/test/java
git commit -m "feat: 목표 및 예산 엔티티 추가"
```

### Task 5: Run full verification

**Files:**
- Modify: none unless verification uncovers issues

**Step 1: Run all tests**

Run: `./gradlew test`
Expected: PASS

**Step 2: Verify package layout**

Run: `find src/main/java/com/ssafy/amagetdone/domain -maxdepth 5 -type f | sort`
Expected: ERD-aligned entity and repository files exist under the agreed domains.

**Step 3: Commit verification-only fixes if needed**

```bash
git add .
git commit -m "test: 엔티티 구조 검증 반영"
```
