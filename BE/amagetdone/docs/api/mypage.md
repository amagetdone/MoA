# Mypage API

## 내 정보 요약 조회

- 기능명: 내 정보 요약 조회
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/mypage`
- Path: `/api/v1/mypage`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables: 없음
- Query Parameters: 없음
- Request Body: 없음
- Response Body:

```json
{
  "status": "ok",
  "message": "내 정보 요약을 조회했습니다.",
  "data": {
    "userName": "테스트유저",
    "userEmail": "test@more.com",
    "profileImageUrl": null,
    "activePlanCount": 2,
    "completedPlanCount": 1,
    "totalSavedAmount": 2550000
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
  - `userName`, `userEmail`, `profileImageUrl`는 `users` 테이블의 현재 사용자 프로필 정보다.
  - `activePlanCount`, `completedPlanCount`, `totalSavedAmount`는 `PlanRepository` 집계값이며 `내 정보` 탭 상단 요약 카드에 함께 쓰인다.
  - `profileImageUrl`은 값이 없을 수 있어 `null`을 허용한다.
  - 예시는 `MypageControllerTest`, 집계 규칙은 `MypageQueryServiceTest` 기준이다.
