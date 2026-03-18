# Admin Data Init AI Category Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** `/admin/data-init`에서 생성하는 계좌/카드 거래를 AI 분류 결과 기반 카테고리로 저장하고, 실패 시 `기타서비스`로 fallback 한다.

**Architecture:** `InitializeSsafyDummyDataUseCase`가 SSAFY 거래 생성 후 저장 전에 AI 서버 배치 분류를 호출한다. 계좌 거래는 `transactionSummary` 우선, 카드 거래는 `merchantName`으로 입력을 만들고, `final_label -> category.name` 매핑 실패나 AI 예외 시 `기타서비스(id=4)`를 사용한다.

**Tech Stack:** Spring Boot, JPA, Mockito, JUnit 5

---

### Task 1: 관리자 더미 초기화 테스트를 AI 분류 기준으로 변경

**Files:**
- Modify: `src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeSsafyDummyDataUseCaseTest.java`

**Step 1: Write the failing test**

- 계좌 거래와 카드 거래가 AI 응답의 `final_label`에 맞는 카테고리로 저장되는 테스트를 추가한다.
- AI 호출 실패 시 `기타서비스(id=4)`로 저장되는 테스트를 추가한다.
- 일부 라벨이 매칭되지 않을 때 해당 거래만 `기타서비스`로 떨어지는 테스트를 추가한다.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.application.usecase.InitializeSsafyDummyDataUseCaseTest"`

Expected: 현재 기본 카테고리 고정 저장 때문에 실패

**Step 3: Write minimal implementation**

- 테스트에서 `MerchantInferenceApiClient` mock을 주입하고 기대 저장 category를 검증한다.

**Step 4: Run test to verify it passes**

같은 명령으로 GREEN 확인

**Step 5: Commit**

```bash
git add src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeSsafyDummyDataUseCaseTest.java
git commit -m "test: cover ai category mapping in admin data init"
```

### Task 2: 관리자 더미 초기화 유스케이스에 AI 분류 로직 추가

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeSsafyDummyDataUseCase.java`

**Step 1: Implement minimal production code**

- `MerchantInferenceApiClient` 의존성 추가
- 계좌 거래 텍스트 추출: `transactionSummary` 우선, 없으면 `transactionMemo`
- 카드 거래 텍스트 추출: `merchantName`
- AI 결과 `final_label -> Category` 매핑
- 실패/누락/미매핑 시 `기타서비스(id=4)` fallback

**Step 2: Run focused test**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.application.usecase.InitializeSsafyDummyDataUseCaseTest"`

Expected: PASS

**Step 3: Refactor**

- 텍스트 정규화와 category resolution을 private 메서드로 정리한다.

**Step 4: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/InitializeSsafyDummyDataUseCase.java
git commit -m "feat: classify admin dummy transactions with ai labels"
```

### Task 3: 계정계 관련 회귀 테스트 실행

**Files:**
- Verify only

**Step 1: Run focused account/admin suite**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.account.*" --tests "com.ssafy.amagetdone.global.init.DataInitializerTest"`

Expected: PASS

**Step 2: Confirm no API contract regressions**

- `/admin/data-init` 응답 DTO와 에러 처리 스펙이 바뀌지 않았는지 확인

**Step 3: Commit**

```bash
git add .
git commit -m "test: verify admin data init ai category integration"
```
