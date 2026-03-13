# Account API Spec

## Overview

- Base path: `/api`
- Response wrapper: `ApiResponse<T>`
- User APIs require `Authorization: Bearer <accessToken>`

## Response Wrapper

```json
{
  "status": "ok",
  "message": "요청 성공 메시지",
  "data": {}
}
```

## 1. Admin API

### 1.1 수시입출금 상품 등록

- Method: `POST`
- Path: `/v1/admin/accounts/products`

Request

```json
{
  "bankCode": "001",
  "accountName": "데모 상품",
  "accountDescription": "데모용 수시입출금 상품"
}
```

Response `data`

```json
{
  "accountTypeUniqueNo": "001-1-abc",
  "bankCode": "001",
  "bankName": "한국은행",
  "accountTypeCode": "1",
  "accountTypeName": "수시입출금",
  "accountName": "데모 상품",
  "accountDescription": "데모용 수시입출금 상품",
  "accountType": "DOMESTIC"
}
```

### 1.2 수시입출금 상품 목록 조회

- Method: `GET`
- Path: `/v1/admin/accounts/products`

Response `data`

```json
[
  {
    "accountTypeUniqueNo": "001-1-abc",
    "bankCode": "001",
    "bankName": "한국은행",
    "accountTypeCode": "1",
    "accountTypeName": "수시입출금",
    "accountName": "데모 상품",
    "accountDescription": "데모용 수시입출금 상품",
    "accountType": "DOMESTIC"
  }
]
```

### 1.3 수시입출금 계좌 생성

- Method: `POST`
- Path: `/v1/admin/accounts`

Request

```json
{
  "userId": 1,
  "accountTypeUniqueNo": "001-1-abc"
}
```

Response `data`

```json
{
  "bankCode": "001",
  "accountNo": "0011234567890123",
  "currency": "KRW",
  "currencyName": "원화"
}
```

## 2. User API

### 2.1 내 계좌 연동 동기화

- Method: `POST`
- Path: `/v1/accounts/me/sync`

Response `data`

```json
[
  {
    "accountNo": "0011234567890123",
    "bankCode": "001",
    "bankName": "한국은행",
    "accountName": "데모 수시입출금",
    "alias": null,
    "verified": true
  }
]
```

### 2.2 내 계좌 목록 조회

- Method: `GET`
- Path: `/v1/accounts/me`

Response `data`

```json
[
  {
    "accountNo": "0011234567890123",
    "bankCode": "001",
    "bankName": "한국은행",
    "accountName": "데모 수시입출금",
    "alias": null,
    "verified": true
  }
]
```

### 2.3 계좌 잔액 조회

- Method: `GET`
- Path: `/v1/accounts/{accountNo}/balance`

Response `data`

```json
{
  "accountNo": "0011234567890123",
  "accountBalance": 2000000,
  "accountCreatedDate": "20260313",
  "accountExpiryDate": "20310313",
  "lastTransactionDate": "20260313",
  "currency": "KRW"
}
```

### 2.4 계좌 입금

- Method: `POST`
- Path: `/v1/accounts/deposit`

Request

```json
{
  "accountNo": "0011234567890123",
  "transactionBalance": 10000,
  "transactionSummary": "데모 입금"
}
```

Response `data`

```json
{
  "transactionUniqueNo": 59,
  "transactionDate": "20260313"
}
```

### 2.5 계좌 이체

- Method: `POST`
- Path: `/v1/accounts/transfer`

Request

```json
{
  "withdrawalAccountNo": "0011234567890123",
  "depositAccountNo": "0201234567890123",
  "transactionBalance": 10000,
  "withdrawalTransactionSummary": "출금(이체)",
  "depositTransactionSummary": "입금(이체)"
}
```

Response `data`

```json
{
  "transactions": [
    {
      "transactionUniqueNo": 61,
      "accountNo": "0011234567890123",
      "transactionDate": "20260313",
      "transactionType": "2",
      "transactionTypeName": "출금(이체)",
      "transactionAccountNo": "0201234567890123"
    },
    {
      "transactionUniqueNo": 62,
      "accountNo": "0201234567890123",
      "transactionDate": "20260313",
      "transactionType": "1",
      "transactionTypeName": "입금(이체)",
      "transactionAccountNo": "0011234567890123"
    }
  ]
}
```

### 2.6 계좌 거래내역 목록 조회

- Method: `GET`
- Path: `/v1/accounts/{accountNo}/transactions`

Query

- `startDate`: `YYYYMMDD`
- `endDate`: `YYYYMMDD`
- `transactionType`: `A | M | D`
- `orderByType`: `ASC | DESC`

Example

```text
/v1/accounts/{accountNo}/transactions?startDate=20260101&endDate=20261231&transactionType=A&orderByType=ASC
```

Response `data`

```json
{
  "totalCount": 2,
  "transactions": [
    {
      "transactionUniqueNo": 61,
      "transactionDate": "20260313",
      "transactionTime": "103229",
      "transactionType": "2",
      "transactionTypeName": "출금(이체)",
      "transactionAccountNo": "0201234567890123",
      "transactionBalance": 10000,
      "transactionAfterBalance": 1990000,
      "transactionSummary": "출금(이체)",
      "transactionMemo": ""
    }
  ]
}
```

## References

- Controller: `src/main/java/com/ssafy/amagetdone/domain/account/api/controller/AccountAdminController.java`
- Controller: `src/main/java/com/ssafy/amagetdone/domain/account/api/controller/AccountController.java`
- DTO: `src/main/java/com/ssafy/amagetdone/domain/account/api/dto/AccountAdminDto.java`
- DTO: `src/main/java/com/ssafy/amagetdone/domain/account/api/dto/AccountDto.java`
