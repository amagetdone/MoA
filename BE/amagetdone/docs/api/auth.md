# Auth API

## 소셜 로그인

- 기능명: 소셜 로그인
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/auth/social/{provider}`
- Path: `/api/v1/auth/social/{provider}`
- Headers:
  - `Content-Type: application/json`
- Path Variables:
  - `provider`: `google` | `kakao` (그 외 값은 `MEMBER_001`)
- Query Parameters: 없음
- Request Body:

```json
{
  "accessToken": "kakao-access-token",
  "idToken": "google-id-token"
}
```

Notes:
- `kakao` 사용 시 `accessToken` 사용.
- `google` 사용 시 `idToken` 사용.

- Response Body:

```json
{
  "status": "ok",
  "message": "소셜 로그인에 성공했습니다.",
  "data": {
    "status": "TEMP",
    "account": false,
    "plan": false,
    "accessToken": "access-token",
    "refreshToken": "refresh-token"
  }
}
```

- Error Responses:

```json
{
  "status": "error",
  "errorCode": "MEMBER_001",
  "message": "Unsupported social provider"
}
```

```json
{
  "status": "error",
  "errorCode": "MEMBER_002",
  "message": "Google id token is required"
}
```

```json
{
  "status": "error",
  "errorCode": "MEMBER_003",
  "message": "Invalid Google id token"
}
```

```json
{
  "status": "error",
  "errorCode": "MEMBER_005",
  "message": "Kakao access token is required"
}
```

```json
{
  "status": "error",
  "errorCode": "MEMBER_006",
  "message": "Invalid Kakao access token"
}
```

- Notes:
  - 응답 `data`에는 `profile` 필드가 없고 `status` 필드를 사용한다.
  - 예시는 `AuthControllerTest.socialAuthReturnsJwtPayloadForKakao` 기준이다.

## 테스트 소셜 로그인

- 기능명: 테스트 소셜 로그인
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/auth/social/test`
- Path: `/api/v1/auth/social/test`
- Headers: 없음
- Path Variables: 없음
- Query Parameters: 없음
- Request Body: 없음
- Response Body:

```json
{
  "status": "ok",
  "message": "테스트 소셜 로그인에 성공했습니다.",
  "data": {
    "status": "TEMP",
    "account": false,
    "plan": false,
    "accessToken": "access-token",
    "refreshToken": "refresh-token"
  }
}
```

- Error Responses:
  - 코드/테스트에서 별도 명시 없음
- Notes:
  - 예시는 `AuthControllerTest.socialTestAuthReturnsJwtPayload` 기준이다.

## 테스트 소셜 사용자 초기화

- 기능명: 테스트 소셜 사용자 초기화
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/auth/social/test/reset`
- Path: `/api/v1/auth/social/test/reset`
- Headers: 없음
- Path Variables: 없음
- Query Parameters: 없음
- Request Body: 없음
- Response Body:

```json
{
  "status": "ok",
  "message": "테스트 소셜 사용자를 초기화했습니다.",
  "data": {
    "success": true
  }
}
```

- Error Responses:
  - 코드/테스트에서 별도 명시 없음
- Notes:
  - 예시는 `AuthControllerTest.socialTestResetReturnsSuccess` 기준이다.

## 토큰 재발급

- 기능명: 토큰 재발급
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/auth/refresh`
- Path: `/api/v1/auth/refresh`
- Headers:
  - `Content-Type: application/json`
- Path Variables: 없음
- Query Parameters: 없음
- Request Body:

```json
{
  "refreshToken": "refresh-token"
}
```

- Response Body:

```json
{
  "status": "ok",
  "message": "토큰 재발급에 성공했습니다.",
  "data": {
    "status": "ACTIVE",
    "account": true,
    "plan": false,
    "accessToken": "new-access-token",
    "refreshToken": "new-refresh-token"
  }
}
```

- Error Responses:

```json
{
  "status": "error",
  "errorCode": "MEMBER_011",
  "message": "Refresh token is required"
}
```

```json
{
  "status": "error",
  "errorCode": "MEMBER_012",
  "message": "Invalid refresh token"
}
```

```json
{
  "status": "error",
  "errorCode": "MEMBER_013",
  "message": "Refresh token reuse detected"
}
```

- Notes:
  - 예시는 `AuthControllerTest.refreshReturnsRotatedTokens` 기준이다.

## 로그아웃

- 기능명: 로그아웃
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/auth/logout`
- Path: `/api/v1/auth/logout`
- Headers:
  - `Content-Type: application/json`
- Path Variables: 없음
- Query Parameters: 없음
- Request Body:

```json
{
  "refreshToken": "refresh-token"
}
```

- Response Body:

```json
{
  "status": "ok",
  "message": "로그아웃에 성공했습니다.",
  "data": {
    "success": true
  }
}
```

- Error Responses:

```json
{
  "status": "error",
  "errorCode": "MEMBER_011",
  "message": "Refresh token is required"
}
```

```json
{
  "status": "error",
  "errorCode": "MEMBER_012",
  "message": "Invalid refresh token"
}
```

- Notes:
  - 예시는 `AuthControllerTest.logoutReturnsSuccess` 기준이다.
