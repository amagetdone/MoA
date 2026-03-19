# Transaction Analysis Sync Separation Design

## 배경

현재 거래 분석 잡은 생성 후 `TransactionAnalysisService.runAnalysis()`에서 SSAFY 거래 동기화를 먼저 수행한 뒤 로컬 분석을 이어간다. 이 구조에서는 다음 문제가 생긴다.

- 분석 지연 원인이 SSAFY 동기화인지, AI 분류인지, 로컬 DB 처리인지 분리해서 보기 어렵다.
- 분석 job 상태가 외부 I/O와 강하게 결합된다.
- 분석 데이터 기준이 로컬 DB인지 외부 최신 상태인지 API만 보고는 명확하지 않다.

## 목표

- 분석 job은 로컬 DB의 거래 데이터만 기준으로 수행한다.
- SSAFY 계좌/카드 거래 동기화는 별도 엔드포인트로 분리한다.
- AI 서버 의존성은 유지하되, SSAFY 의존성은 분석 흐름에서 제거한다.

## 선택지

### 1. 분석과 sync를 완전히 분리한다

- `POST /transactions/sync`를 새로 추가한다.
- `POST /transactions/analysis-jobs`는 로컬 DB만 읽어 분석한다.
- 추천안이다. 책임이 가장 명확하고 장애 추적이 쉬워진다.

### 2. 기존 분석 API에 옵션을 남긴다

- 분석 API는 유지하고 `forceFullSync`를 옵션화한다.
- 기존 클라이언트 영향은 적지만 API 의미가 계속 섞인다.

### 3. 내부 구현만 분리하고 외부 API는 유지한다

- 외부에서는 여전히 분석 호출 시 sync가 섞여 보인다.
- 내부 구조만 개선될 뿐 운영 가시성은 크게 나아지지 않는다.

## 결정

1번을 채택한다.

## API 구조

### `POST /transactions/sync`

- 인증 사용자 기준으로 SSAFY 계좌 거래와 카드 거래를 동기화한다.
- 로컬 `account_transaction_override`, `card_transaction_override`만 갱신한다.
- 요청 필드는 `months`, `forceFullSync`를 유지할 수 있다.
- 응답은 계좌 거래 반영 건수와 카드 거래 반영 건수를 반환한다.

### `POST /transactions/analysis-jobs`

- SSAFY 동기화를 수행하지 않는다.
- 로컬 override 테이블과 로컬 기준 정보만 사용해 분석 job을 생성하고 실행한다.
- 요청 DTO에서 `forceFullSync` 필드는 제거한다.

## 내부 흐름

### 거래 동기화

1. 컨트롤러가 인증 사용자 ID와 요청 DTO를 받는다.
2. `SyncLatestTransactionsUseCase`가 `TransactionSyncService`를 호출한다.
3. `TransactionSyncService`는 SSAFY 계좌 거래, 카드 거래를 각각 조회한다.
4. 신규 거래만 로컬 override 테이블에 적재한다.
5. 적재 건수를 응답으로 반환한다.

### 거래 분석

1. 분석 job을 생성한다.
2. 비동기 실행에서 `runAnalysis(jobId)`만 호출한다.
3. 분석 서비스는 로컬 override 테이블에서 기간 내 거래를 조회한다.
4. 내부 이체, 수입 등 제외 규칙을 적용한다.
5. AI 서버로 merchant labeling을 요청한다.
6. 필수 지출 분류와 snapshot 저장을 수행한다.
7. 결과와 job 상태를 로컬 DB에 저장한다.

## 영향 범위

- `TransactionController`
- `TransactionAnalysisJobDto`
- `StartTransactionAnalysisJobUseCase`
- `TransactionAnalysisResponseFacade`
- `RealTransactionAnalysisResponseFacade`
- `MockTransactionAnalysisResponseFacade`
- `TransactionAnalysisService`
- transaction 도메인의 controller/service/usecase 테스트

## 예외 처리

- `POST /transactions/sync` 실패는 SSAFY 호출 실패로 명확히 반환한다.
- `POST /transactions/analysis-jobs` 실패는 로컬 분석 또는 AI 분류 실패로 한정된다.
- 분석 API에서 sync 관련 파라미터와 예외 메시지는 제거한다.

## 테스트 전략

- 컨트롤러 테스트
  - `POST /transactions/sync` 응답 검증
  - `POST /transactions/analysis-jobs` 요청 DTO에서 `forceFullSync` 제거 검증
- 유스케이스 테스트
  - 분석 job 시작 시 더 이상 sync를 호출하지 않는 점 검증
  - 거래 sync 유스케이스 응답 검증
- 서비스 테스트
  - `TransactionAnalysisService`가 로컬 데이터만으로 분석하는 점 검증
  - `TransactionSyncService`의 SSAFY 동기화 적재 동작 유지 검증

## 운영 기대 효과

- 분석과 동기화의 책임이 분리된다.
- 분석 job 지연과 실패 원인을 더 빠르게 좁힐 수 있다.
- 클라이언트는 “동기화 후 분석” 흐름을 명시적으로 제어할 수 있다.
