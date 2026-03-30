# Admin Payment Design

**Date:** 2026-03-24

## Goal

기존 admin data init 성격의 기능을 보다 일반적인 admin API로 묶고, 테스트 결제 시 SSAFY 계정계와 우리 시스템의 거래내역을 즉시 동기화할 수 있도록 admin 결제 API를 추가한다.

## Scope

- `POST /admin/payments/account`
- `POST /admin/payments/card`
- `POST /admin/payments/card/cancel`
- 기존 admin 상품/데이터 초기화 엔드포인트를 `AdminController`로 통합
- 결제 전용 서비스와 use case 분리
- 결제 직후 단건 거래를 로컬 override 테이블에 append 저장

## Non-Goals

- 일반 사용자용 결제 API 추가
- 기존 `/transactions/sync` 흐름 대체
- 결제 취소 시 기존 거래 row 수정

## Architecture

### Controller

- 기존 `AccountAdminController`, `AdminDataInitController`를 `AdminController`로 통합한다.
- 컨트롤러는 `/admin` 경로 하위에서 admin 성격의 엔드포인트를 제공한다.
- 결제 관련 엔드포인트는 `/admin/payments/*` 아래에 둔다.

### Service Layer

- 기존 상품/초기화 계열 책임은 `AccountAdminService`로 둔다.
- 새 결제 책임은 `AdminPaymentService`로 분리한다.
- 컨트롤러는 입출력 매핑만 담당하고, 비즈니스 판단은 서비스와 use case로 위임한다.

### Use Cases

- `ExecuteAdminAccountPaymentUseCase`
- `ExecuteAdminCardPaymentUseCase`
- `CancelAdminCardPaymentUseCase`

각 use case는 다음 공통 흐름을 가진다.

1. 현재 로그인 사용자의 자산인지 검증
2. SSAFY admin API 호출
3. 응답으로 받은 거래 1건을 로컬 override에 즉시 저장
4. API 응답용 DTO 반환

## API Design

### POST /admin/payments/account

요청:

- `accountNo`
- `counterpartyAccountNo`
- `amount`
- `withdrawalSummary`
- `depositSummary`

처리:

- 현재 로그인 사용자 기준으로 `accountNo`가 활성/연결 계좌인지 확인
- SSAFY 수시입출금 이체 API 호출
- 출금 계좌 기준 거래 1건을 `account_transaction_override`에 저장

응답:

- 원격 결제 결과
- 로컬 저장된 거래 식별 정보
- 동기화 대상 타입/account 번호

### POST /admin/payments/card

요청:

- `cardNo`
- `merchantId`
- `amount`

처리:

- 현재 로그인 사용자 기준으로 `cardNo` 소유 여부 확인
- 로컬 `Card` 엔티티에서 `cvc`와 연결 사용자 `userKey` 확보
- SSAFY 카드 결제 API 호출
- 응답 거래 1건을 `card_transaction_override`에 저장

응답:

- 원격 결제 결과
- 로컬 저장된 거래 식별 정보
- 동기화 대상 타입/card 번호

### POST /admin/payments/card/cancel

요청:

- `cardNo`
- `transactionUniqueNo`

처리:

- 현재 로그인 사용자 기준으로 카드 소유 여부 확인
- 로컬 `Card` 엔티티에서 `cvc` 확보
- SSAFY `deleteTransaction` 호출
- 취소 거래를 새로운 `card_transaction_override` row로 append 저장

응답:

- 취소 거래 결과
- 로컬 저장된 취소 거래 식별 정보
- 상태값 `CANCEL`

## Data Synchronization Rules

- 전체 `/transactions/sync`를 재호출하지 않는다.
- 결제 API 응답으로 받은 단건 거래만 즉시 로컬 override에 적재한다.
- 돈/거래 이력 성격의 데이터이므로 append-only 원칙을 따른다.
- 카드 취소는 기존 거래를 수정하지 않고 별도 취소 row를 추가한다.

## Persistence Strategy

### Account payment

- SSAFY 수시입출금 이체 응답으로 받은 출금 거래를 `AccountTransactionOverride`로 매핑한다.
- 기존 `TransactionSyncService`의 account 매핑 규칙과 동일한 카테고리/메모/시간 변환 규칙을 재사용한다.

### Card payment

- SSAFY 카드 결제 응답을 `CardTransactionOverride`로 매핑한다.
- merchant/category 매핑은 기존 카드 동기화 규칙과 일관되게 유지한다.

### Card cancel

- 취소 응답도 `CardTransactionOverride`로 저장한다.
- `billStatementsStatus`는 `CANCEL`을 저장한다.
- 기존 승인 거래의 row update는 수행하지 않는다.

## Error Handling

- 사용자 자산 검증 실패:
  - 계좌는 `ACCOUNT_NOT_FOUND`
  - 카드는 `CARD_NOT_FOUND`
- 계좌 이체 실패:
  - `DEMAND_DEPOSIT_TRANSFER_API_CALL_FAILED`
  - `INVALID_DEMAND_DEPOSIT_TRANSFER_API_RESPONSE`
- 카드 결제 실패:
  - `CREDIT_CARD_TRANSACTION_API_CALL_FAILED`
  - `INVALID_CREDIT_CARD_TRANSACTION_API_RESPONSE`
- 카드 취소 실패:
  - 신규 에러코드 추가
  - `CREDIT_CARD_TRANSACTION_CANCEL_API_CALL_FAILED`
  - `INVALID_CREDIT_CARD_TRANSACTION_CANCEL_API_RESPONSE`
- SSAFY 세부 오류 정보는 기존 `AccountException.detail` 구조를 유지한다.

## Testing Strategy

### Controller tests

- `AdminControllerTest`
- 각 엔드포인트가 서비스 응답을 `ApiResponse.ok`로 래핑하는지 검증
- 기존 admin 엔드포인트 테스트를 통합 컨트롤러 기준으로 재정리

### Use case tests

- 계좌 결제:
  - 자산 소유 검증
  - SSAFY 이체 호출
  - override 저장
  - 응답 매핑
- 카드 결제:
  - 카드 조회
  - cvc 활용
  - SSAFY 결제 호출
  - override 저장
- 카드 취소:
  - SSAFY 취소 호출
  - 취소 거래 append 저장
  - 기존 거래 row 비수정

### TDD discipline

- 컨트롤러 테스트부터 실패를 확인
- use case 테스트 실패를 확인
- 최소 구현 추가
- 관련 테스트 재실행

## Trade-offs

- 컨트롤러는 통합하지만 서비스와 use case는 분리해 API 표면과 내부 책임을 동시에 정리한다.
- 기존 sync를 재사용하지 않고 단건 저장 로직을 별도로 두어 테스트 결제 API 목적에 맞는 즉시성과 정확성을 확보한다.
- 일부 매핑 로직 중복 가능성이 있으나, 우선은 명확한 결제 단건 저장 흐름을 우선한다.
