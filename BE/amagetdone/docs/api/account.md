# Account API Spec

Base URL: `https://j14d101.p.ssafy.io/api`

## 수시입출금 상품 등록

- 기능명: `수시입출금 상품 등록`
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/admin/accounts/products`
- Path: `/api/v1/admin/accounts/products`
- Headers:
  - `Authorization: Bearer {accessToken}`
  - `Content-Type: application/json`
- Path Variables: `없음`
- Query Parameters: `없음`
- Request Body:

```json
{
  "bankCode": "001",
  "accountName": "급여 통장",
  "accountDescription": "월급 수령용 수시입출금 상품"
}
```

- Response Body:

```json
{
  "status": "ok",
  "message": "수시입출금 상품을 등록했습니다.",
  "data": {
    "accountTypeUniqueNo": "001-1-abc",
    "bankCode": "001",
    "bankName": "한국은행",
    "accountTypeCode": "1",
    "accountTypeName": "수시입출금",
    "accountName": "급여 통장",
    "accountDescription": "월급 수령용 수시입출금 상품",
    "accountType": "DOMESTIC"
  }
}
```

- Error Responses:
  - `502 ACCOUNT_005`: Demand deposit product API call failed
  - `502 ACCOUNT_006`: Invalid demand deposit product API response
- Notes:
  - 컨트롤러 응답 메시지는 `"수시입출금 상품을 등록했습니다."`다.

## 수시입출금 상품 목록 조회

- 기능명: `수시입출금 상품 목록 조회`
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/admin/accounts/products`
- Path: `/api/v1/admin/accounts/products`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables: `없음`
- Query Parameters: `없음`
- Request Body: `없음`
- Response Body:

```json
{
  "status": "ok",
  "message": "수시입출금 상품 목록을 조회했습니다.",
  "data": [
    {
      "accountTypeUniqueNo": "001-1-abc",
      "bankCode": "001",
      "bankName": "한국은행",
      "accountTypeCode": "1",
      "accountTypeName": "수시입출금",
      "accountName": "급여 통장",
      "accountDescription": "월급 수령용 수시입출금 상품",
      "accountType": "DOMESTIC"
    }
  ]
}
```

- Error Responses:
  - `502 ACCOUNT_005`: Demand deposit product API call failed
  - `502 ACCOUNT_006`: Invalid demand deposit product API response
- Notes:
  - 관리자 API.

## 수시입출금 계좌 생성

- 기능명: `수시입출금 계좌 생성`
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/admin/accounts`
- Path: `/api/v1/admin/accounts`
- Headers:
  - `Authorization: Bearer {accessToken}`
  - `Content-Type: application/json`
- Path Variables: `없음`
- Query Parameters: `없음`
- Request Body:

```json
{
  "userId": 7,
  "accountTypeUniqueNo": "001-1-abc"
}
```

- Response Body:

```json
{
  "status": "ok",
  "message": "수시입출금 계좌를 생성했습니다.",
  "data": {
    "bankCode": "001",
    "accountNo": "0011234567890123",
    "currency": "KRW",
    "currencyName": "원"
  }
}
```

- Error Responses:
  - `404 ACCOUNT_002`: User not found
  - `400 ACCOUNT_003`: User key is required
  - `502 ACCOUNT_007`: Demand deposit account API call failed
  - `502 ACCOUNT_008`: Invalid demand deposit account API response
- Notes:
  - 관리자 API.

## Ssafy 은행 계좌 목록 조회 (연동 전)

- 기능명: `Ssafy 은행 계좌 목록 조회 (연동 전)`
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/accounts/bank`
- Path: `/api/v1/accounts/bank`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables: `없음`
- Query Parameters: `없음`
- Request Body: `없음`
- Response Body:

```json
{
  "status": "ok",
  "message": "은행 계좌 목록을 조회했습니다.",
  "data": [
    {
      "bankCode": "001",
      "bankName": "한국은행",
      "accountNo": "0011234567890123",
      "accountName": "주거래 입출금",
      "accountTypeName": "수시입출금",
      "accountBalance": 1250000,
      "currency": "KRW"
    }
  ]
}
```

- Error Responses:
  - `401 MEMBER_008`: Authorization header is required
  - `401 MEMBER_009`: Access token is required
  - `401 MEMBER_010`: Invalid access token
  - `404 ACCOUNT_002`: User not found
  - `400 ACCOUNT_003`: User key is required
  - `502 ACCOUNT_007`: Demand deposit account API call failed
  - `502 ACCOUNT_008`: Invalid demand deposit account API response
- Notes:
  - 로컬 DB 동기화 전, 금융망 계좌 원본 목록을 조회하는 용도.

## 내 계좌 연동 동기화

- 기능명: `내 계좌 연동 동기화`
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/accounts/me/sync`
- Path: `/api/v1/accounts/me/sync`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables: `없음`
- Query Parameters: `없음`
- Request Body:

```json
{}
```

- Response Body:

```json
{
  "status": "ok",
  "message": "내 계좌 목록을 동기화했습니다.",
  "data": [
    {
      "accountNo": "0011234567890123",
      "bankCode": "001",
      "bankName": "한국은행",
      "accountName": "주거래 입출금",
      "alias": null,
      "verified": false
    }
  ]
}
```

- Error Responses:
  - `401 MEMBER_008`: Authorization header is required
  - `401 MEMBER_009`: Access token is required
  - `401 MEMBER_010`: Invalid access token
  - `404 ACCOUNT_002`: User not found
  - `400 ACCOUNT_003`: User key is required
  - `502 ACCOUNT_007`: Demand deposit account API call failed
  - `502 ACCOUNT_008`: Invalid demand deposit account API response
- Notes:
  - 컨트롤러 단위 테스트에서 `Authorization` 헤더로 `userId`를 읽는 흐름을 검증한다.

## 단일 계좌 조회

- 기능명: `단일 계좌 조회`
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/accounts/{accountNo}`
- Path: `/api/v1/accounts/{accountNo}`
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
  "message": "계좌를 조회했습니다.",
  "data": {
    "accountNo": "0011234567890123",
    "bankCode": "001",
    "bankName": "한국은행",
    "accountName": "주거래 입출금",
    "alias": "생활비 통장",
    "verified": true
  }
}
```

- Error Responses:
  - `401 MEMBER_008`: Authorization header is required
  - `401 MEMBER_009`: Access token is required
  - `401 MEMBER_010`: Invalid access token
  - `404 ACCOUNT_004`: Account not found
- Notes:
  - 응답 스키마는 `AccountSummaryResponse`.

## 내 계좌 목록 조회

- 기능명: `내 계좌 목록 조회`
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/accounts/me`
- Path: `/api/v1/accounts/me`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables: `없음`
- Query Parameters: `없음`
- Request Body: `없음`
- Response Body:

```json
{
  "status": "ok",
  "message": "내 계좌 목록을 조회했습니다.",
  "data": [
    {
      "accountNo": "0011234567890123",
      "bankCode": "001",
      "bankName": "한국은행",
      "accountName": "주거래 입출금",
      "alias": "생활비 통장",
      "verified": true
    }
  ]
}
```

- Error Responses:
  - `401 MEMBER_008`: Authorization header is required
  - `401 MEMBER_009`: Access token is required
  - `401 MEMBER_010`: Invalid access token
- Notes:
  - 로컬 계좌 테이블 기준 조회.

## 계좌 잔액 조회

- 기능명: `계좌 잔액 조회`
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/accounts/{accountNo}/balance`
- Path: `/api/v1/accounts/{accountNo}/balance`
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
  "message": "계좌 잔액을 조회했습니다.",
  "data": {
    "accountNo": "0011234567890123",
    "accountBalance": 1250000,
    "accountCreatedDate": "2025-03-01",
    "accountExpiryDate": "2030-03-01",
    "lastTransactionDate": "2026-03-16",
    "currency": "KRW"
  }
}
```

- Error Responses:
  - `401 MEMBER_008`: Authorization header is required
  - `401 MEMBER_009`: Access token is required
  - `401 MEMBER_010`: Invalid access token
  - `404 ACCOUNT_002`: User not found
  - `400 ACCOUNT_003`: User key is required
  - `404 ACCOUNT_004`: Account not found
  - `502 ACCOUNT_009`: Demand deposit balance API call failed
  - `502 ACCOUNT_010`: Invalid demand deposit balance API response
- Notes:
  - 잔액은 금융 API 실시간 조회값.

## 계좌 입금

- 기능명: `계좌 입금`
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/accounts/deposit`
- Path: `/api/v1/accounts/deposit`
- Headers:
  - `Authorization: Bearer {accessToken}`
  - `Content-Type: application/json`
- Path Variables: `없음`
- Query Parameters: `없음`
- Request Body:

```json
{
  "accountNo": "0011234567890123",
  "transactionBalance": 50000,
  "transactionSummary": "월급 입금"
}
```

- Response Body:

```json
{
  "status": "ok",
  "message": "계좌 입금을 완료했습니다.",
  "data": {
    "transactionUniqueNo": 202603160000001,
    "transactionDate": "2026-03-16"
  }
}
```

- Error Responses:
  - `401 MEMBER_008`: Authorization header is required
  - `401 MEMBER_009`: Access token is required
  - `401 MEMBER_010`: Invalid access token
  - `404 ACCOUNT_002`: User not found
  - `400 ACCOUNT_003`: User key is required
  - `404 ACCOUNT_004`: Account not found
  - `502 ACCOUNT_011`: Demand deposit deposit API call failed
  - `502 ACCOUNT_012`: Invalid demand deposit deposit API response
- Notes:
  - Notion 기존 문구 `"완료되었습니다"`가 아니라 코드 기준 `"완료했습니다"`가 정확하다.

## 계좌 이체

- 기능명: `계좌 이체`
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/accounts/transfer`
- Path: `/api/v1/accounts/transfer`
- Headers:
  - `Authorization: Bearer {accessToken}`
  - `Content-Type: application/json`
- Path Variables: `없음`
- Query Parameters: `없음`
- Request Body:

```json
{
  "withdrawalAccountNo": "0011234567890123",
  "depositAccountNo": "0029876543210987",
  "transactionBalance": 30000,
  "withdrawalTransactionSummary": "친구 송금",
  "depositTransactionSummary": "친구에게 받은 돈"
}
```

- Response Body:

```json
{
  "status": "ok",
  "message": "계좌 이체를 완료했습니다.",
  "data": {
    "transactions": [
      {
        "transactionUniqueNo": 202603160000010,
        "accountNo": "0011234567890123",
        "transactionDate": "2026-03-16",
        "transactionType": "2",
        "transactionTypeName": "출금",
        "transactionAccountNo": "0029876543210987"
      },
      {
        "transactionUniqueNo": 202603160000011,
        "accountNo": "0029876543210987",
        "transactionDate": "2026-03-16",
        "transactionType": "1",
        "transactionTypeName": "입금",
        "transactionAccountNo": "0011234567890123"
      }
    ]
  }
}
```

- Error Responses:
  - `401 MEMBER_008`: Authorization header is required
  - `401 MEMBER_009`: Access token is required
  - `401 MEMBER_010`: Invalid access token
  - `404 ACCOUNT_002`: User not found
  - `400 ACCOUNT_003`: User key is required
  - `404 ACCOUNT_004`: Account not found
  - `502 ACCOUNT_013`: Demand deposit transfer API call failed
  - `502 ACCOUNT_014`: Invalid demand deposit transfer API response
- Notes:
  - 출금/입금 거래 2건을 배열로 반환.

## 계좌 거래내역 목록 조회

- 기능명: `계좌 거래내역 목록 조회`
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/v1/accounts/{accountNo}/transactions`
- Path: `/api/v1/accounts/{accountNo}/transactions`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables:
  - `accountNo: String`
- Query Parameters:
  - `startDate: String` (required, `yyyy-MM-dd`)
  - `endDate: String` (required, `yyyy-MM-dd`)
  - `transactionType: String` (optional, default `A`)
  - `orderByType: String` (optional, default `ASC`)
- Request Body: `없음`
- Response Body:

```json
{
  "status": "ok",
  "message": "계좌 거래내역을 조회했습니다.",
  "data": {
    "totalCount": 2,
    "transactions": [
      {
        "transactionUniqueNo": 202603150000001,
        "transactionDate": "2026-03-15",
        "transactionTime": "09:12:31",
        "transactionType": "1",
        "transactionTypeName": "입금",
        "transactionAccountNo": "0029876543210987",
        "transactionBalance": 50000,
        "transactionAfterBalance": 1250000,
        "transactionSummary": "월급 입금",
        "transactionMemo": "급여"
      },
      {
        "transactionUniqueNo": 202603150000002,
        "transactionDate": "2026-03-15",
        "transactionTime": "12:03:21",
        "transactionType": "2",
        "transactionTypeName": "출금",
        "transactionAccountNo": "0031111222233334",
        "transactionBalance": 12000,
        "transactionAfterBalance": 1238000,
        "transactionSummary": "점심 결제",
        "transactionMemo": "식비"
      }
    ]
  }
}
```

- Error Responses:
  - `401 MEMBER_008`: Authorization header is required
  - `401 MEMBER_009`: Access token is required
  - `401 MEMBER_010`: Invalid access token
  - `404 ACCOUNT_002`: User not found
  - `400 ACCOUNT_003`: User key is required
  - `404 ACCOUNT_004`: Account not found
- Notes:
  - 컨트롤러 기본값은 `transactionType=A`, `orderByType=ASC`.
