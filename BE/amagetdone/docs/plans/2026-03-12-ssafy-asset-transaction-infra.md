# SSAFY Asset/Transaction Infra Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add SSAFY Open API clients under `asset.infra` and `transaction.infra` for merchant list, card transaction list, and demand deposit transaction list.

**Architecture:** Keep each domain independent. `asset` owns its merchant client and config, `transaction` owns its card and demand-deposit clients and config. Clients return domain-oriented records, convert external failures into domain exceptions, and are verified with `MockRestServiceServer`.

**Tech Stack:** Spring Boot 3.5, `RestClient`, `@ConfigurationProperties`, JUnit 5, Spring Test `MockRestServiceServer`

---

### Task 1: Add Asset Merchant Client Test

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantApiClientTest.java`

**Step 1: Write the failing test**

Add tests for:
- successful `inquireMerchantList` mapping
- non-`H0000` header response failure
- transport exception failure

Use `MockRestServiceServer.bindTo(RestClient.builder())`.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.ssafy.amagetdone.domain.asset.infra.client.SsafyMerchantApiClientTest`

Expected: FAIL because `SsafyMerchantApiClient` and related types do not exist yet.

**Step 3: Write minimal implementation**

Create:
- `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/AssetSsafyApiProperties.java`
- `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantApiClient.java`

Implement:
- `@Component` + `@ConfigurationProperties(prefix = "asset.ssafy")` properties
- `RestClient` based POST call to `/creditCard/inquireMerchantList`
- private header builder
- response header success check
- mapping to domain record return types

Also extend:
- `src/main/java/com/ssafy/amagetdone/domain/asset/exception/AssetErrorCode.java`

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.ssafy.amagetdone.domain.asset.infra.client.SsafyMerchantApiClientTest`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/asset/infra/client src/main/java/com/ssafy/amagetdone/domain/asset/exception/AssetErrorCode.java src/test/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantApiClientTest.java
git commit -m "feat: add SSAFY merchant infra client"
```

### Task 2: Add Card Transaction Client Test

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionApiClientTest.java`

**Step 1: Write the failing test**

Add tests for:
- successful card transaction list mapping
- SSAFY business failure when `responseCode` is not `H0000`
- transport exception conversion

Verify mapping of:
- top-level card info
- one transaction item including `merchantId`, `cardStatus`, `billStatementsYn`, `billStatementsStatus`

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyCardTransactionApiClientTest`

Expected: FAIL because transaction client and exception classes do not exist yet.

**Step 3: Write minimal implementation**

Create:
- `src/main/java/com/ssafy/amagetdone/domain/transaction/exception/TransactionErrorCode.java`
- `src/main/java/com/ssafy/amagetdone/domain/transaction/exception/TransactionException.java`
- `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/TransactionSsafyApiProperties.java`
- `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionApiClient.java`

Implement:
- request record with `userKey`, `cardNo`, `cvc`, `startDate`, `endDate`
- POST call to `/creditCard/inquireCreditCardTransactionList`
- response header validation
- mapping to domain result records

Update:
- `src/main/java/com/ssafy/amagetdone/global/exception/GlobalExceptionHandler.java`

Add handler for `TransactionException`.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyCardTransactionApiClientTest`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/transaction/exception src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client src/main/java/com/ssafy/amagetdone/global/exception/GlobalExceptionHandler.java src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionApiClientTest.java
git commit -m "feat: add SSAFY card transaction infra client"
```

### Task 3: Add Demand Deposit Transaction Client Test

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionApiClientTest.java`

**Step 1: Write the failing test**

Add tests for:
- successful demand deposit transaction list mapping
- business failure when header code is not `H0000`
- malformed response failure when `REC` or `list` is missing

Verify mapping of:
- `totalCount`
- first transaction item including `transactionType`, `transactionAfterBalance`, `transactionSummary`

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClientTest`

Expected: FAIL because the demand deposit client does not exist yet.

**Step 3: Write minimal implementation**

Create:
- `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionApiClient.java`

Implement:
- request record with `userKey`, `accountNo`, `startDate`, `endDate`, `transactionType`, `orderByType`
- POST call to `/demandDeposit/inquireTransactionHistoryList`
- response validation and mapping

Reuse:
- `TransactionSsafyApiProperties`
- `TransactionException`

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClientTest`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionApiClient.java src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionApiClientTest.java
git commit -m "feat: add SSAFY demand deposit transaction client"
```

### Task 4: Wire Configuration Properties

**Files:**
- Modify: `src/main/resources/application.properties`

**Step 1: Write the failing test**

Add or extend one client test to instantiate properties from Spring context if needed, or add a focused properties binding test:

- Create: `src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyApiPropertiesBindingTest.java`

Test that `AssetSsafyApiProperties` and `TransactionSsafyApiProperties` are registered beans when Spring starts.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyApiPropertiesBindingTest`

Expected: FAIL if properties are not registered or placeholders are missing.

**Step 3: Write minimal implementation**

Add property placeholders:
- `asset.ssafy.base-url=${ASSET_SSAFY_BASE_URL:...}`
- `asset.ssafy.institution-code=${ASSET_SSAFY_INSTITUTION_CODE:00100}`
- `asset.ssafy.fintech-app-no=${ASSET_SSAFY_FINTECH_APP_NO:001}`
- `asset.ssafy.api-key=${ASSET_SSAFY_API_KEY:}`
- `transaction.ssafy.base-url=${TRANSACTION_SSAFY_BASE_URL:...}`
- `transaction.ssafy.institution-code=${TRANSACTION_SSAFY_INSTITUTION_CODE:00100}`
- `transaction.ssafy.fintech-app-no=${TRANSACTION_SSAFY_FINTECH_APP_NO:001}`
- `transaction.ssafy.api-key=${TRANSACTION_SSAFY_API_KEY:}`

Use the same SSAFY base host path in both defaults.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyApiPropertiesBindingTest`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/resources/application.properties src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyApiPropertiesBindingTest.java
git commit -m "chore: add SSAFY infra configuration properties"
```

### Task 5: Run Focused Verification

**Files:**
- No new files

**Step 1: Run focused tests**

Run:
- `./gradlew test --tests com.ssafy.amagetdone.domain.asset.infra.client.SsafyMerchantApiClientTest`
- `./gradlew test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyCardTransactionApiClientTest`
- `./gradlew test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClientTest`
- `./gradlew test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyApiPropertiesBindingTest`

Expected: PASS

**Step 2: Run broader regression slice**

Run:
- `./gradlew test --tests com.ssafy.amagetdone.domain.asset.AssetEntityMappingTest`
- `./gradlew test --tests com.ssafy.amagetdone.domain.transaction.TransactionEntityMappingTest`
- `./gradlew test --tests com.ssafy.amagetdone.domain.transaction.FkRelationshipMappingTest`

Expected: PASS

**Step 3: Review git diff**

Run: `git diff --stat`

Expected: only asset/transaction infra, exceptions, properties, tests, and docs changes.

**Step 4: Commit**

```bash
git add src/main/java src/main/resources/application.properties src/test/java
git commit -m "test: verify SSAFY infra clients"
```
