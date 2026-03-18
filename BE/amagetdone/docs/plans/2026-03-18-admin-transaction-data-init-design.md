# Admin Transaction Data Init Design

**Date:** 2026-03-18

## Goal

Admin Data init API를 기존의 무의미한 랜덤 더미 생성 방식에서 제거하고, 제공된 실제 거래내역 엑셀을 기반으로 미리 정제한 하드코딩 시드를 대상 계좌 또는 카드에 적재하는 구조로 재설계한다.

## Scope

- `user` 단위 적재 API는 제공하지 않는다.
- `accountNo` 기반 계좌 거래내역 적재 API를 제공한다.
- `cardNo` 기반 카드 거래내역 적재 API를 제공한다.
- 기존 SSAFY API를 통한 계좌 생성/카드 생성 로직은 Admin init 경로에서 제거한다.
- 요청 파라미터에서 `apiKey`, `userApiKey`는 제거한다.
- 실거래 원본은 아래 두 파일을 기준으로 한다.
  - `C:/Users/SSAFY/more/data/origin/거래내역조회_20260225.xlsx`
  - `C:/Users/SSAFY/more/data/origin/카드이용내역.xls`

## Fixed Rules

- 기준 현재 시점은 `2026-03-18` 이다.
- 완료된 월은 `2026-02` 로 본다.
- 적재 대상 데이터는 완료된 월 1개월치만 사용한다.
- 저장 시 거래 월은 오늘 기준 3개월 전 월인 `2025-12` 로 이동한다.
- 엑셀 원본의 `일/시/분/초` 는 유지하고 월만 `2025-12` 로 치환한다.
- 같은 계좌 또는 카드에 대해 API를 재호출하면 기존 override 거래내역을 삭제한 뒤 다시 적재한다.

## Architecture

### API

- `POST /admin/data-init/account`
  - request: `accountNo`
  - response: 적재 대상 번호, 적재 건수, 기준 월
- `POST /admin/data-init/card`
  - request: `cardNo`
  - response: 적재 대상 번호, 적재 건수, 기준 월

기존 `POST /admin/data-init` 단일 API는 제거한다.

### Layer Responsibility

- Controller
  - 요청 DTO 검증
  - 서비스 호출
  - 응답 래핑
- Service
  - 유스케이스 위임
- UseCase
  - 대상 조회
  - 기존 override 삭제
  - 하드코딩 시드 조회
  - 엔티티 매핑 및 저장
  - 결과 응답 생성
- Scenario class
  - 계좌/카드 실거래 시드를 보관하는 전용 클래스
  - 런타임 엑셀 파싱은 하지 않는다

## Components

### DTO

- `AccountAdminDto.AccountTransactionInitRequest`
- `AccountAdminDto.CardTransactionInitRequest`
- `AccountAdminDto.TransactionInitResponse`

### Controller

- 기존 `AdminDataInitController.initialize(...)` 제거
- `initializeAccountTransactions(...)` 추가
- `initializeCardTransactions(...)` 추가

### Service

- `initializeDummyData(...)` 제거
- `initializeAccountTransactions(...)` 추가
- `initializeCardTransactions(...)` 추가

### UseCase

- `InitializeSsafyDummyDataUseCase` 제거
- `InitializeAccountTransactionOverridesUseCase` 추가
- `InitializeCardTransactionOverridesUseCase` 추가

### Scenario

- `AccountRealTransactionScenario`
- `CardRealTransactionScenario`
- 필요 시 공통 날짜 보정 전용 support util 추가

## Data Model Strategy

### Account Transactions

- `Account` 는 `accountNo` 로 조회한다.
- 조회된 계좌에 대해 기존 `AccountTransactionOverride` 를 전부 삭제한다.
- `거래내역조회_20260225.xlsx` 의 완료월 구간 거래를 바탕으로 만든 하드코딩 시드를 `AccountTransactionOverride` 로 저장한다.

### Card Transactions

- `Card` 는 `cardNo` 로 조회한다.
- 조회된 카드에 대해 기존 `CardTransactionOverride` 를 전부 삭제한다.
- `카드이용내역.xls` 의 완료월 구간 거래를 바탕으로 만든 하드코딩 시드를 `CardTransactionOverride` 로 저장한다.

### Category And Merchant

- 기존 구현처럼 기본 카테고리 하나를 공통 매핑한다.
- 카드 거래는 가맹점명이 시드에 포함되므로 merchant 조회 후 없으면 생성한다.
- merchant 식별자는 시드에서 고정된 deterministic id 를 사용한다.

## Seed Management

런타임 엑셀 파싱 대신 아래 원칙으로 시드를 코드에 둔다.

- 엑셀을 읽어 선별한 완료월 거래만 반영한다.
- 개인정보 또는 민감정보는 필요한 최소 범위만 사용한다.
- 거래 시드 클래스는 계좌와 카드를 완전히 분리한다.
- 시드 클래스는 단순 데이터 공급자 역할만 한다.
- 시드 데이터의 날짜는 원본의 day/time 을 유지하면서 `2025-12` 로 맞춰 저장한다.

## Transaction And Idempotency

- 계좌 적재와 카드 적재는 각각 하나의 `@Transactional` 로 처리한다.
- 순서는 `대상 조회 -> 기존 override 삭제 -> 새 override 저장` 으로 고정한다.
- 재실행 시 결과가 동일해야 하므로 delete-and-reload 정책을 사용한다.

## Error Handling

- `accountNo` 대상 계좌가 없으면 `AccountException` 기반 예외를 반환한다.
- `cardNo` 대상 카드가 없으면 도메인 예외를 반환한다.
- 시드 데이터가 비어 있거나 필수 설정이 누락되면 명시적 예외를 반환한다.
- Admin init 경로에서는 SSAFY API 호출이 없어야 하며, 따라서 외부 API 실패는 이 경로의 주된 실패 원인이 아니다.

## Testing

### Controller Tests

- 계좌 거래 적재 endpoint가 서비스 호출 결과를 올바르게 응답으로 감싸는지 검증한다.
- 카드 거래 적재 endpoint가 서비스 호출 결과를 올바르게 응답으로 감싸는지 검증한다.

### UseCase Tests

- 대상 계좌의 기존 override 거래를 삭제 후 새로 저장하는지 검증한다.
- 대상 카드의 기존 override 거래를 삭제 후 새로 저장하는지 검증한다.
- 저장 건수가 시드 건수와 일치하는지 검증한다.
- 모든 거래일이 `2025-12` 월로 이동됐는지 검증한다.
- 계좌 또는 카드 미존재 시 예외를 던지는지 검증한다.

## Migration Notes

- `InitializeSsafyDummyDataUseCase` 와 관련 테스트는 제거 또는 대체한다.
- Admin init 경로에서 더 이상 `apiKey`, `userApiKey` 요청 필드를 사용하지 않는다.
- SSAFY key 설정은 애플리케이션에 남아 있어도 되지만, Admin init 기능에서는 사용하지 않는다.
