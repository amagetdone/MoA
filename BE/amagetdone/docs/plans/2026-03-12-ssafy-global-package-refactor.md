# SSAFY Global Package Refactor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** `global.infra.ssafy` 하위 클래스를 역할별 패키지로 재배치하고 관련 import와 테스트를 정리한다.

**Architecture:** 기능은 유지하고 package 선언과 import만 조정한다. `model`, `envelope`, `support`, `exception` 하위 패키지로 분리하고, domain client와 테스트는 새 경로를 참조한다.

**Tech Stack:** Java 17, Spring Boot, Jackson, JUnit 5, Gradle

---

### Task 1: Update Structure Tests First

**Files:**
- Modify: `src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyInfraClientStructureTest.java`

**Step 1: Write the failing structure assertions**

구조 테스트가 아래 파일 존재를 검증하게 바꾼다.

- `global/infra/ssafy/model/...`
- `global/infra/ssafy/envelope/...`
- `global/infra/ssafy/support/...`
- `global/infra/ssafy/exception/...`

**Step 2: Run test to verify it fails**

Run:

```bash
./gradlew.bat test --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyInfraClientStructureTest
```

Expected: FAIL because files are still in a flat package.

**Step 3: Commit**

```bash
git add src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyInfraClientStructureTest.java
git commit -m "test: require ssafy global package structure"
```

### Task 2: Move Global SSAFY Classes By Role

**Files:**
- Move/modify:
  - `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyRequestHeader.java`
  - `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyResponseHeader.java`
  - `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyRequest.java`
  - `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyResponse.java`
  - `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyRequestEnvelope.java`
  - `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyResponseEnvelope.java`
  - `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyApiRequestFactory.java`
  - `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyApiResponseValidator.java`
  - `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyApiException.java`

**Step 1: Change package declarations**

- `model`
- `envelope`
- `support`
- `exception`

**Step 2: Fix imports between moved classes**

**Step 3: Run focused tests**

Run:

```bash
./gradlew.bat test --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiRequestFactoryTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiResponseValidatorTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyEnvelopeSerializationTest
```

Expected: FAIL first on stale imports, then PASS after fixing imports and test packages.

**Step 4: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/global/infra/ssafy src/test/java/com/ssafy/amagetdone/global/infra/ssafy
git commit -m "refactor: split ssafy global package by role"
```

### Task 3: Update Domain Client Imports

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantApiClient.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionApiClient.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionApiClient.java`

**Step 1: Replace old flat-package imports with new role-based imports**

**Step 2: Run client tests**

Run:

```bash
./gradlew.bat test --tests com.ssafy.amagetdone.domain.asset.infra.client.SsafyMerchantApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyCardTransactionApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClientTest
```

Expected: PASS

**Step 3: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/asset/infra/client src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client
git commit -m "refactor: update clients for ssafy subpackages"
```

### Task 4: Final Regression Verification

**Files:**
- Modify if needed: `src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyInfraClientStructureTest.java`
- Modify if needed: `src/test/java/com/ssafy/amagetdone/global/infra/ssafy/...`

**Step 1: Run regression suite**

Run:

```bash
./gradlew.bat test --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiRequestFactoryTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiResponseValidatorTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyEnvelopeSerializationTest --tests com.ssafy.amagetdone.domain.asset.infra.client.SsafyMerchantApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyCardTransactionApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyInfraClientStructureTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyApiPropertiesConfigurationTest
```

Expected: PASS

**Step 2: Run clean regression**

Run:

```bash
./gradlew.bat clean test --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiRequestFactoryTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyApiResponseValidatorTest --tests com.ssafy.amagetdone.global.infra.ssafy.SsafyEnvelopeSerializationTest --tests com.ssafy.amagetdone.domain.asset.infra.client.SsafyMerchantApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyCardTransactionApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClientTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyInfraClientStructureTest --tests com.ssafy.amagetdone.domain.transaction.infra.client.SsafyApiPropertiesConfigurationTest
```

Expected: PASS

**Step 3: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/global/infra/ssafy src/main/java/com/ssafy/amagetdone/domain/asset/infra/client src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client src/test/java/com/ssafy/amagetdone/global/infra/ssafy src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client
git commit -m "refactor: reorganize ssafy global packages"
```
