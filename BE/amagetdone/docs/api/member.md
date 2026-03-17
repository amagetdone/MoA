# Member API

## 추가정보 입력 (회원 프로필 입력 완료)

- 기능명: 추가정보 입력 (회원 프로필 입력 완료)
- Method: `PATCH`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/members/me/profile`
- Path: `/api/v1/members/me/profile`
- Headers:
  - `Authorization: Bearer {accessToken}`
  - `Content-Type: application/json`
- Path Variables: 없음
- Query Parameters: 없음
- Request Body:

```json
{
  "nickname": "moa",
  "email": "moa@example.com",
  "birthDate": "1997-03-01",
  "hasFixedIncome": true
}
```

- Response Body:

```json
{
  "status": "ok",
  "message": "프로필 입력을 완료했습니다.",
  "data": {
    "status": "ACTIVE"
  }
}
```

- Error Responses:

```json
{
  "status": "error",
  "errorCode": "MEMBER_008",
  "message": "Authorization header is required"
}
```

```json
{
  "status": "error",
  "errorCode": "MEMBER_009",
  "message": "Access token is required"
}
```

```json
{
  "status": "error",
  "errorCode": "MEMBER_010",
  "message": "Invalid access token"
}
```

```json
{
  "status": "error",
  "errorCode": "MEMBER_007",
  "message": "User not found"
}
```

- Notes:
  - 응답 계약은 `data.status` 단일 필드다.
  - `nextAction`, `profileCompleted`, `userId` 필드는 응답에 없다.
  - 예시는 `MemberProfileControllerTest.completeProfileReturnsCompletedPayloadForAccessToken` 기준이다.

## 프로필 이미지 업로드

- 기능명: 프로필 이미지 업로드
- Method: `PATCH`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/members/me/profile-image`
- Path: `/api/v1/members/me/profile-image`
- Headers:
  - `Authorization: Bearer {accessToken}`
  - `Content-Type: multipart/form-data`
- Path Variables: 없음
- Query Parameters: 없음
- Request Body:
  - form-data field `image`: 이미지 파일 (`MultipartFile`)

- Response Body:

```json
{
  "status": "ok",
  "message": "프로필 이미지가 업로드되었습니다.",
  "data": "profile/new.png"
}
```

- Error Responses:

```json
{
  "status": "error",
  "errorCode": "MEMBER_008",
  "message": "Authorization header is required"
}
```

```json
{
  "status": "error",
  "errorCode": "MEMBER_009",
  "message": "Access token is required"
}
```

```json
{
  "status": "error",
  "errorCode": "MEMBER_010",
  "message": "Invalid access token"
}
```

```json
{
  "status": "error",
  "errorCode": "MEMBER_007",
  "message": "User not found"
}
```

```json
{
  "status": "error",
  "errorCode": "INVALID_REQUEST",
  "message": "File is empty."
}
```

- Notes:
  - 파일이 비어 있거나 확장자가 없으면 `INVALID_REQUEST`가 반환될 수 있다.
  - 업로드 중 예외는 `INTERNAL_SERVER_ERROR`로 처리될 수 있다.
  - 예시는 `MemberProfileControllerTest.uploadProfileImageWithAuthenticatedPrincipalReturnsObjectKey` 기준이다.
