# SSAFY Data Init Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build `/v1/admin/accounts/data-init` so an authenticated admin can seed one SSAFY account, one linked SSAFY card, and preset account/card transactions for a target user while persisting the local account, card, and transaction override records immediately.

**Architecture:** Keep the new entry point inside the existing account admin layer. Add one orchestration use case plus admin-only SSAFY clients that accept runtime `apiKey` and `userApiKey`, then persist local entities directly from SSAFY create responses instead of waiting for the sync service.

**Tech Stack:** Spring Boot, JPA, RestClient, Mockito, JUnit 5

---

### Task 1: Add the Admin Endpoint Contract

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/api/controller/AccountAdminController.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/api/dto/AccountAdminDto.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/application/service/AccountService.java`
- Test: `src/test/java/com/ssafy/amagetdone/domain/account/api/controller/AccountAdminControllerTest.java`

**Step 1: Write the failing controller test**

Add a test that calls `controller.initializeDummyData(request)` and verifies:
- `AccountService.initializeDummyData(request)` is invoked
- the response is wrapped in `ApiResponse.ok(...)`
- the DTO returned by the service is preserved

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.api.controller.AccountAdminControllerTest"`

Expected: FAIL because the DTO, service method, or controller endpoint does not exist yet.

**Step 3: Write minimal contract code**

Add to `AccountAdminDto`:
- `DataInitRequest(String apiKey, String userApiKey)`
- `DataInitResponse(...)`

Add to `AccountService`:
- `initializeDummyData(AccountAdminDto.DataInitRequest request)`

Add to `AccountAdminController`:
- `@PostMapping("/data-init")`
- `initializeDummyData(@RequestBody AccountAdminDto.DataInitRequest request)`

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.api.controller.AccountAdminControllerTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/api/controller/AccountAdminController.java src/main/java/com/ssafy/amagetdone/domain/account/api/dto/AccountAdminDto.java src/main/java/com/ssafy/amagetdone/domain/account/application/service/AccountService.java src/test/java/com/ssafy/amagetdone/domain/account/api/controller/AccountAdminControllerTest.java
git commit -m "test: add admin data init contract"
```

### Task 2: Add User Lookup and Error Codes for Admin Initialization

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/FindAccountUserByUserKeyUseCase.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/member/model/repository/UserRepository.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/exception/AccountErrorCode.java`
- Test: `src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/FindAccountUserByUserKeyUseCaseTest.java`

**Step 1: Write the failing lookup tests**

Cover:
- finds a user by `userKey`
- throws `USER_NOT_FOUND` when no user matches
- throws `USER_KEY_REQUIRED` when input is blank

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.application.usecase.FindAccountUserByUserKeyUseCaseTest"`

Expected: FAIL because the use case and repository query do not exist.

**Step 3: Write minimal implementation**

Add:
- `UserRepository.findByUserKey(String userKey)`
- `FindAccountUserByUserKeyUseCase.execute(String userKey)`
- any missing account admin initialization error codes that are needed now, such as:
  - invalid data-init request
  - card identifier type mismatch if needed later

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.application.usecase.FindAccountUserByUserKeyUseCaseTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/FindAccountUserByUserKeyUseCase.java src/main/java/com/ssafy/amagetdone/domain/member/model/repository/UserRepository.java src/main/java/com/ssafy/amagetdone/domain/account/exception/AccountErrorCode.java src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/FindAccountUserByUserKeyUseCaseTest.java
git commit -m "test: add user-key lookup for admin init"
```

### Task 3: Introduce Admin SSAFY Demand-Deposit Client

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyAdminDemandDepositApiClient.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/infra/client/dto/AdminDemandDepositInitDto.java`
- Test: `src/test/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyAdminDemandDepositApiClientTest.java`

**Step 1: Write the failing client tests**

Cover:
- `createAccount(apiKey, userKey, accountTypeUniqueNo)`
- `deposit(apiKey, userKey, accountNo, amount, summary)`
- `transfer(apiKey, userKey, withdrawalAccountNo, depositAccountNo, amount, summaries)`
- invalid blank keys fail before outbound call

Mock `RestClient` interactions the same way existing client tests in the repo do, or add focused unit tests around request validation if there is no established client test harness.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminDemandDepositApiClientTest"`

Expected: FAIL because the new client and DTOs do not exist.

**Step 3: Write minimal implementation**

Implement admin-only methods that:
- accept runtime `apiKey`
- use `SsafyApiRequestFactory.create(...)`
- call:
  - `createDemandDepositAccount`
  - `updateDemandDepositAccountDeposit`
  - `updateDemandDepositAccountTransfer`
- map create/deposit/transfer responses into typed DTOs

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminDemandDepositApiClientTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyAdminDemandDepositApiClient.java src/main/java/com/ssafy/amagetdone/domain/account/infra/client/dto/AdminDemandDepositInitDto.java src/test/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyAdminDemandDepositApiClientTest.java
git commit -m "test: add admin demand deposit ssafy client"
```

### Task 4: Introduce Admin SSAFY Credit-Card Client

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyAdminCreditCardApiClient.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/infra/client/dto/AdminCreditCardInitDto.java`
- Test: `src/test/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyAdminCreditCardApiClientTest.java`

**Step 1: Write the failing client tests**

Cover:
- `createCard(apiKey, userKey, cardUniqueNo, withdrawalAccountNo, withdrawalDate)`
- `createCardTransaction(apiKey, userKey, cardNo, cvc, merchantId, paymentBalance)`
- blank keys fail fast
- malformed response handling maps to account error codes

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminCreditCardApiClientTest"`

Expected: FAIL because the client and DTOs do not exist.

**Step 3: Write minimal implementation**

Implement admin-only calls for:
- `createCreditCard`
- `createCreditCardTransaction`

Map:
- card metadata
- card transaction metadata

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminCreditCardApiClientTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyAdminCreditCardApiClient.java src/main/java/com/ssafy/amagetdone/domain/account/infra/client/dto/AdminCreditCardInitDto.java src/test/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyAdminCreditCardApiClientTest.java
git commit -m "test: add admin credit card ssafy client"
```

### Task 5: Resolve Local Card Identifier Mapping

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/model/entity/Card.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/model/repository/CardRepository.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionSyncService.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/model/entity/CardTransactionOverride.java`
- Test: `src/test/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionSyncServiceTest.java`
- Test: any card entity mapping test if required

**Step 1: Write the failing test that locks the desired identifier type**

Cover whichever contract is correct after implementation:
- local `Card` stores SSAFY `cardUniqueNo` losslessly
- card transaction persistence and sync still work with that identifier

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncServiceTest"`

Expected: FAIL once the test asserts the new identifier contract.

**Step 3: Write minimal implementation**

If SSAFY `cardUniqueNo` is truly string-based, align:
- `Card.cardUniqueNo`
- repository query signatures
- foreign-key references in `CardTransactionOverride`
- transaction sync repository usages

If an existing numeric contract is proven valid, document that in code comments or DTO mapping and keep the entity unchanged.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncServiceTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/model/entity/Card.java src/main/java/com/ssafy/amagetdone/domain/account/model/repository/CardRepository.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionSyncService.java src/main/java/com/ssafy/amagetdone/domain/transaction/model/entity/CardTransactionOverride.java src/test/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionSyncServiceTest.java
git commit -m "refactor: align local card identifier mapping"
```

### Task 6: Implement the Data-Init Orchestration Use Case

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeSsafyDummyDataUseCase.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/application/service/AccountService.java`
- Test: `src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeSsafyDummyDataUseCaseTest.java`

**Step 1: Write the failing orchestration tests**

Cover:
- happy path order:
  1. find user
  2. create primary account
  3. persist primary account
  4. create helper account without local persistence
  5. seed account transactions
  6. create card from primary account
  7. persist card
  8. seed card transactions
  9. persist local override rows
- intermediate failure stops later steps
- response includes account/card/transaction counts

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.application.usecase.InitializeSsafyDummyDataUseCaseTest"`

Expected: FAIL because the orchestration use case does not exist.

**Step 3: Write minimal implementation**

Implement:
- preset constants for:
  - primary demand-deposit product ID
  - helper demand-deposit product ID if reused
  - card product ID
  - withdrawal date
  - account transaction amounts/summaries
  - merchant IDs/payment amounts
- transactional orchestration
- thin delegation from `AccountService`

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.application.usecase.InitializeSsafyDummyDataUseCaseTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeSsafyDummyDataUseCase.java src/main/java/com/ssafy/amagetdone/domain/account/application/service/AccountService.java src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeSsafyDummyDataUseCaseTest.java
git commit -m "feat: orchestrate ssafy admin data initialization"
```

### Task 7: Persist Local Account, Card, and Transaction Overrides

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeSsafyDummyDataUseCase.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/model/repository/AccountRepository.java` if helper lookup helpers are needed
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/model/repository/MerchantRepository.java` only if missing query helpers are needed
- Test: `src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeSsafyDummyDataUseCaseTest.java`

**Step 1: Write the failing persistence assertions**

Add assertions for:
- primary account saved exactly once
- helper account never saved
- card saved with the newly created account as withdrawal account
- account transactions saved with default category and expected balances
- card transactions saved with resolved merchants and expected amounts

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.application.usecase.InitializeSsafyDummyDataUseCaseTest"`

Expected: FAIL because persistence mapping is still incomplete.

**Step 3: Write minimal implementation**

Implement local mapping helpers inside the use case or a dedicated mapper method set:
- create/sync `Account`
- create `Card`
- map deposit/transfer responses to `AccountTransactionOverride`
- map card transaction responses to `CardTransactionOverride`
- resolve merchant rows from existing preset merchant IDs

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.application.usecase.InitializeSsafyDummyDataUseCaseTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeSsafyDummyDataUseCase.java src/main/java/com/ssafy/amagetdone/domain/account/model/repository/AccountRepository.java src/main/java/com/ssafy/amagetdone/domain/account/model/repository/MerchantRepository.java src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeSsafyDummyDataUseCaseTest.java
git commit -m "feat: persist local data for admin init flow"
```

### Task 8: Run Focused Verification and Update Docs

**Files:**
- Modify: `docs/plans/2026-03-17-ssafy-data-init-design.md` only if implementation changed the approved design
- Modify: `docs/plans/2026-03-17-ssafy-data-init.md` only if task paths changed materially

**Step 1: Run focused tests**

Run:
- `./gradlew test --tests "com.ssafy.amagetdone.domain.account.api.controller.AccountAdminControllerTest"`
- `./gradlew test --tests "com.ssafy.amagetdone.domain.account.application.usecase.FindAccountUserByUserKeyUseCaseTest"`
- `./gradlew test --tests "com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminDemandDepositApiClientTest"`
- `./gradlew test --tests "com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminCreditCardApiClientTest"`
- `./gradlew test --tests "com.ssafy.amagetdone.domain.account.application.usecase.InitializeSsafyDummyDataUseCaseTest"`
- `./gradlew test --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncServiceTest"`

Expected: PASS

**Step 2: Run a broader regression slice**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.*" --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncServiceTest"`

Expected: PASS

**Step 3: Update docs only if reality drifted**

If any file path, identifier type decision, or preset behavior changed, update the design/plan docs to match the implemented state.

**Step 4: Commit**

```bash
git add docs/plans/2026-03-17-ssafy-data-init-design.md docs/plans/2026-03-17-ssafy-data-init.md
git commit -m "docs: finalize ssafy data init plan"
```
