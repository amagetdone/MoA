# Transaction Analysis Sync Separation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 거래 SSAFY 동기화와 로컬 기반 분석 job 실행을 분리해 `POST /transactions/sync`와 로컬 전용 `POST /transactions/analysis-jobs`를 제공한다.

**Architecture:** 거래 동기화는 `SyncLatestTransactionsUseCase`를 별도 컨트롤러 경로로 노출하고, 분석 잡은 `TransactionAnalysisService`에서 SSAFY sync 호출을 제거한 채 로컬 override 테이블만 읽도록 바꾼다. 분석 API 요청 DTO에서는 `forceFullSync`를 제거하고, 테스트는 sync 분리와 로컬 분석 기준을 각각 검증한다.

**Tech Stack:** Spring Boot, Spring MVC, JPA, Mockito, JUnit 5

---

### Task 1: 분석 API 스펙 변경 테스트 추가

**Files:**
- Modify: `src/test/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionControllerTest.java`
- Modify: `src/test/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionControllerMockModeTest.java`

**Step 1: Write the failing test**

- `POST /api/v1/transactions/analysis-jobs` 요청 JSON에서 `forceFullSync` 없이도 정상 동작하는 테스트를 추가한다.
- facade 호출이 `months`만 전달되는 형태로 바뀌었음을 검증한다.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionControllerTest" --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionControllerMockModeTest"`

Expected: 컴파일 실패 또는 기존 시그니처 불일치로 실패

**Step 3: Write minimal implementation**

- `TransactionAnalysisJobDto.CreateRequest`
- `TransactionAnalysisResponseFacade`
- `RealTransactionAnalysisResponseFacade`
- `MockTransactionAnalysisResponseFacade`
- `StartTransactionAnalysisJobUseCase`
- `TransactionController`

위 파일들의 분석 job 생성 시그니처에서 `forceFullSync`를 제거한다.

**Step 4: Run test to verify it passes**

같은 명령을 다시 실행해 녹색 확인

**Step 5: Commit**

```bash
git add src/test/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionControllerTest.java src/test/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionControllerMockModeTest.java src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/TransactionAnalysisJobDto.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/TransactionAnalysisResponseFacade.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/RealTransactionAnalysisResponseFacade.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/MockTransactionAnalysisResponseFacade.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/StartTransactionAnalysisJobUseCase.java src/main/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionController.java
git commit -m "refactor: remove sync option from analysis job api"
```

### Task 2: 거래 sync 엔드포인트 테스트 추가

**Files:**
- Modify: `src/test/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionControllerTest.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/TransactionSyncDto.java`

**Step 1: Write the failing test**

- `POST /api/v1/transactions/sync`가 인증 사용자 기준으로 `SyncLatestTransactionsUseCase`를 호출하고 적재 건수를 반환하는 테스트를 추가한다.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionControllerTest"`

Expected: 엔드포인트 또는 DTO 부재로 실패

**Step 3: Write minimal implementation**

- `TransactionSyncDto.Request`, `TransactionSyncDto.Response` 추가
- `TransactionController`에 `POST /transactions/sync` 추가
- `SyncLatestTransactionsUseCase`를 컨트롤러에 주입하고 응답 매핑 구현

**Step 4: Run test to verify it passes**

같은 명령을 다시 실행해 녹색 확인

**Step 5: Commit**

```bash
git add src/test/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionControllerTest.java src/main/java/com/ssafy/amagetdone/domain/transaction/api/controller/TransactionController.java src/main/java/com/ssafy/amagetdone/domain/transaction/api/dto/TransactionSyncDto.java
git commit -m "feat: add transaction sync endpoint"
```

### Task 3: 분석 서비스 로컬 전용 동작 테스트 추가

**Files:**
- Modify: `src/test/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionAnalysisServiceTest.java`
- Modify or Delete: `src/test/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionAnalysisJobStatusIntegrationTest.java`

**Step 1: Write the failing test**

- `runAnalysis()`가 SSAFY sync 없이도 로컬 override 데이터만으로 분석 결과를 저장하는 테스트를 추가한다.
- `TransactionSyncService` 호출이 더 이상 없어야 함을 검증한다.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionAnalysisServiceTest"`

Expected: 기존 테스트/시그니처와 구현이 어긋나 실패

**Step 3: Write minimal implementation**

- `TransactionAnalysisService.runAnalysis()`에서 `transactionSyncService.syncLatest(...)` 호출 제거
- 필요 시 `runAnalysis` 시그니처에서 `forceFullSync` 제거
- 관련 호출부와 테스트 더블 정리

**Step 4: Run test to verify it passes**

같은 명령을 다시 실행해 녹색 확인

**Step 5: Commit**

```bash
git add src/test/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionAnalysisServiceTest.java src/test/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionAnalysisJobStatusIntegrationTest.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionAnalysisService.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/StartTransactionAnalysisJobUseCase.java
git commit -m "refactor: run transaction analysis from local data only"
```

### Task 4: sync 응답 흐름 정리 및 기존 조회 경로 회귀 확인

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/SyncLatestTransactionsUseCase.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionQueryService.java`
- Modify: 관련 테스트 파일

**Step 1: Write the failing test**

- 거래 조회 흐름이 기존처럼 필요 시 sync를 사용할 수 있는지, 새 sync API와 충돌하지 않는지 테스트를 추가한다.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.transaction.*"`

Expected: 시그니처/의존 경로 변경으로 일부 실패

**Step 3: Write minimal implementation**

- `SyncLatestTransactionsUseCase`의 기본 months 처리와 응답 재사용 정리
- `TransactionQueryService`가 필요한 범위에서만 기존 동작을 유지하도록 조정

**Step 4: Run test to verify it passes**

같은 명령을 다시 실행해 녹색 확인

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/transaction/application/usecase/SyncLatestTransactionsUseCase.java src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionQueryService.java src/test/java/com/ssafy/amagetdone/domain/transaction
git commit -m "test: cover transaction sync separation regressions"
```

### Task 5: 최종 검증

**Files:**
- Verify only

**Step 1: Run focused tests**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionControllerTest" --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionControllerMockModeTest" --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionAnalysisServiceTest"`

Expected: PASS

**Step 2: Run broader transaction suite**

Run: `./gradlew test --tests "com.ssafy.amagetdone.domain.transaction.*"`

Expected: PASS

**Step 3: Manual verification checklist**

- `POST /transactions/sync`가 SSAFY 거래 동기화만 담당하는지 확인
- `POST /transactions/analysis-jobs`가 로컬 DB만 기준으로 job을 시작하는지 확인
- 분석 job 상태 조회가 sync 실패와 무관하게 분석 결과만 반영하는지 확인

**Step 4: Commit**

```bash
git add .
git commit -m "refactor: separate transaction sync from analysis jobs"
```
