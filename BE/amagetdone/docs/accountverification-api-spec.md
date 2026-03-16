# Account Verification API Spec

## Overview

- Base path: `/api`
- Response wrapper: `ApiResponse<T>`
- 1원 인증 요청 API는 사용자 액세스 토큰이 필요합니다.
- 1원 인증 검증/상태조회 API는 admin 경계에서 사용합니다.

## Response Wrapper

```json
{
  "status": "ok",
  "message": "요청 성공 메시지",
  "data": {}
}
```

## 1. User API

### 1.1 1원 인증 요청

- Method: `POST`
- Path: `/v1/accounts/verification/requests`

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
  "requestedAt": "2026-03-13T09:00:00",
  "status": "REQUESTED"
}
```

## 2. Admin API

### 2.1 1원 인증 검증

- Method: `POST`
- Path: `/v1/admin/accounts/verification/check`

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
  "verifiedAt": "2026-03-13T09:03:00"
}
```

### 2.2 1원 인증 상태 조회

- Method: `GET`
- Path: `/v1/admin/accounts/verification/{accountNo}`

Query

- `userId`: 사용자 식별자

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
  "requestedAt": "2026-03-13T09:00:00",
  "verifiedAt": "2026-03-13T09:03:00",
  "failureReason": null
}
```

## Persistence Notes

- 인증 상태는 외부 SSAFY 응답만으로 판단하지 않고 우리 DB의 `AccountVerification` 이력 기준으로 확인합니다.
- 검증 성공 시 계좌의 `Account.verified` 값을 `true`로 갱신합니다.
- 인증 요청/실패/성공 이력은 누적 저장하고 최신 요청 기준으로 상태를 조회합니다.

## References

- Controller: `src/main/java/com/ssafy/amagetdone/domain/accountverification/api/controller/AccountVerificationController.java`
- Controller: `src/main/java/com/ssafy/amagetdone/domain/accountverification/api/controller/AccountVerificationAdminController.java`
- DTO: `src/main/java/com/ssafy/amagetdone/domain/accountverification/api/dto/AccountVerificationDto.java`
- DTO: `src/main/java/com/ssafy/amagetdone/domain/accountverification/api/dto/AccountVerificationAdminDto.java`
- Service: `src/main/java/com/ssafy/amagetdone/domain/accountverification/application/service/AccountVerificationService.java`
