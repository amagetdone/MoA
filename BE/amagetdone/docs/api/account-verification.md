# Account Verification API Spec

Base URL: `https://j14d101.p.ssafy.io/api`

## 1원 인증 요청

- 기능명: `1원 인증 요청`
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/accounts/verification/requests`
- Path: `/api/v1/accounts/verification/requests`
- Headers:
  - `Authorization: Bearer {accessToken}`
  - `Content-Type: application/json`
- Path Variables: `없음`
- Query Parameters: `없음`
- Request Body:

```json
{
  "accountNo": "0011234567890123"
}
```

- Response Body:

```json
{
  "status": "ok",
  "message": "1원 인증 요청을 생성했습니다.",
  "data": {
    "verificationId": 12,
    "accountNo": "0011234567890123",
    "authText": "AMAGETDONE",
    "transactionUniqueNo": 88,
    "requestedAt": "2026-03-13T09:00:00Z",
    "status": "REQUESTED"
  }
}
```

- Error Responses:
  - `401 MEMBER_008`: Authorization header is required
  - `401 MEMBER_009`: Access token is required
  - `401 MEMBER_010`: Invalid access token
  - `404 ACCOUNT_004`: Account not found
  - `502 ACCOUNT_VERIFICATION_002`: Verification API call failed
  - `502 ACCOUNT_VERIFICATION_003`: Invalid verification API response
- Notes:
  - 컨트롤러 단위 테스트에서 Authorization 헤더 기반 `userId` 추출을 검증한다.

## 1원 인증 코드 확인

- 기능명: `1원 인증 코드 확인`
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/accounts/verification/check`
- Path: `/api/v1/accounts/verification/check`
- Headers:
  - `Authorization: Bearer {accessToken}`
  - `Content-Type: application/json`
- Path Variables: `없음`
- Query Parameters: `없음`
- Request Body:

```json
{
  "accountNo": "0011234567890123",
  "authCode": "123"
}
```

- Response Body:

```json
{
  "status": "ok",
  "message": "1원 인증을 완료했습니다.",
  "data": {
    "verificationId": 12,
    "accountNo": "0011234567890123",
    "status": "VERIFIED",
    "verified": true,
    "transactionUniqueNo": 88,
    "verifiedAt": "2026-03-13T09:01:00Z"
  }
}
```

- Error Responses:
  - `401 MEMBER_008`: Authorization header is required
  - `401 MEMBER_009`: Access token is required
  - `401 MEMBER_010`: Invalid access token
  - `404 ACCOUNT_VERIFICATION_001`: Verification request not found
  - `400 ACCOUNT_VERIFICATION_004`: Auth history not found
  - `400 ACCOUNT_VERIFICATION_005`: Auth code expired
  - `400 ACCOUNT_VERIFICATION_006`: Auth code mismatch
  - `400 ACCOUNT_VERIFICATION_007`: Invalid auth code
  - `400 ACCOUNT_VERIFICATION_008`: Invalid auth text
- Notes:
  - 요청 필드명은 `authCode`이며, 계좌번호와 함께 전달해야 한다.

## 1원 인증 상태 조회

- 기능명: `1원 인증 상태 조회`
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/accounts/verification/{accountNo}`
- Path: `/api/v1/accounts/verification/{accountNo}`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables:
  - `accountNo: String`
- Query Parameters: `없음`
- Request Body: `없음`
- Response Body:

```json
{
  "status": "ok",
  "message": "1원 인증 상태를 조회했습니다.",
  "data": {
    "verificationId": 12,
    "accountNo": "0011234567890123",
    "status": "VERIFIED",
    "verified": true,
    "authText": "AMAGETDONE",
    "requestTransactionUniqueNo": 88,
    "requestedAt": "2026-03-13T09:00:00Z",
    "verifiedAt": "2026-03-13T09:01:00Z",
    "failureReason": null
  }
}
```

- Error Responses:
  - `401 MEMBER_008`: Authorization header is required
  - `401 MEMBER_009`: Access token is required
  - `401 MEMBER_010`: Invalid access token
  - `404 ACCOUNT_VERIFICATION_001`: Verification request not found
- Notes:
  - `requestTransactionUniqueNo` 필드명은 요청 생성 시 거래번호를 의미한다.
