# Account One-Won Verification Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add SSAFY one-won account verification with a user request API, admin check API, admin status lookup API, and DB-backed verification history inside `domain/account`.

**Architecture:** Extend the existing account domain instead of creating a new domain. Persist verification history in a dedicated entity, keep the latest status query in our DB, and update `Account.verified` when admin verification succeeds.

**Tech Stack:** Spring Boot, Spring Web, Spring Data JPA, RestClient, JUnit 5, Mockito, AssertJ

---

## Chunk 1: API surface and persistence skeleton

### Task 1: Add failing tests for DTO/controller/service entry points

**Files:**
- Modify: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/account/api/controller/AccountControllerTest.java`
- Modify: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/account/api/controller/AccountAdminControllerTest.java`
- Modify: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/account/application/service/AccountServiceTest.java`

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
void accountServiceDelegatesVerificationMethodsToUseCases() {
    // account service delegates to request/check/status use cases
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests '*AccountControllerTest' --tests '*AccountAdminControllerTest' --tests '*AccountServiceTest'`
Expected: FAIL because verification DTOs and methods do not exist yet

- [ ] **Step 3: Write minimal implementation**

```java
// add DTO records
// add controller endpoints
// add AccountService delegation methods
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests '*AccountControllerTest' --tests '*AccountAdminControllerTest' --tests '*AccountServiceTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/api \
        BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/application/service/AccountService.java \
        BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/account
git commit -m "feat: add account verification api surface"
```

### Task 2: Add failing persistence tests for verification history

**Files:**
- Modify: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/ErdSchemaAlignmentTest.java`
- Create: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/model/entity/AccountVerification.java`
- Create: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/model/entity/AccountVerificationStatus.java`
- Create: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/model/repository/AccountVerificationRepository.java`

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
git add BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/model \
        BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/ErdSchemaAlignmentTest.java
git commit -m "feat: add account verification persistence"
```

## Chunk 2: Verification request and admin check behavior

### Task 3: Add failing use case tests for request/check flows

**Files:**
- Create: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/RequestAccountVerificationUseCaseTest.java`
- Create: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/CheckAccountVerificationUseCaseTest.java`
- Create: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/GetAccountVerificationStatusUseCaseTest.java`

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
git add BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/application \
        BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/account/application/usecase
git commit -m "feat: add account verification use cases"
```

### Task 4: Add failing infra tests for SSAFY account auth client support

**Files:**
- Create: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyAccountAuthApiClientTest.java`
- Modify: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyDemandDepositAccountApiClient.java`
- Create: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/infra/client/dto/AccountVerificationDto.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void requestVerificationCallsOpenAccountAuthEndpoint() { }

@Test
void checkVerificationCallsCheckAuthCodeEndpoint() { }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests '*SsafyAccountAuthApiClientTest'`
Expected: FAIL because account auth client methods do not exist yet

- [ ] **Step 3: Write minimal implementation**

```java
public AccountVerificationDto.RequestVerificationResponse requestAccountVerification(...) { ... }
public AccountVerificationDto.CheckVerificationResponse checkAccountVerification(...) { ... }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests '*SsafyAccountAuthApiClientTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/infra \
        BE/amagetdone/src/test/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyAccountAuthApiClientTest.java
git commit -m "feat: add ssafy account auth client"
```

## Chunk 3: Integration, docs, and verification

### Task 5: Wire errors, docs, and final verification

**Files:**
- Modify: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/exception/AccountErrorCode.java`
- Modify: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/exception/AccountException.java`
- Modify: `BE/amagetdone/docs/account-api-spec.md`
- Modify: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/domain/account/model/entity/Account.java`

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
        BE/amagetdone/docs/account-api-spec.md
git commit -m "feat: complete account one-won verification"
```

- [ ] **Step 6: Run broader verification**

Run: `./gradlew test`
Expected: PASS for the full backend test suite
