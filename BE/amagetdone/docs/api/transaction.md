# Transaction API

## 지출 요약 조회

### 기능명

- 지출 요약 조회

### Method

- `GET`

### Full URL

- `https://j14d101.p.ssafy.io/api/transactions/expenses/summary?fromMonth={fromMonth}&toMonth={toMonth}&bucket={bucket}`

### Path

- `/api/transactions/expenses/summary`

### Headers

- `Authorization: Bearer {accessToken}`

### Path Variables

- 없음

### Query Parameters

- `fromMonth` (String, optional, format: `yyyy-MM`)
- `toMonth` (String, optional, format: `yyyy-MM`)
- `bucket` (String, optional, example: `MONTH`)

### Request Body

- 없음

### Response Body

```json
{
  "status": "ok",
  "message": "지출 요약을 조회했습니다.",
  "data": {
    "range": {
      "fromMonth": "2025-10",
      "toMonth": "2026-03",
      "bucket": "MONTH"
    },
    "points": [
      {
        "label": "2026-03",
        "expenseAmount": 1900000,
        "fixedExpenseAmount": 650000,
        "variableExpenseAmount": 1250000
      }
    ],
    "comparison": {
      "latestAmount": 1900000,
      "previousAmount": 1720000,
      "changeAmount": 180000,
      "changeRate": 10.47
    },
    "lastAnalyzedAt": "2026-03-13T15:20:00"
  }
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

- Response 예시는 `TransactionControllerTest` 기준이다.

---

## 월 지출 조회

### 기능명

- 월 지출 조회

### Method

- `GET`

### Full URL

- `https://j14d101.p.ssafy.io/api/transactions/expenses/monthly?month={month}`

### Path

- `/api/transactions/expenses/monthly`

### Headers

- `Authorization: Bearer {accessToken}`

### Path Variables

- 없음

### Query Parameters

- `month` (String, optional, format: `yyyy-MM`)

### Request Body

- 없음

### Response Body

```json
{
  "status": "ok",
  "message": "월 지출을 조회했습니다.",
  "data": {
    "month": "2026-03",
    "totalExpenseAmount": 1900000,
    "fixedExpenseAmount": 650000,
    "variableExpenseAmount": 1250000,
    "excludedIncomeAmount": 2300000,
    "excludedInternalTransferAmount": 420000,
    "transactionCount": 84,
    "lastAnalyzedAt": "2026-03-13T15:20:00"
  }
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

### Notes

- Response 예시는 `TransactionControllerTest` 기준이다.

---

## 거래 내역 목록 조회

### 기능명

- 거래 내역 목록 조회

### Method

- `GET`

### Full URL

- `https://j14d101.p.ssafy.io/api/transactions?fromDate={fromDate}&toDate={toDate}&analysisInclusion={analysisInclusion}&page={page}&size={size}`

### Path

- `/api/transactions`

### Headers

- `Authorization: Bearer {accessToken}`

### Path Variables

- 없음

### Query Parameters

- `fromDate` (String, optional, format: `yyyy-MM-dd`)
- `toDate` (String, optional, format: `yyyy-MM-dd`)
- `analysisInclusion` (String, optional, example: `ALL`)
- `page` (Integer, optional)
- `size` (Integer, optional)

### Request Body

- 없음

### Response Body

```json
{
  "status": "ok",
  "message": "거래 내역을 조회했습니다.",
  "data": {
    "sync": {
      "status": "SUCCEEDED",
      "triggered": true,
      "syncedAt": "2026-03-13T15:18:11"
    },
    "page": 0,
    "size": 20,
    "totalElements": 143,
    "items": [
      {
        "transactionId": 10231,
        "sourceType": "CARD",
        "sourceId": "card-10231",
        "occurredAt": "2026-03-12T13:22:11",
        "merchantText": "Coffee Shop",
        "amount": 6100,
        "direction": "OUT",
        "category": {
          "id": 13,
          "name": "Cafe"
        },
        "expenseType": "VARIABLE",
        "analysisIncluded": true,
        "excludedReason": null
      }
    ]
  }
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

### Notes

- Response 예시는 `TransactionControllerTest` 기준이다.

---

## 분석 작업 생성

### 기능명

- 분석 작업 생성

### Method

- `POST`

### Full URL

- `https://j14d101.p.ssafy.io/api/transactions/analysis-jobs`

### Path

- `/api/transactions/analysis-jobs`

### Headers

- `Authorization: Bearer {accessToken}`
- `Content-Type: application/json`

### Path Variables

- 없음

### Query Parameters

- 없음

### Request Body

```json
{
  "months": 6,
  "forceFullSync": false,
  "relabel": false
}
```

### Response Body

```json
{
  "status": "ok",
  "message": "분석 작업을 접수했습니다.",
  "data": {
    "jobId": "txn-analysis-20260313-001",
    "status": "PENDING",
    "requestedMonths": 6,
    "requestedAt": "2026-03-13T15:19:00"
  }
}
```

### Error Responses

```json
{
  "status": "error",
  "message": "Transaction analysis is already running",
  "errorCode": "TRANSACTION_007"
}
```

```json
{
  "status": "error",
  "message": "Authorization header is required",
  "errorCode": "MEMBER_008"
}
```

### Notes

- 성공 시 HTTP status는 `202 Accepted`다.
- Response 예시는 `TransactionControllerTest` 기준이다.

---

## 분석 작업 상태 조회

### 기능명

- 분석 작업 상태 조회

### Method

- `GET`

### Full URL

- `https://j14d101.p.ssafy.io/api/transactions/analysis-jobs/{jobId}`

### Path

- `/api/transactions/analysis-jobs/{jobId}`

### Headers

- `Authorization: Bearer {accessToken}`

### Path Variables

- `jobId` (String): 분석 작업 ID

### Query Parameters

- 없음

### Request Body

- 없음

### Response Body

```json
{
  "status": "ok",
  "message": "분석 작업 상태를 조회했습니다.",
  "data": {
    "jobId": "txn-analysis-20260313-001",
    "status": "RUNNING",
    "progress": {
      "step": "CATEGORY_LABELING",
      "completedSteps": 3,
      "totalSteps": 4
    },
    "requestedMonths": 6,
    "requestedAt": "2026-03-13T15:19:00",
    "startedAt": "2026-03-13T15:19:02",
    "finishedAt": null,
    "errorCode": null,
    "errorMessage": null
  }
}
```

완료 예시(`onboardingResult` 포함):

```json
{
  "status": "ok",
  "message": "분석 작업 상태를 조회했습니다.",
  "data": {
    "jobId": "txn-analysis-20260313-001",
    "status": "SUCCEEDED",
    "onboardingResult": {
      "summaryText": "3개월 간 소비패턴을 분석했어요.",
      "averageIncome": 3000000,
      "averageExpense": 1900000,
      "remainingAmount": 1100000,
      "fixedExpenseRatio": 65.0,
      "variableExpenseRatio": 35.0,
      "insightMessage": "소비 여유가 있어요. 목표 저축도 고려해 보세요.",
      "categoryExpenses": [
        {
          "categoryId": 10,
          "categoryName": "식비",
          "amount": 750000
        }
      ]
    }
  }
}
```

### Error Responses

```json
{
  "status": "error",
  "message": "Transaction analysis job was not found",
  "errorCode": "TRANSACTION_008"
}
```

```json
{
  "status": "error",
  "message": "Authorization header is required",
  "errorCode": "MEMBER_008"
}
```

### Notes

- `onboardingResult`는 작업 상태에 따라 없을 수 있다.
- `TransactionControllerMockModeTest`에서는 `transaction.analysis-response-mode=mock`일 때 고정 응답을 반환한다.

---

## 주간 분석 조회

### 기능명

- 주간 분석 조회

### Method

- `GET`

### Full URL

- `https://j14d101.p.ssafy.io/api/transactions/analysis/weeks/{yearWeek}`

### Path

- `/api/transactions/analysis/weeks/{yearWeek}`

### Headers

- `Authorization: Bearer {accessToken}`

### Path Variables

- `yearWeek` (String, format: `yyyy-Www`, example: `2026-W10`)

### Query Parameters

- 없음

### Request Body

- 없음

### Response Body

```json
{
  "status": "ok",
  "message": "주간 분석을 조회했습니다.",
  "data": {
    "yearWeek": "2026-W10",
    "weekStartDate": "2026-03-02",
    "weekEndDate": "2026-03-08",
    "expenseAmount": 480000,
    "essentialAmount": 155000,
    "fixedEssentialAmount": 72000,
    "variableEssentialAmount": 83000,
    "categoryExpenses": [
      {
        "categoryId": 2,
        "categoryName": "Subscription",
        "amount": 17000
      }
    ],
    "lastAnalyzedAt": "2026-03-13T15:20:00"
  }
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

### Notes

- Response 예시는 `TransactionControllerTest` 기준이다.

---

## 월간 분석 조회

### 기능명

- 월간 분석 조회

### Method

- `GET`

### Full URL

- `https://j14d101.p.ssafy.io/api/transactions/analysis/months/{yearMonth}`

### Path

- `/api/transactions/analysis/months/{yearMonth}`

### Headers

- `Authorization: Bearer {accessToken}`

### Path Variables

- `yearMonth` (String, format: `yyyy-MM`, example: `2026-03`)

### Query Parameters

- 없음

### Request Body

- 없음

### Response Body

```json
{
  "status": "ok",
  "message": "월간 분석을 조회했습니다.",
  "data": {
    "yearMonth": "2026-03",
    "expenseAmount": 1900000,
    "monthlyEssentialAmount": 680000,
    "fixedEssentialAmount": 430000,
    "variableEssentialAmount": 250000,
    "categoryExpenses": [
      {
        "categoryId": 10,
        "categoryName": "Food",
        "amount": 750000,
        "ratio": 39.47
      }
    ],
    "lastAnalyzedAt": "2026-03-13T15:20:00"
  }
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

### Notes

- Response 예시는 `TransactionControllerTest` 기준이다.
