# Global API

## 헬스 체크

### 기능명

- 헬스 체크

### Method

- `GET`

### Full URL

- `https://j14d101.p.ssafy.io/api/health`

### Path

- `/api/health`

### Headers

- 없음

### Path Variables

- 없음

### Query Parameters

- 없음

### Request Body

- 없음

### Response Body

```json
{
  "status": "ok",
  "message": "헬스 체크에 성공했습니다.",
  "data": "Hello World!"
}
```

### Error Responses

```json
{
  "status": "error",
  "message": "Internal server error",
  "errorCode": "INTERNAL_SERVER_ERROR"
}
```

### Notes

- `/health`는 `SecurityConfig`의 공개 엔드포인트라 인증 없이 호출 가능하다.
- Response 예시는 `GlobalHealthCheckControllerTest` 기준이다.
