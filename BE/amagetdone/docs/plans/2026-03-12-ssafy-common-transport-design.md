# SSAFY Common Transport Refactor Design

**Date:** 2026-03-12

## Goal

`asset`와 `transaction` 도메인에 흩어진 SSAFY 공통 요청/응답 구조를 `global.infra.ssafy`로 올려서,
도메인별 `infra.client`는 각 API의 payload와 결과 매핑만 관리하도록 정리한다.

## Scope

- SSAFY 공통 요청 Header/응답 Header 모델 분리
- 제네릭 요청/응답 transport 모델 설계
- 공통 Header 생성 factory 추가
- 공통 응답 검증 validator 추가
- `asset`/`transaction` client가 공통 transport를 참조하도록 구조 재설계
- domain별 payload/request/response/result 타입 경계 재정의

## Out Of Scope

- controller, application, usecase 추가
- DB 저장 로직 추가
- SSAFY 외 다른 외부 API 추상화
- global 레벨의 범용 HTTP base client 도입

## Design Principles

- `global`에는 도메인 의미가 아닌 SSAFY transport 규약만 둔다.
- 도메인 client는 endpoint, payload, domain result 매핑만 안다.
- 공통화는 중복 제거가 목적이지만, domain 예외 체계를 침범하지 않는다.
- 문서 기준 공통 구조는 Header와 응답 성공 규칙(`responseCode == H0000`)까지만 다룬다.

## Architecture

### Global Infra

신규 패키지:

- `src/main/java/com/ssafy/amagetdone/global/infra/ssafy`

구성 요소:

- `SsafyRequestHeader`
  - `apiName`
  - `transmissionDate`
  - `transmissionTime`
  - `institutionCode`
  - `fintechAppNo`
  - `apiServiceCode`
  - `institutionTransactionUniqueNo`
  - `apiKey`
  - `userKey`
- `SsafyResponseHeader`
  - `responseCode`
  - `responseMessage`
  - `apiName`
  - `transmissionDate`
  - `transmissionTime`
  - `institutionCode`
  - `fintechAppNo`
  - `apiServiceCode`
  - `institutionTransactionUniqueNo`
- `SsafyRequest<T>`
  - 내부 애플리케이션에서 사용하는 transport 모델
  - `header`, `payload`로 분리된 형태
- `SsafyResponse<T>`
  - validator 통과 후 client가 다루는 transport 모델
  - `header`, `payload`로 분리된 형태
- `SsafyRequestEnvelope<T>`
  - JSON 직렬화 전용 wrapper
  - `Header`와 payload 평탄화 담당
- `SsafyResponseEnvelope<T>`
  - JSON 역직렬화 전용 wrapper
  - `Header`와 payload 평탄화 담당
- `SsafyApiRequestFactory`
  - 공통 Header 생성
  - `userKey` 유무에 따라 Header 조립
  - `SsafyRequest<T>`를 `SsafyRequestEnvelope<T>`로 변환
- `SsafyApiResponseValidator`
  - null Header, 실패 응답 코드, payload 누락 검증
  - 검증 통과 시 `SsafyResponse<T>` 반환
- `SsafyApiException`
  - global infra 내부 전용 예외

### Domain Infra

`asset.infra.client`, `transaction.infra.client`에는 아래만 남긴다.

- endpoint 경로
- 도메인 payload request/response record
- 외부 payload를 domain result로 변환하는 매핑
- `SsafyApiException`을 domain 예외로 변환하는 처리

도메인에서 제거 대상:

- `*SsafyRequestHeader`
- `*SsafyResponseHeader`

## Transport Model Strategy

SSAFY 문서의 실제 JSON은 `Header`와 payload 필드가 최상위에 함께 존재한다.
하지만 도메인 코드에서 이 구조를 직접 표현하면 Header 타입이 도메인마다 중복된다.

이 설계에서는 transport와 JSON wrapper를 분리한다.

내부 표현:

- `SsafyRequest<T>(header, payload)`
- `SsafyResponse<T>(header, payload)`

직렬화/역직렬화 표현:

- `SsafyRequestEnvelope<T>`
- `SsafyResponseEnvelope<T>`

효과:

- 도메인은 Header 선언을 몰라도 된다.
- JSON 구조는 문서와 동일하게 유지할 수 있다.
- 검증과 테스트 포인트가 transport 레이어에 집중된다.

## Domain Payload Boundary

### Asset

유지:

- `SsafyMerchantRequestPayload`
- `SsafyMerchantResponsePayload`
- `SsafyMerchantRecord`
- `MerchantItem`
- `MerchantListResult`

`SsafyMerchantApiClient`는 `SsafyApiRequestFactory`로 요청 envelope를 만들고,
`SsafyApiResponseValidator`를 거친 payload만 받아 결과 모델로 매핑한다.

### Transaction

유지:

- 카드 조회 payload/request/result 타입
- 입출금 조회 payload/request/result 타입

분리 원칙:

- 카드와 입출금은 payload 구조가 다르므로 공통 payload로 합치지 않는다.
- 둘 다 공통 transport만 공유한다.

## Error Handling

`global.infra.ssafy`는 domain error code를 모른다.

대신 `SsafyApiException`에 최소 사유만 둔다.

- `INVALID_CONFIGURATION`
- `CALL_FAILED`
- `INVALID_HEADER`
- `FAILED_RESPONSE_CODE`
- `INVALID_PAYLOAD`

도메인 client는 이 예외를 자기 error code로 변환한다.

예시:

- `asset`
  - `MERCHANT_API_CALL_FAILED`
  - `INVALID_MERCHANT_API_RESPONSE`
- `transaction`
  - `CARD_TRANSACTION_API_CALL_FAILED`
  - `INVALID_CARD_TRANSACTION_API_RESPONSE`
  - `DEMAND_DEPOSIT_TRANSACTION_API_CALL_FAILED`
  - `INVALID_DEMAND_DEPOSIT_API_RESPONSE`

이 방식의 장점:

- global helper는 재사용 가능
- domain 예외 체계와 API 계약은 그대로 유지
- 응답 코드 검증 중복 제거

## Data Flow

1. domain client가 payload와 endpoint 메타데이터를 준비한다.
2. `SsafyApiRequestFactory`가 공통 Header를 만들고 request envelope를 조립한다.
3. domain client가 `RestClient`로 SSAFY API를 호출한다.
4. 응답은 `SsafyResponseEnvelope<T>`로 역직렬화된다.
5. `SsafyApiResponseValidator`가 Header와 payload를 검증한다.
6. 검증이 끝난 `SsafyResponse<T>`에서 payload만 꺼내 domain result로 매핑한다.
7. global infra 예외가 발생하면 domain client가 적절한 domain 예외로 변환한다.

## Testing Strategy

### Global Infra Tests

- request factory가 공통 Header를 올바르게 채우는지 검증
- `userKey` 있는 요청과 없는 요청을 각각 검증
- response validator가 `H0000` 성공/실패 코드를 정확히 처리하는지 검증
- envelope 직렬화/역직렬화가 SSAFY 문서 구조와 일치하는지 검증

### Domain Client Regression Tests

- 기존 endpoint 호출 경로가 유지되는지 검증
- payload 필드가 올바르게 전송되는지 검증
- global infra 예외가 domain 예외로 변환되는지 검증
- domain result 매핑 결과가 기존과 동일한지 회귀 검증

## Planned File Changes

Create:

- `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyRequestHeader.java`
- `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyResponseHeader.java`
- `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyRequest.java`
- `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyResponse.java`
- `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyRequestEnvelope.java`
- `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyResponseEnvelope.java`
- `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyApiRequestFactory.java`
- `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyApiResponseValidator.java`
- `src/main/java/com/ssafy/amagetdone/global/infra/ssafy/SsafyApiException.java`
- `src/test/java/com/ssafy/amagetdone/global/infra/ssafy/...`

Modify:

- `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/SsafyMerchantApiClient.java`
- `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyCardTransactionApiClient.java`
- `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/SsafyDemandDepositTransactionApiClient.java`
- domain별 SSAFY payload 파일명과 책임

Delete or replace:

- `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/AssetSsafyRequestHeader.java`
- `src/main/java/com/ssafy/amagetdone/domain/asset/infra/client/AssetSsafyResponseHeader.java`
- `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/TransactionSsafyRequestHeader.java`
- `src/main/java/com/ssafy/amagetdone/domain/transaction/infra/client/TransactionSsafyResponseHeader.java`

## Risks

- Jackson에서 평탄 payload와 `Header`를 동시에 다루는 매핑이 예상보다 까다로울 수 있다.
- 기존 테스트가 Header 구현 세부사항에 결합되어 있으면 테스트 수정 범위가 커질 수 있다.
- factory가 너무 많은 설정 책임을 가져가면 global helper가 비대해질 수 있으므로,
  endpoint와 payload 의미는 여전히 domain client에 남겨야 한다.
