# Account OpenAPI Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the account domain for SSAFY demand-deposit OpenAPI, including admin product/account creation APIs, user account sync/list APIs, and account operation APIs.

**Architecture:** Replace the leftover `asset` implementation under `domain/account` with real `account` packages, add dedicated demand-deposit OpenAPI clients, persist synced account metadata locally, and reuse existing shared SSAFY infrastructure and transaction-history client patterns.

**Tech Stack:** Spring Boot, Spring Web, Spring Data JPA, RestClient, JUnit 5, Mockito

---

## Chunk 1: Foundation Cleanup

### Task 1: Document and branch setup

**Files:**
- Create: `docs/superpowers/specs/2026-03-13-account-openapi-design.md`
- Create: `docs/superpowers/plans/2026-03-13-account-openapi.md`
- Modify: git branch metadata only

- [ ] **Step 1: Create the feature branch in the current workspace**

Run: `git switch -c feat/back/account-openapi`
Expected: branch switches successfully while preserving current account migration changes

- [ ] **Step 2: Confirm account migration changes are still present**

Run: `git status --short`
Expected: deleted `domain/asset` files and new `domain/account` files remain visible

### Task 2: Rename asset leftovers to account

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/exception/AssetErrorCode.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/exception/AssetException.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/model/entity/Account.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/model/repository/AccountRepository.java`
- Modify: `src/main/java/com/ssafy/amagetdone/global/exception/GlobalExceptionHandler.java`
- Delete or replace: irrelevant merchant/asset client leftovers under `domain/account/infra/client`

- [ ] **Step 1: Add a failing compile-oriented test or build check target**

Run: `./gradlew test --tests '*Account*'`
Expected: FAIL because account implementation/tests do not exist yet

- [ ] **Step 2: Rename account exceptions and package declarations**

Implement the minimal account exception types and package declarations needed for compilation.

- [ ] **Step 3: Re-run focused tests/build check**

Run: `./gradlew compileJava`
Expected: compilation moves forward past account package naming errors

## Chunk 2: Account OpenAPI Clients

### Task 3: Create demand-deposit DTOs and properties

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/infra/client/AccountSsafyApiProperties.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/infra/client/dto/DemandDepositProductDto.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/infra/client/dto/DemandDepositAccountDto.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/infra/client/dto/DemandDepositBalanceDto.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/infra/client/dto/DemandDepositTransferDto.java`

- [ ] **Step 1: Write failing DTO/client tests**

Create narrow tests that assert mapping/validation expectations for one client at a time.

- [ ] **Step 2: Run the failing tests**

Run: `./gradlew test --tests '*Account*Client*'`
Expected: FAIL because the DTOs/clients do not exist yet

- [ ] **Step 3: Implement DTOs/properties**

Write only the request/response records needed by the selected APIs.

- [ ] **Step 4: Re-run tests**

Run: `./gradlew test --tests '*Account*Client*'`
Expected: tests still fail later on missing client behavior, not missing DTO classes

### Task 4: Implement account OpenAPI clients

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyDemandDepositProductApiClient.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyDemandDepositAccountApiClient.java`

- [ ] **Step 1: Write a failing test for one client behavior**

Start with configuration/request validation for the product registration client.

- [ ] **Step 2: Run the test and verify the expected failure**

Run: `./gradlew test --tests '*SsafyDemandDepositProductApiClientTest'`
Expected: FAIL for missing client implementation

- [ ] **Step 3: Implement the minimal client**

Follow the existing transaction client pattern using `SsafyApiRequestFactory` and `SsafyApiResponseValidator`.

- [ ] **Step 4: Repeat for account create/list/balance/deposit/transfer behaviors**

Run focused tests after each behavior.

## Chunk 3: Application and Persistence

### Task 5: Implement local account persistence and sync

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/model/entity/Account.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/model/repository/AccountRepository.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/SyncAccountsUseCase.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/ListAccountsUseCase.java`

- [ ] **Step 1: Write a failing sync use case test**

Cover: OpenAPI list response is converted into local rows for one authenticated user.

- [ ] **Step 2: Run the test**

Run: `./gradlew test --tests '*SyncAccountsUseCaseTest'`
Expected: FAIL for missing use case/entity behavior

- [ ] **Step 3: Implement the minimal sync path**

Add account create/update helpers and repository queries.

- [ ] **Step 4: Run the sync tests**

Run: `./gradlew test --tests '*SyncAccountsUseCaseTest'`
Expected: PASS

### Task 6: Implement account service orchestration

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/application/service/AccountService.java`
- Create: use case classes for product registration, product list, account create, balance, deposit, transfer, transactions

- [ ] **Step 1: Write a failing service test for one user flow**

Start with `syncAndListAccounts`.

- [ ] **Step 2: Run the test**

Run: `./gradlew test --tests '*AccountServiceTest'`
Expected: FAIL

- [ ] **Step 3: Implement the minimal orchestration**

Wire service methods to use cases and existing auth/user lookup patterns.

- [ ] **Step 4: Expand tests for balance/deposit/transfer/transactions**

Run focused tests after each addition.

## Chunk 4: HTTP API Surface

### Task 7: Add admin and user controllers plus request/response DTOs

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/api/controller/AccountAdminController.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/api/controller/AccountController.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/api/dto/AccountAdminDto.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/api/dto/AccountDto.java`

- [ ] **Step 1: Write a failing controller test**

Cover one admin endpoint and one authenticated user endpoint.

- [ ] **Step 2: Run the test**

Run: `./gradlew test --tests '*AccountControllerTest'`
Expected: FAIL

- [ ] **Step 3: Implement controller endpoints**

Use `ApiResponse.ok(...)`, `JwtAccessTokenReader`, and Swagger tags/operations to keep the API surface clear.

- [ ] **Step 4: Re-run controller tests**

Run: `./gradlew test --tests '*AccountControllerTest'`
Expected: PASS

## Chunk 5: Verification

### Task 8: Run focused verification

**Files:**
- No code changes required unless verification fails

- [ ] **Step 1: Run targeted account tests**

Run: `./gradlew test --tests '*Account*' --tests '*DemandDeposit*'`
Expected: all newly added account-focused tests pass

- [ ] **Step 2: Run a full backend compile check**

Run: `./gradlew compileJava`
Expected: PASS

- [ ] **Step 3: Inspect git diff**

Run: `git status --short`
Expected: only intended account/domain/docs changes remain
