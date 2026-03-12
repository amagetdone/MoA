# SSAFY Global Package Refactor Design

**Date:** 2026-03-12

## Goal

`global.infra.ssafy` 하위에 흩어져 있는 공통 transport 관련 클래스를 역할별 하위 패키지로 재배치해
가독성과 책임 경계를 명확하게 만든다.

## Scope

- `global.infra.ssafy` 내부 클래스의 패키지 재구성
- 관련 import 정리
- 공통 테스트와 domain client 테스트의 import/구조 검증 갱신

## Out Of Scope

- SSAFY 요청/응답 동작 변경
- JSON 직렬화/역직렬화 구조 변경
- domain client API 시그니처 변경
- 새로운 helper/base client 도입

## Package Structure

- `global.infra.ssafy.model`
  - `SsafyRequestHeader`
  - `SsafyResponseHeader`
  - `SsafyRequest<T>`
  - `SsafyResponse<T>`
- `global.infra.ssafy.envelope`
  - `SsafyRequestEnvelope<T>`
  - `SsafyResponseEnvelope<T>`
- `global.infra.ssafy.support`
  - `SsafyApiRequestFactory`
  - `SsafyApiResponseValidator`
- `global.infra.ssafy.exception`
  - `SsafyApiException`

## Dependency Direction

- `support` -> `model`, `envelope`, `exception`
- `envelope` -> `model`
- `model` -> 없음
- `exception` -> 없음
- domain client -> 필요한 하위 패키지만 참조

## Refactor Rules

- 파일명은 유지하고 package 선언과 import만 조정한다.
- 기능 변경은 하지 않는다.
- domain 쪽 behavior는 그대로 유지한다.

## Testing

최소 검증:

- `SsafyApiRequestFactoryTest`
- `SsafyApiResponseValidatorTest`
- `SsafyEnvelopeSerializationTest`
- `SsafyMerchantApiClientTest`
- `SsafyCardTransactionApiClientTest`
- `SsafyDemandDepositTransactionApiClientTest`
- `SsafyInfraClientStructureTest`

최종 검증:

- 위 스위트 `test`
- 위 스위트 `clean test`
