# Plan API

## 목표 이미지 업로드

### 기능명

- 목표 이미지 업로드

### Method

- `PATCH`

### Full URL

- `https://j14d101.p.ssafy.io/api/v1/plans/{planId}/image`

### Path

- `/api/v1/plans/{planId}/image`

### Headers

- `Authorization: Bearer {accessToken}`
- `Content-Type: multipart/form-data`

### Path Variables

- `planId` (Integer): 이미지를 업로드할 목표 ID

### Query Parameters

- 없음

### Request Body

`multipart/form-data`

- form field: `image` (binary file)

### Response Body

```json
{
  "status": "ok",
  "message": "목표 이미지가 업로드되었습니다.",
  "data": "plan/new.png"
}
```

### Error Responses

```json
{
  "status": "error",
  "message": "Authorization header is required",
  "errorCode": "MEMBER_008"
}
```

```json
{
  "status": "error",
  "message": "Plan not found",
  "errorCode": "PLAN_001"
}
```

### Notes

- Response 예시는 `PlanImageControllerTest` 기준이다.

---

## S3 파일 업로드 테스트

### 기능명

- S3 파일 업로드 테스트

### Method

- `POST`

### Full URL

- `https://j14d101.p.ssafy.io/api/test/s3/upload`

### Path

- `/api/test/s3/upload`

### Headers

- `Authorization: Bearer {accessToken}`
- `Content-Type: multipart/form-data`

### Path Variables

- 없음

### Query Parameters

- 없음

### Request Body

`multipart/form-data`

- form field: `file` (binary file)

### Response Body

```json
{
  "status": "ok",
  "message": "S3 업로드 테스트에 성공했습니다.",
  "data": "profile/test.png"
}
```

### Error Responses

```json
{
  "status": "error",
  "message": "Authorization header is required",
  "errorCode": "MEMBER_008"
}
```

```json
{
  "status": "error",
  "message": "Internal server error",
  "errorCode": "INTERNAL_SERVER_ERROR"
}
```

### Notes

- Response 예시는 `S3TestControllerTest` 기준이다.

---

## S3 업로드 파일 삭제 테스트

### 기능명

- S3 업로드 파일 삭제 테스트

### Method

- `DELETE`

### Full URL

- `https://j14d101.p.ssafy.io/api/test/s3?objectKey={objectKey}`

### Path

- `/api/test/s3`

### Headers

- `Authorization: Bearer {accessToken}`

### Path Variables

- 없음

### Query Parameters

- `objectKey` (String, required): 업로드 API가 반환한 S3 객체 키

### Request Body

- 없음

### Response Body

```json
{
  "status": "ok",
  "message": "S3 삭제 테스트에 성공했습니다.",
  "data": true
}
```

### Error Responses

```json
{
  "status": "error",
  "message": "Authorization header is required",
  "errorCode": "MEMBER_008"
}
```

```json
{
  "status": "error",
  "message": "Internal server error",
  "errorCode": "INTERNAL_SERVER_ERROR"
}
```

### Notes

- Response 예시는 `S3TestControllerTest` 기준이다.
