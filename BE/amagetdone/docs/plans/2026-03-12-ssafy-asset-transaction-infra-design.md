# SSAFY Asset/Transaction Infra Design

**Date:** 2026-03-12

## Goal

`asset`와 `transaction` 도메인 하위 `infra` 패키지에 SSAFY 금융 Open API 조회 연동 코드를 추가한다. 이번 범위는 외부 API 호출, 응답 매핑, 도메인 예외 변환, 단위 테스트까지로 제한한다.

## Scope

- `asset.infra.client`에 가맹점 목록조회 연동 추가
- `transaction.infra.client`에 카드 거래내역 조회 연동 추가
- `transaction.infra.client`에 수시입출금 거래내역 조회 연동 추가
- 도메인별 설정 프로퍼티, 응답 모델, 예외 코드, 클라이언트 테스트 추가

## Out of Scope

- controller, application service, usecase 추가
- DB 저장 및 동기화 로직
- `global` 공통 SSAFY 클라이언트 도입
- Notion 작업 정리

## Architecture

### Asset

- `domain.asset.infra.client.SsafyMerchantApiClient`
  - `inquireMerchantList` 호출 전담
  - Header 생성, HTTP 호출, 응답 성공 여부 검사, 도메인 모델 변환 수행
- `domain.asset.infra.client.AssetSsafyApiProperties`
  - `base-url`, `institution-code`, `fintech-app-no`, `api-key` 보관

### Transaction

- `domain.transaction.infra.client.SsafyCardTransactionApiClient`
  - `inquireCreditCardTransactionList` 호출 전담
- `domain.transaction.infra.client.SsafyDemandDepositTransactionApiClient`
  - `inquireTransactionHistoryList` 호출 전담
- `domain.transaction.infra.client.TransactionSsafyApiProperties`
  - 카드/입출금 조회에서 공통으로 쓰는 SSAFY 설정 보관

### Packaging Rule

- 공통 클라이언트는 `global`로 올리지 않는다.
- 도메인마다 독립적인 요청/응답 DTO와 Header 생성 로직을 가진다.
- 응답은 외부 API 원문 DTO가 아니라 도메인 친화 record로 반환한다.

## API Mapping

### Asset Merchant List

- 문서: `docs/6_카드.md`
- API: `POST /creditCard/inquireMerchantList`
- 요청 body: `Header`
- 응답 body: `REC[]`
  - `categoryId`
  - `categoryName`
  - `merchantId`
  - `merchantName`

### Card Transaction List

- 문서: `docs/6_카드.md`
- API: `POST /creditCard/inquireCreditCardTransactionList`
- 요청 body:
  - `Header`
  - `cardNo`
  - `cvc`
  - `startDate`
  - `endDate`
- 응답 body:
  - 상위: `cardIssuerCode`, `cardIssuerName`, `cardName`, `cardNo`, `estimatedBalance`
  - 목록: `transactionUniqueNo`, `categoryId`, `categoryName`, `merchantId`, `merchantName`, `transactionDate`, `transactionTime`, `transactionBalance`, `cardStatus`, `billStatementsYn`, `billStatementsStatus`

### Demand Deposit Transaction List

- 문서: `docs/4_수시입출금.md`
- API: `POST /demandDeposit/inquireTransactionHistoryList`
- 요청 body:
  - `Header`
  - `accountNo`
  - `startDate`
  - `endDate`
  - `transactionType`
  - `orderByType`
- 응답 body:
  - 상위: `totalCount`, `list[]`
  - 목록: `transactionUniqueNo`, `transactionDate`, `transactionTime`, `transactionType`, `transactionTypeName`, `transactionAccountNo`, `transactionBalance`, `transactionAfterBalance`, `transactionSummary`, `transactionMemo`

## Request/Response Model Design

### Asset Return Model

- `MerchantListResult`
- `MerchantItem`

### Transaction Return Models

- `CardTransactionListResult`
- `CardTransactionItem`
- `DemandDepositTransactionListResult`
- `DemandDepositTransactionItem`

### Request Models

- 카드 조회 요청 record
  - `userKey`, `cardNo`, `cvc`, `startDate`, `endDate`
- 입출금 조회 요청 record
  - `userKey`, `accountNo`, `startDate`, `endDate`, `transactionType`, `orderByType`

## Error Handling

### Asset

- `MERCHANT_API_CALL_FAILED`
- `INVALID_MERCHANT_API_RESPONSE`

### Transaction

- `CARD_TRANSACTION_API_CALL_FAILED`
- `INVALID_CARD_TRANSACTION_API_RESPONSE`
- `DEMAND_DEPOSIT_TRANSACTION_API_CALL_FAILED`
- `INVALID_DEMAND_DEPOSIT_API_RESPONSE`

### Rules

- HTTP 호출 실패, 직렬화 실패, null 응답은 도메인 예외로 변환한다.
- SSAFY 응답 `Header.responseCode`가 `H0000`이 아니면 실패로 처리한다.
- infra 계층에서는 최소 입력 검증만 수행한다.
  - blank, null 같은 즉시 실패 가능한 값만 차단

## Configuration

- `src/main/resources/application.properties`에 프로퍼티 placeholder만 추가한다.
- 실제 값은 환경변수로만 주입한다.
- 비밀값은 문서나 로그에 노출하지 않는다.

예상 키:

- `asset.ssafy.base-url`
- `asset.ssafy.institution-code`
- `asset.ssafy.fintech-app-no`
- `asset.ssafy.api-key`
- `transaction.ssafy.base-url`
- `transaction.ssafy.institution-code`
- `transaction.ssafy.fintech-app-no`
- `transaction.ssafy.api-key`

## Testing Strategy

- `MockRestServiceServer`를 사용한 클라이언트 단위 테스트 작성
- 각 클라이언트마다 아래 3가지 케이스를 최소 보장
  - 성공 응답 매핑
  - HTTP 호출 실패 예외 변환
  - `responseCode != H0000` 또는 잘못된 응답 구조 실패

## Planned Files

- `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantApiClient.java`
- `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/AssetSsafyApiProperties.java`
- `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionApiClient.java`
- `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionApiClient.java`
- `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/TransactionSsafyApiProperties.java`
- `src/main/java/com/ssafy/amagetdone/domain/transaction/exception/TransactionErrorCode.java`
- `src/main/java/com/ssafy/amagetdone/domain/transaction/exception/TransactionException.java`
- `src/test/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantApiClientTest.java`
- `src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionApiClientTest.java`
- `src/test/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionApiClientTest.java`
