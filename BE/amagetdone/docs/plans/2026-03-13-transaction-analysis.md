# Transaction Analysis API Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add transaction synchronization, async consumption analysis, expense summary queries, weekly and monthly analysis queries, and the weekly essential-expense budget endpoint while reusing the existing SSAFY-aligned transaction entities and leaving the current budget tables unchanged.

**Architecture:** Raw transactions continue to live in `TransactionOverride`, `AccountTransactionOverride`, and `CardTransactionOverride`. New persistence is limited to analysis jobs, per-transaction analysis output, and weekly essential-expense snapshots. Query APIs read either from the existing transaction tables joined with analysis results or from the new weekly snapshot tables, while synchronization and analysis orchestration stay inside the `transaction` domain.

**Tech Stack:** Java 17, Spring Boot 3, Spring Web, Spring Data JPA, RestClient, PostgreSQL, JUnit 5, Mockito

---

### Task 1: Add failing tests for new analysis and snapshot persistence

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/transaction/TransactionAnalysisEntityMappingTest.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/budget/EssentialExpenseSnapshotEntityMappingTest.java`
- Modify: `src/test/java/com/ssafy/amagetdone/domain/ErdSchemaAlignmentTest.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/entity/TransactionAnalysisJob.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/entity/TransactionAnalysisResult.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/vo/TransactionAnalysisJobStatus.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/vo/TransactionAnalysisStep.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/vo/TransactionExcludedReason.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/model/entity/EssentialExpenseSnapshotWeek.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/model/entity/EssentialExpenseSnapshotWeekCategory.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/TransactionAnalysisJobRepository.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/TransactionAnalysisResultRepository.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/model/repository/EssentialExpenseSnapshotWeekRepository.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/model/repository/EssentialExpenseSnapshotWeekCategoryRepository.java`

**Step 1: Write the failing test**

Add mapping tests that assert:

```java
Class<?> job = assertEntityClass("TransactionAnalysisJob", "transaction_analysis_job");
Class<?> result = assertEntityClass("TransactionAnalysisResult", "transaction_analysis_result");
assertManyToOneField(TransactionAnalysisResult.class, "transaction", TransactionOverride.class, "transaction_override_id");
assertManyToOneField(EssentialExpenseSnapshotWeekCategory.class, "snapshotWeek", EssentialExpenseSnapshotWeek.class, "snapshot_week_id");
```

Extend `ErdSchemaAlignmentTest` to verify `@Enumerated` and `@ColumnDefault` declarations on the new enums and status fields.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.TransactionAnalysisEntityMappingTest" --tests "com.ssafy.amagetdone.domain.budget.EssentialExpenseSnapshotEntityMappingTest" --tests "com.ssafy.amagetdone.domain.ErdSchemaAlignmentTest"`

Expected: FAIL because the new entities, enums, and repositories do not exist yet.

**Step 3: Write minimal implementation**

Create the new entities and repositories with:

- `TransactionAnalysisJob` referencing `User`
- `TransactionAnalysisResult` referencing `TransactionOverride` and `Category`
- `EssentialExpenseSnapshotWeek` referencing `User`
- `EssentialExpenseSnapshotWeekCategory` referencing `EssentialExpenseSnapshotWeek` and `Category`

Keep all existing budget entities unchanged.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.TransactionAnalysisEntityMappingTest" --tests "com.ssafy.amagetdone.domain.budget.EssentialExpenseSnapshotEntityMappingTest" --tests "com.ssafy.amagetdone.domain.ErdSchemaAlignmentTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/transaction/model/entity/TransactionAnalysisJob.java src/main/java/com/ssafy/amagetdone/domain/transaction/model/entity/TransactionAnalysisResult.java src/main/java/com/ssafy/amagetdone/domain/transaction/model/vo/TransactionAnalysisJobStatus.java src/main/java/com/ssafy/amagetdone/domain/transaction/model/vo/TransactionAnalysisStep.java src/main/java/com/ssafy/amagetdone/domain/transaction/model/vo/TransactionExcludedReason.java src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/TransactionAnalysisJobRepository.java src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/TransactionAnalysisResultRepository.java src/main/java/com/ssafy/amagetdone/domain/budget/model/entity/EssentialExpenseSnapshotWeek.java src/main/java/com/ssafy/amagetdone/domain/budget/model/entity/EssentialExpenseSnapshotWeekCategory.java src/main/java/com/ssafy/amagetdone/domain/budget/model/repository/EssentialExpenseSnapshotWeekRepository.java src/main/java/com/ssafy/amagetdone/domain/budget/model/repository/EssentialExpenseSnapshotWeekCategoryRepository.java src/test/java/com/ssafy/amagetdone/domain/transaction/TransactionAnalysisEntityMappingTest.java src/test/java/com/ssafy/amagetdone/domain/budget/EssentialExpenseSnapshotEntityMappingTest.java src/test/java/com/ssafy/amagetdone/domain/ErdSchemaAlignmentTest.java
git commit -m "feat: add transaction analysis persistence models"
```

### Task 2: Add failing tests for repository lookups over existing transaction tables

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/transaction/model/repository/TransactionRepositoryQueryTest.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/AccountTransactionOverrideRepository.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/CardTransactionOverrideRepository.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/TransactionOverrideRepository.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/TransactionAnalysisJobRepository.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/TransactionAnalysisResultRepository.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/budget/model/repository/EssentialExpenseSnapshotWeekRepository.java`

**Step 1: Write the failing test**

Cover repository contracts for:

```java
Optional<AccountTransactionOverride> findTopByAccount_IdOrderByTransactionDateTimeDesc(String accountId);
Optional<CardTransactionOverride> findTopByCard_CardUniqueNoOrderByTransactionDateTimeDesc(Long cardUniqueNo);
boolean existsByAccount_IdAndTransactionUniqueNo(String accountId, Long transactionUniqueNo);
boolean existsByCard_CardUniqueNoAndTransactionUniqueNo(Long cardUniqueNo, Long transactionUniqueNo);
Optional<TransactionAnalysisJob> findFirstByUser_IdAndStatusOrderByRequestedAtDesc(Integer userId, TransactionAnalysisJobStatus status);
```

Also add read queries for:

- transaction list by user and date range
- expense summary aggregation
- monthly analysis lookup
- weekly essential snapshot lookup by user and `yearWeek`

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.model.repository.TransactionRepositoryQueryTest"`

Expected: FAIL because the required repository methods do not exist yet.

**Step 3: Write minimal implementation**

Add derived query methods or `@Query` methods to the existing repositories. Keep the existing transaction entities unchanged and express the new behavior through repository interfaces only.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.model.repository.TransactionRepositoryQueryTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/AccountTransactionOverrideRepository.java src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/CardTransactionOverrideRepository.java src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/TransactionOverrideRepository.java src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/TransactionAnalysisJobRepository.java src/main/java/com/ssafy/amagetdone/domain/transaction/model/repository/TransactionAnalysisResultRepository.java src/main/java/com/ssafy/amagetdone/domain/budget/model/repository/EssentialExpenseSnapshotWeekRepository.java src/test/java/com/ssafy/amagetdone/domain/transaction/model/repository/TransactionRepositoryQueryTest.java
git commit -m "feat: add transaction analysis repository queries"
```

### Task 3: Add the merchant inference client with focused tests

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/MerchantInferenceApiClient.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/MerchantInferenceApiProperties.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/dto/MerchantInferenceDto.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/exception/TransactionErrorCode.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/MerchantInferenceApiClientTest.java`

**Step 1: Write the failing test**

Add a RestClient test similar to the existing SSAFY client tests. Verify:

- the client posts to `/inference/merchant/batch`
- the request body contains `merchant_texts`
- the response maps `merchant_text`, `normalized_merchant_text`, `raw_label`, `final_label`, `confidence`, `fallback_applied`, and `threshold_used`
- invalid payloads raise `TransactionException`

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.infra.client.MerchantInferenceApiClientTest"`

Expected: FAIL because the client and DTOs do not exist yet.

**Step 3: Write minimal implementation**

Implement a small transaction-domain client that wraps the batch inference endpoint and returns a typed result list.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.infra.client.MerchantInferenceApiClientTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/MerchantInferenceApiClient.java src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/MerchantInferenceApiProperties.java src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/dto/MerchantInferenceDto.java src/main/java/com/ssafy/amagetdone/domain/transaction/exception/TransactionErrorCode.java src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/MerchantInferenceApiClientTest.java
git commit -m "feat: add merchant inference client"
```

### Task 4: Implement synchronization decision logic against the existing transaction entities

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionSyncServiceTest.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionSyncService.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/SyncLatestTransactionsUseCase.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/support/util/TransactionDateRangeFactory.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/asset/model/repository/AccountRepository.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/asset/model/repository/CardRepository.java`

**Step 1: Write the failing test**

Add service tests for these cases:

- no local transaction exists, so the service seeds the requested window
- newest remote transaction equals newest local transaction, so the source is skipped
- newest remote transaction differs, so the service reloads from the newest local date through today
- demand deposit and card rows are inserted only when `transactionUniqueNo` does not already exist for the source

Example skeleton:

```java
@Test
void syncReloadsAccountTransactionsWhenNewestRemoteDoesNotMatchLocal() {
    given(accountTransactionOverrideRepository.findTopByAccount_IdOrderByTransactionDateTimeDesc("ACC-1"))
        .willReturn(Optional.of(existing));
    given(demandDepositApiClient.inquireTransactions(any())).willReturn(remoteResponse);

    service.syncLatest(userId, 6, false);

    verify(accountTransactionOverrideRepository).saveAll(anyList());
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncServiceTest"`

Expected: FAIL because the sync service and repository methods are incomplete.

**Step 3: Write minimal implementation**

Implement `TransactionSyncService` so it:

- resolves the authenticated user and linked accounts/cards
- checks the latest local row in the existing transaction tables
- calls the SSAFY APIs in descending order
- compares fingerprints
- inserts only missing rows into `AccountTransactionOverride` and `CardTransactionOverride`

Do not introduce a new raw transaction table or modify the current budget entities.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncServiceTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionSyncService.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/SyncLatestTransactionsUseCase.java src/main/java/com/ssafy/amagetdone/domain/transaction/support/util/TransactionDateRangeFactory.java src/main/java/com/ssafy/amagetdone/domain/asset/model/repository/AccountRepository.java src/main/java/com/ssafy/amagetdone/domain/asset/model/repository/CardRepository.java src/test/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionSyncServiceTest.java
git commit -m "feat: add transaction sync service"
```

### Task 5: Implement the async analysis pipeline and weekly essential snapshot builder

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionAnalysisServiceTest.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionAnalysisService.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/StartTransactionAnalysisJobUseCase.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/GetTransactionAnalysisJobUseCase.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/support/util/TransactionExclusionPolicy.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/support/util/FixedExpenseClassifier.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/support/util/WeeklyEssentialAllocator.java`

**Step 1: Write the failing test**

Add tests for:

- income-side demand deposit transactions are excluded
- transfers to service-registered accounts are excluded
- subscription labels are always `FIXED`
- 3-month recurring essential transactions become `FIXED`
- non-recurring essential transactions stay `VARIABLE`
- weekly snapshot totals equal the sum of category-level fixed and variable amounts
- duplicate running jobs are rejected

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionAnalysisServiceTest"`

Expected: FAIL because the pipeline services and policies do not exist yet.

**Step 3: Write minimal implementation**

Implement the orchestration so it:

- calls `TransactionSyncService`
- loads existing transaction rows for the requested months
- normalizes merchant text and calls `MerchantInferenceApiClient`
- stores `TransactionAnalysisResult`
- stores `EssentialExpenseSnapshotWeek` and `EssentialExpenseSnapshotWeekCategory`
- updates `TransactionAnalysisJob` status and progress

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionAnalysisServiceTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionAnalysisService.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/StartTransactionAnalysisJobUseCase.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/GetTransactionAnalysisJobUseCase.java src/main/java/com/ssafy/amagetdone/domain/transaction/support/util/TransactionExclusionPolicy.java src/main/java/com/ssafy/amagetdone/domain/transaction/support/util/FixedExpenseClassifier.java src/main/java/com/ssafy/amagetdone/domain/transaction/support/util/WeeklyEssentialAllocator.java src/test/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionAnalysisServiceTest.java
git commit -m "feat: add transaction analysis pipeline"
```

### Task 6: Expose transaction summary, monthly expense, list, and job-status APIs

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionQueryControllerTest.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionQueryController.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/ExpenseSummaryDto.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/MonthlyExpenseDto.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/TransactionListDto.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/TransactionAnalysisJobDto.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/GetExpenseSummaryUseCase.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/GetMonthlyExpenseUseCase.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/ListTransactionsUseCase.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionQueryService.java`

**Step 1: Write the failing test**

Use MockMvc tests to cover:

- `GET /transactions/expenses/summary`
- `GET /transactions/expenses/monthly`
- `GET /transactions`
- `POST /transactions/analysis-jobs`
- `GET /transactions/analysis-jobs/{jobId}`

Verify the controller:

- reads `Authorization`
- delegates to the right use case
- returns `ApiResponse.ok(...)`
- uses `202 Accepted` for job creation

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionQueryControllerTest"`

Expected: FAIL because the controller, DTOs, and query use cases do not exist yet.

**Step 3: Write minimal implementation**

Implement:

- controller endpoints without `/v1`
- DTOs that match the approved response shape
- query service methods for summary, monthly totals, transaction lists, and job-status reads
- job creation endpoint delegating to `StartTransactionAnalysisJobUseCase`

Follow the existing token-reader pattern from `MemberProfileController`.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionQueryControllerTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionQueryController.java src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/ExpenseSummaryDto.java src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/MonthlyExpenseDto.java src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/TransactionListDto.java src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/TransactionAnalysisJobDto.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/GetExpenseSummaryUseCase.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/GetMonthlyExpenseUseCase.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/ListTransactionsUseCase.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionQueryService.java src/test/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionQueryControllerTest.java
git commit -m "feat: add transaction query endpoints"
```

### Task 7: Expose weekly and monthly analysis read APIs

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionAnalysisReadControllerTest.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionAnalysisReadController.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/WeeklyTransactionAnalysisDto.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/MonthlyTransactionAnalysisDto.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/GetWeeklyTransactionAnalysisUseCase.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/GetMonthlyTransactionAnalysisUseCase.java`

**Step 1: Write the failing test**

Add controller tests for:

- `GET /transactions/analysis/weeks/{yearWeek}`
- `GET /transactions/analysis/months/{yearMonth}`

Verify that:

- weekly responses contain expense amount, essential amount, fixed and variable breakdown, and category expenses
- monthly responses contain monthly totals and the monthly essential amount derived from weekly snapshot data

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionAnalysisReadControllerTest"`

Expected: FAIL because the controller and DTOs do not exist yet.

**Step 3: Write minimal implementation**

Implement read-side queries that join transaction analysis results with weekly essential snapshots and map them into weekly and monthly analysis DTOs.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionAnalysisReadControllerTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionAnalysisReadController.java src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/WeeklyTransactionAnalysisDto.java src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/MonthlyTransactionAnalysisDto.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/GetWeeklyTransactionAnalysisUseCase.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/GetMonthlyTransactionAnalysisUseCase.java src/test/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionAnalysisReadControllerTest.java
git commit -m "feat: add transaction analysis read endpoints"
```

### Task 8: Expose the weekly essential-expense budget endpoint and verify the integrated surface

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/budget/api/controller/BudgetEssentialExpenseControllerTest.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/api/controller/BudgetEssentialExpenseController.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/api/dto/WeeklyEssentialExpenseDto.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/application/usecase/GetWeeklyEssentialExpensesUseCase.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/budget/application/service/EssentialExpenseSnapshotQueryService.java`

**Step 1: Write the failing test**

Add MockMvc tests for `GET /budgets/weeks/{yearWeek}/essential-expenses` that verify:

- the response contains `essentialAmount`
- the response contains `fixedExpenseCategories`
- the response contains `variableExpenseCategories`
- the endpoint does not return saving-budget or free-spendable fields

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.budget.api.controller.BudgetEssentialExpenseControllerTest"`

Expected: FAIL because the controller, DTO, and query service do not exist yet.

**Step 3: Write minimal implementation**

Implement the budget-facing essential-expense query by reading `EssentialExpenseSnapshotWeek` and `EssentialExpenseSnapshotWeekCategory`. Do not change any existing budget entity or add the broader `/budgets/weeks/{yearWeek}/setup` facade in this task.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.budget.api.controller.BudgetEssentialExpenseControllerTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/budget/api/controller/BudgetEssentialExpenseController.java src/main/java/com/ssafy/amagetdone/domain/budget/api/dto/WeeklyEssentialExpenseDto.java src/main/java/com/ssafy/amagetdone/domain/budget/application/usecase/GetWeeklyEssentialExpensesUseCase.java src/main/java/com/ssafy/amagetdone/domain/budget/application/service/EssentialExpenseSnapshotQueryService.java src/test/java/com/ssafy/amagetdone/domain/budget/api/controller/BudgetEssentialExpenseControllerTest.java
git commit -m "feat: add weekly essential expense endpoint"
```

### Task 9: Run the focused regression suite and document the integration boundary

**Files:**
- Modify: `docs/plans/2026-03-13-transaction-analysis-design.md`
- Modify: `README.md`

**Step 1: Write the failing test**

Add any final assertion gaps discovered while integrating controller paths or repository queries. If no extra assertion is needed, add a short documentation update that states `/budgets/weeks/{yearWeek}/setup` remains an integration facade owned jointly with another teammate.

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionQueryControllerTest" --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionAnalysisReadControllerTest" --tests "com.ssafy.amagetdone.domain.budget.api.controller.BudgetEssentialExpenseControllerTest" --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncServiceTest" --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionAnalysisServiceTest"`

Expected: PASS if no extra assertion was added, otherwise FAIL on the new assertion until the last adjustment is made.

**Step 3: Write minimal implementation**

Fix the last integration issue if one appears. Otherwise, only update docs to record the interface boundary with the other teammate.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionQueryControllerTest" --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionAnalysisReadControllerTest" --tests "com.ssafy.amagetdone.domain.budget.api.controller.BudgetEssentialExpenseControllerTest" --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncServiceTest" --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionAnalysisServiceTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add README.md docs/plans/2026-03-13-transaction-analysis-design.md
git commit -m "docs: record transaction analysis integration boundary"
```

**Out of Scope for This Plan**

- `GET /budgets/weeks/{yearWeek}/setup`
- saving-budget recommendation logic
- free-spendable amount logic
- modifications to existing budget tables
