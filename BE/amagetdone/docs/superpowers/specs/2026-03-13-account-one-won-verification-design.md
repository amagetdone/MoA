# Account One-Won Verification Design

## Goal

SSAFY 1원 인증 API를 별도 `domain/accountverification` 도메인으로 추가한다. 사용자는 본인 계좌에 대해 인증 요청을 생성하고, 관리자 기능은 해당 요청을 검증하고 우리 DB에 저장된 인증 상태를 확인할 수 있어야 한다.

## Scope

- `user API`는 1원 인증 요청만 담당한다.
- `admin API`는 1원 인증 코드 검증과 DB 기준 인증 상태 조회를 담당한다.
- 인증 상태는 SSAFY 응답만으로 판단하지 않고 우리 DB에 저장된 요청/검증 이력을 기준으로 판단한다.
- 검증 성공 시 계좌의 `verified` 상태를 `true`로 반영한다.

## API Design

### User API

- `POST /v1/accounts/verification/requests`

Request

```json
{
  "accountNo": "0011234567890123"
}
```

Response `data`

```json
{
  "verificationId": 12,
  "accountNo": "0011234567890123",
  "authText": "AMAGETDONE",
  "transactionUniqueNo": 88,
  "requestedAt": "2026-03-13T09:00:00Z",
  "status": "REQUESTED"
}
```

### Admin API

- `POST /v1/admin/accounts/verification/check`

Request

```json
{
  "userId": 7,
  "accountNo": "0011234567890123",
  "authCode": "1234"
}
```

Response `data`

```json
{
  "verificationId": 12,
  "accountNo": "0011234567890123",
  "status": "SUCCESS",
  "verified": true,
  "transactionUniqueNo": 88,
  "verifiedAt": "2026-03-13T09:03:00Z"
}
```

- `GET /v1/admin/accounts/{accountNo}/verification?userId=7`

Response `data`

```json
{
  "verificationId": 12,
  "userId": 7,
  "accountNo": "0011234567890123",
  "status": "SUCCESS",
  "verified": true,
  "authText": "AMAGETDONE",
  "requestTransactionUniqueNo": 88,
  "requestedAt": "2026-03-13T09:00:00Z",
  "verifiedAt": "2026-03-13T09:03:00Z",
  "failureReason": null
}
```

## Domain Structure

기능은 `domain/accountverification`으로 분리하고, `domain/account`는 계좌 원본 정보와 `verified` 상태만 유지한다.

- `domain/account`
  계좌 엔티티, 계좌 조회/연동, `verified` 상태 보유
- `domain/accountverification/api`
  사용자 요청 DTO, 관리자 요청/응답 DTO, 컨트롤러 엔드포인트
- `domain/accountverification/application`
  요청 생성, 검증 수행, 상태 조회용 use case와 service
- `domain/accountverification/model`
  `AccountVerification` 엔티티, 상태 enum, repository
- `domain/accountverification/infra`
  SSAFY `openAccountAuth`, `checkAuthCode` 전용 client와 DTO
- `domain/accountverification/exception`
  1원 인증 요청/검증 실패용 에러 코드와 예외

## Persistence Design

`domain/accountverification`의 `AccountVerification` 엔티티는 다음 책임을 가진다.

- 어떤 사용자의 어떤 계좌에 대해 1원 인증 요청이 생성됐는지 저장
- 최근 요청의 상태를 `REQUESTED`, `SUCCESS`, `FAILED`, `EXPIRED` 중 하나로 관리
- 요청 시 사용한 `authText`, SSAFY 거래번호, 요청 시각, 검증 시각 저장
- 검증 실패 사유 또는 최근 실패 메시지 저장
- 관리자 확인 API가 외부 호출 없이 DB만 보고 상태를 판단할 수 있게 보장

필드 초안

- `id`
- `user` (`users_id`)
- `account` (`account_id`)
- `accountNo`
- `authText`
- `requestTransactionUniqueNo`
- `status`
- `requestedAt`
- `verifiedAt`
- `failureReason`

운영 단순화를 위해 계좌별 최신 요청 1건만 갱신하는 방식보다, 요청 단위 row를 누적 저장하고 `findTopByUserIdAndAccountNoOrderByRequestedAtDesc`로 최신 상태를 본다.

## Verification Flow

### 1. User request

1. 로그인 사용자 ID로 계좌 소유 여부를 확인한다.
2. SSAFY `openAccountAuth` 호출로 1원 인증 송금을 요청한다.
3. 응답의 거래번호와 `authText`를 포함한 `AccountVerification` row를 `REQUESTED` 상태로 저장한다.
4. 응답에는 코드 자체를 포함하지 않고 요청 생성 결과만 반환한다.

### 2. Admin check

1. `userId + accountNo` 기준 최신 `REQUESTED` 인증 row를 조회한다.
2. 저장된 `authText`와 입력 `authCode`로 SSAFY `checkAuthCode`를 호출한다.
3. 성공 시 해당 인증 row를 `SUCCESS`로 갱신하고 `verifiedAt`을 저장한다.
4. 같은 트랜잭션 안에서 `Account.verified`도 `true`로 바꾼다.
5. 실패 시 인증 row를 `FAILED`로 갱신하고 실패 사유를 저장한다.

### 3. Admin status lookup

1. `userId + accountNo` 기준 최신 인증 row를 조회한다.
2. 최신 상태와 계좌 `verified` 값을 응답으로 내려준다.
3. 인증 이력이 없으면 별도 에러 또는 `NOT_REQUESTED` 성격의 예외 응답을 사용한다.

## Infra Design

`domain/accountverification/infra`에 전용 SSAFY 계좌 인증 client를 둔다.

- `SsafyAccountVerificationApiClient.requestVerification`
  내부적으로 `POST /accountAuth/openAccountAuth` 호출
- `SsafyAccountVerificationApiClient.checkVerification`
  내부적으로 `POST /accountAuth/checkAuthCode` 호출

별도 DTO 파일을 추가한다.

- `AccountVerificationClientDto.RequestVerificationRequest`
- `AccountVerificationClientDto.RequestVerificationResponse`
- `AccountVerificationClientDto.CheckVerificationRequest`
- `AccountVerificationClientDto.CheckVerificationResponse`

## Error Handling

다음 경우를 명시적으로 예외 처리한다.

- 사용자가 본인 계좌가 아닌 계좌로 인증 요청
- 관리자 검증 시 최신 요청 이력이 없음
- 관리자 검증 시 이미 성공한 인증을 다시 검증
- SSAFY 요청 실패
- SSAFY 응답 형식 오류
- 인증 코드 불일치 또는 만료

SSAFY의 `A1086`, `A1087`, `A1088`, `A1090` 계열은 내부 `AccountVerificationErrorCode`로 매핑해 구분 가능하게 둔다.

## Testing Strategy

- 컨트롤러 테스트
  `user` 요청 엔드포인트, `admin` 검증/조회 엔드포인트의 경계와 메시지 확인
- 서비스 테스트
  `AccountVerificationService`가 새 use case를 올바르게 위임하는지 확인
- use case 테스트
  요청 저장, 성공 시 `Account.verified` 반영, 실패 시 상태 갱신 확인
- infra client 테스트
  SSAFY `accountAuth` 요청/응답 직렬화 및 에러 매핑 확인
- 엔티티 매핑 테스트
  `AccountVerification`의 컬럼명과 기본값을 `ErdSchemaAlignmentTest`에 추가

## Notes

- `authText`는 설정값 또는 상수로 관리하되, 한 곳에서만 정의한다.
- 사용자 앱 흐름은 user API를 사용하지만, 검증 판단과 운영 확인 기능은 admin API와 DB 상태 중심으로 둔다.
- `Account.verified` 변경은 `domain/accountverification`에서 `Account` 엔티티를 조회해 반영한다.
