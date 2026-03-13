# Account One-Won Verification Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add SSAFY one-won account verification with a user request API, admin check API, admin status lookup API, and DB-backed verification history in a separate `domain/accountverification`.

**Architecture:** Add a focused `accountverification` domain that owns the verification flow, persistence, and SSAFY account-auth integration. Keep `domain/account` responsible for account ownership data and update `Account.verified` when verification succeeds.

**Tech Stack:** Spring Boot, Spring Web, Spring Data JPA, RestClient, JUnit 5, Mockito, AssertJ

---

## Chunk 1: API surface and persistence skeleton

### Task 1: Add failing tests for accountverification DTO/controller/service entry points

**Files:**
- Create: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/accountverification/api/controller/AccountVerificationControllerTest.java`
- Create: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/accountverification/api/controller/AccountVerificationAdminControllerTest.java`
- Create: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/accountverification/application/service/AccountVerificationServiceTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void requestVerificationReadsUserIdFromAuthorizationHeader() {
    // controller delegates to service
}

@Test
void checkVerificationDelegatesToAdminServiceMethod() {
    // admin controller delegates to service
}

@Test
void accountVerificationServiceDelegatesVerificationMethodsToUseCases() {
    // account verification service delegates to request/check/status use cases
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests '*AccountVerificationControllerTest' --tests '*AccountVerificationAdminControllerTest' --tests '*AccountVerificationServiceTest'`
Expected: FAIL because verification DTOs and methods do not exist yet

- [ ] **Step 3: Write minimal implementation**

```java
// add accountverification DTO records
// add accountverification controllers
// add AccountVerificationService delegation methods
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests '*AccountVerificationControllerTest' --tests '*AccountVerificationAdminControllerTest' --tests '*AccountVerificationServiceTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/accountverification \
        BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/accountverification
git commit -m "feat: add accountverification api surface"
```

### Task 2: Add failing persistence tests for verification history

**Files:**
- Modify: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/ErdSchemaAlignmentTest.java`
- Create: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/accountverification/model/entity/AccountVerification.java`
- Create: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/accountverification/model/entity/AccountVerificationStatus.java`
- Create: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/accountverification/model/repository/AccountVerificationRepository.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void accountVerificationDeclaresExpectedDefaults() throws Exception {
    assertColumnDefault(AccountVerification.class, "status", "'REQUESTED'");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests '*ErdSchemaAlignmentTest'`
Expected: FAIL because `AccountVerification` mapping does not exist yet

- [ ] **Step 3: Write minimal implementation**

```java
@Entity
class AccountVerification extends BaseEntity { ... }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests '*ErdSchemaAlignmentTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/accountverification/model \
        BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/ErdSchemaAlignmentTest.java
git commit -m "feat: add accountverification persistence"
```

## Chunk 2: Verification request and admin check behavior

### Task 3: Add failing use case tests for request/check flows

**Files:**
- Create: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/accountverification/application/usecase/RequestAccountVerificationUseCaseTest.java`
- Create: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/accountverification/application/usecase/CheckAccountVerificationUseCaseTest.java`
- Create: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/accountverification/application/usecase/GetAccountVerificationStatusUseCaseTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void requestVerificationStoresRequestedHistory() { }

@Test
void checkVerificationMarksHistorySuccessAndAccountVerified() { }

@Test
void checkVerificationMarksHistoryFailedWhenCodeMismatch() { }

@Test
void statusLookupReturnsLatestVerificationRecord() { }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests '*RequestAccountVerificationUseCaseTest' --tests '*CheckAccountVerificationUseCaseTest' --tests '*GetAccountVerificationStatusUseCaseTest'`
Expected: FAIL because use cases do not exist yet

- [ ] **Step 3: Write minimal implementation**

```java
@Component
class RequestAccountVerificationUseCase { ... }

@Component
class CheckAccountVerificationUseCase { ... }

@Component
class GetAccountVerificationStatusUseCase { ... }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests '*RequestAccountVerificationUseCaseTest' --tests '*CheckAccountVerificationUseCaseTest' --tests '*GetAccountVerificationStatusUseCaseTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/accountverification/application \
        BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/accountverification/application/usecase
git commit -m "feat: add accountverification use cases"
```

### Task 4: Add failing infra tests for SSAFY account auth client support

**Files:**
- Create: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/accountverification/infra/client/SsafyAccountVerificationApiClientTest.java`
- Create: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/accountverification/infra/client/SsafyAccountVerificationApiClient.java`
- Create: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/accountverification/infra/client/AccountVerificationSsafyApiProperties.java`
- Create: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/accountverification/infra/client/dto/AccountVerificationClientDto.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void requestVerificationCallsOpenAccountAuthEndpoint() { }

@Test
void checkVerificationCallsCheckAuthCodeEndpoint() { }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests '*SsafyAccountVerificationApiClientTest'`
Expected: FAIL because account auth client methods do not exist yet

- [ ] **Step 3: Write minimal implementation**

```java
public AccountVerificationClientDto.RequestVerificationResponse requestVerification(...) { ... }
public AccountVerificationClientDto.CheckVerificationResponse checkVerification(...) { ... }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests '*SsafyAccountVerificationApiClientTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/accountverification/infra \
        BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/accountverification/infra/client/SsafyAccountVerificationApiClientTest.java
git commit -m "feat: add ssafy accountverification client"
```

## Chunk 3: Integration, docs, and verification

### Task 5: Wire errors, docs, and final verification

**Files:**
- Modify: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/exception/AccountErrorCode.java`
- Modify: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/model/entity/Account.java`
- Create: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/accountverification/exception/AccountVerificationErrorCode.java`
- Create: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/accountverification/exception/AccountVerificationException.java`
- Modify: `BE/amagetdone/docs/account-api-spec.md`

- [ ] **Step 1: Write the failing test**

```java
@Test
void accountVerificationFailureCodesMapToDomainErrors() { }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests '*Account*'`
Expected: FAIL because new error mappings and account state updates are incomplete

- [ ] **Step 3: Write minimal implementation**

```java
// add error codes, account verified mutator, docs updates
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests '*Account*'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account \
        BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/accountverification \
        BE/amagetdone/docs/account-api-spec.md
git commit -m "feat: complete account one-won verification"
```

- [ ] **Step 6: Run broader verification**

Run: `./gradlew test`
Expected: PASS for the full backend test suite
