# Test Optimization Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove low-value tests, consolidate overlapping contract tests, and leave a smaller, faster, clearer backend test suite.

**Architecture:** Keep behavior-heavy controller/service/client tests intact, delete implementation-style tests, and merge fragmented mapping/documentation/security-contract tests into a few higher-signal suites. Prefer cheap contract tests over redundant Spring context boot tests unless the context itself is what we need to protect.

**Tech Stack:** Gradle, JUnit 5, Spring Boot Test, WebMvcTest, Mockito, reflection-based contract checks

---

### Task 1: Remove Low-Value Structural Tests

**Files:**
- Delete: `src/test/java/com/ssafy/amagetdone/domain/EntityLombokAnnotationTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/domain/ControllerServiceLombokConstructorTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/domain/BaseEntityScopeTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/global/config/SpringdocPropertyContractTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/AmagetdoneApplicationTests.java`

**Step 1: Remove files**

Delete the low-value structural tests above.

**Step 2: Run focused verification**

Run: `./gradlew test --tests '*SecurityConfig*'`

Expected: no dependency on the deleted files.

### Task 2: Consolidate Entity Mapping Tests

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/DomainMappingContractTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/domain/ErdSchemaAlignmentTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/domain/transaction/FkRelationshipMappingTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/domain/transaction/TransactionEntityMappingTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/domain/transaction/TransactionAnalysisEntityMappingTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/domain/budget/BudgetEntityMappingTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/domain/budget/EssentialExpenseSnapshotEntityMappingTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/domain/member/MemberEntityMappingTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/domain/plan/PlanEntityMappingTest.java`

**Step 1: Write consolidated contract test**

Create one contract-oriented test suite that covers the still-important entity mapping guarantees: selected column names, defaults, enum mapping, and key foreign-key relationships.

**Step 2: Remove fragmented mapping tests**

Delete the superseded files listed above.

**Step 3: Run focused verification**

Run: `./gradlew test --tests 'com.ssafy.amagetdone.domain.DomainMappingContractTest'`

Expected: the new contract suite passes and replaced files are no longer needed.

### Task 3: Consolidate Documentation and Security Contract Tests

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/global/config/OpenApiContractTest.java`
- Modify: `src/test/java/com/ssafy/amagetdone/config/SecurityConfigIntegrationTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/global/config/SwaggerConfigTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/global/docs/controller/BudgetHomeDocsContractTest.java`
- Delete: `src/test/java/com/ssafy/amagetdone/config/SecurityConfigPublicEndpointsTest.java`

**Step 1: Merge OpenAPI-focused assertions**

Create a single `OpenApiContractTest` that covers the surviving Swagger configuration and the explicit schema wrapper contract.

**Step 2: Keep only higher-signal security coverage**

Review `SecurityConfigIntegrationTest` and add any must-keep public-endpoint assertions before removing the narrower matcher-only test.

**Step 3: Run focused verification**

Run: `./gradlew test --tests '*OpenApiContractTest' --tests '*SecurityConfigIntegrationTest'`

Expected: contract behavior remains covered with fewer files.

### Task 4: Run Broad Regression Verification

**Files:**
- Verify: `src/test/java/com/ssafy/amagetdone/**`

**Step 1: Run the most affected slices**

Run: `./gradlew test --tests 'com.ssafy.amagetdone.config.*' --tests 'com.ssafy.amagetdone.domain.*' --tests 'com.ssafy.amagetdone.global.config.*'`

Expected: refactored areas pass.

**Step 2: Run full suite**

Run: `./gradlew test`

Expected: all remaining tests pass.

### Task 5: Commit and Push

**Files:**
- Commit: all updated test files and plan docs

**Step 1: Review git diff**

Run: `git status --short`

**Step 2: Commit**

Run: `git add docs/plans src/test/java && git commit -m "refactor: optimize backend test suite"`

**Step 3: Push**

Run: `git push -u origin ref/back/test`
