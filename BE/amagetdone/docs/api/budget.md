# Budget API

Base URL: `https://j14d101.p.ssafy.io/api`

## 1) 월급 저장

- 기능명: 월급 저장
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/budgets/income`
- Path: `/api/budgets/income`
- Headers:
  - `Authorization: Bearer {accessToken}`
  - `Content-Type: application/json`
- Path Variables: 없음
- Query Parameters: 없음
- Request Body:

```json
{
  "monthlyIncome": 3200000
}
```

- Response Body:

```json
{
  "status": "ok",
  "message": "월급을 저장했습니다.",
  "data": {
    "monthlyIncome": 3200000,
    "baseMonth": "2026-03-01"
  }
}
```

- Error Responses:
  - `401 Unauthorized` (인증 필요)
- Notes:
  - 응답 `data`는 `BudgetIncomeDto.Response`.

## 2) 월급 조회

- 기능명: 월급 조회
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/budgets/income`
- Path: `/api/budgets/income`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables: 없음
- Query Parameters: 없음
- Request Body: 없음
- Response Body:

```json
{
  "status": "ok",
  "message": "월급을 조회했습니다.",
  "data": {
    "monthlyIncome": 0,
    "baseMonth": "2026-03-01"
  }
}
```

- Error Responses:
  - `401 Unauthorized` (인증 필요)
- Notes:
  - 현재 월 수입이 없을 때 `monthlyIncome=0` 응답 케이스가 테스트돼 있다.

## 3) 예산 배분 실행

- 기능명: 예산 배분 실행
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/budgets/allocate`
- Path: `/api/budgets/allocate`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables: 없음
- Query Parameters: 없음
- Request Body: 없음
- Response Body:

```json
{
  "status": "ok",
  "message": "예산 배분을 실행했습니다.",
  "data": {
    "success": true
  }
}
```

- Error Responses:
  - `401 Unauthorized` (인증 필요)
- Notes:
  - 요청 본문 없이 실행된다.

## 4) 홈 대시보드 자유소비 잔액 조회

- 기능명: 홈 대시보드 자유소비 잔액 조회
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/budgets/home/dashboard`
- Path: `/api/budgets/home/dashboard`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables: 없음
- Query Parameters: 없음
- Request Body: 없음
- Response Body:

```json
{
  "status": "ok",
  "message": "홈 대시보드 자유소비 잔액을 조회했습니다.",
  "data": {
    "weekStartDate": "2026-03-09",
    "weekEndDate": "2026-03-15",
    "daysRemaining": 3,
    "finalFreeAmount": 120000,
    "actualSpentAmount": 45000,
    "remainingAmount": 75000,
    "dailyAvailable": 25000
  }
}
```

- Error Responses:
  - `401 Unauthorized` (인증 필요)
- Notes:
  - 응답 `data`는 `BudgetDashboardDto.Response`.

## 5) 홈 화면 통합 조회

- 기능명: 홈 화면 통합 조회
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/budgets/home`
- Path: `/api/budgets/home`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables: 없음
- Query Parameters: 없음
- Request Body: 없음
- Response Body:

```json
{
  "status": "ok",
  "message": "홈 화면 통합 정보를 조회했습니다.",
  "data": {
    "freeSpending": {
      "usedAmount": 45000,
      "totalAmount": 120000,
      "remainingAmount": 75000,
      "daysRemaining": 3,
      "dailyAvailable": 25000
    },
    "weeklySaving": {
      "thisWeekSavingAmount": 180000,
      "remainingToTargetAmount": 820000,
      "progressRate": 18
    },
    "assetSummary": {
      "totalAssetAmount": 5400000
    },
    "plans": [
      {
        "planId": 11,
        "title": "Travel Fund",
        "priority": 1,
        "dDay": 45,
        "savedAmount": 680000,
        "targetAmount": 1000000,
        "progressRate": 68,
        "imageUrl": "https://example.com/travel.png"
      }
    ]
  }
}
```

- Error Responses:
  - `401 Unauthorized` (인증 필요)
- Notes:
  - `weeklySaving.progressRate`, `plans[].progressRate`는 `long` 정수형이다.
  - 실수(`80.0`, `17.5`)가 아닌 정수 백분율로 응답된다.

## 6) 내 예산 요약 지표 조회

- 기능명: 내 예산 요약 지표 조회
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/budgets/me/summary`
- Path: `/api/budgets/me/summary`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables: 없음
- Query Parameters: 없음
- Request Body: 없음
- Response Body:

```json
{
  "status": "ok",
  "message": "내 예산 요약 지표를 조회했습니다.",
  "data": {
    "activePlanCount": 2,
    "completedPlanCount": 1,
    "totalSavedAmount": 450000
  }
}
```

- Error Responses:
  - `401 Unauthorized` (인증 필요)
- Notes:
  - 응답 `data`는 `BudgetSummaryDto.Response`.

## 7) 주간 예산 편성 통합 조회

- 기능명: 주간 예산 편성 통합 조회
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/budgets/weeks/{yearWeek}/setup`
- Path: `/api/budgets/weeks/{yearWeek}/setup`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables:
  - `yearWeek` (예: `2026-W10`)
- Query Parameters: 없음
- Request Body: 없음
- Response Body:

```json
{
  "status": "ok",
  "message": "주간 예산 편성 통합 정보를 조회했습니다.",
  "data": {
    "plannedIncomeAmount": 1200000,
    "plannedEssentialAmount": 300000,
    "plannedSavingAmount": 400000,
    "plannedFreeAmount": 500000,
    "finalFreeAmount": 450000,
    "actualSpentAmount": 120000,
    "remainingFreeAmount": 330000,
    "essentials": [
      {
        "categoryId": 2,
        "categoryName": "Subscription",
        "expenseScope": "FIXED",
        "plannedAmount": 70000,
        "actualSpentAmount": 65000
      }
    ],
    "savings": [
      {
        "planTitle": "Travel Fund",
        "priority": 1,
        "plannedAmount": 250000,
        "finalAmount": 230000
      }
    ]
  }
}
```

- Error Responses:
  - `401 Unauthorized` (인증 필요)
- Notes:
  - `plannedFreeAmount`, `remainingFreeAmount`는 응답 계산값이다.

## 8) 주간 예산 수동 조정 저장

- 기능명: 주간 예산 수동 조정 저장
- Method: `POST`
- Full URL: `https://j14d101.p.ssafy.io/api/budgets/weeks/{yearWeek}/adjustments`
- Path: `/api/budgets/weeks/{yearWeek}/adjustments`
- Headers:
  - `Authorization: Bearer {accessToken}`
  - `Content-Type: application/json`
- Path Variables:
  - `yearWeek` (예: `2026-W10`)
- Query Parameters: 없음
- Request Body:

```json
{
  "type": "FREE_BUDGET_ADD",
  "amount": 20000,
  "planId": null,
  "reason": "비상 자유비 보정"
}
```

- Response Body:

```json
{
  "status": "ok",
  "message": "주간 예산 수동 조정을 저장했습니다.",
  "data": {
    "yearWeek": "2026-W10",
    "adjustmentType": "FREE_BUDGET_ADD",
    "adjustmentAmount": 20000,
    "planId": null,
    "reason": "비상 자유비 보정",
    "manualAdjustmentAmount": 20000,
    "planFinalAmount": null,
    "finalFreeAmount": 170000,
    "budgetWeekStatus": "ADJUSTED"
  }
}
```

- Error Responses:
  - `400 Bad Request`

```json
{
  "status": "error",
  "errorCode": "INVALID_REQUEST"
}
```

  - `401 Unauthorized` (인증 필요)
- Notes:
  - `type`은 `BudgetAdjustmentType` enum 값을 사용한다.

## 9) 주간 저축비 상세 조회

- 기능명: 주간 저축비 상세 조회
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/budgets/weeks/{yearWeek}/savings`
- Path: `/api/budgets/weeks/{yearWeek}/savings`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables:
  - `yearWeek` (예: `2026-W10`)
- Query Parameters: 없음
- Request Body: 없음
- Response Body:

```json
{
  "status": "ok",
  "message": "주간 저축비 상세 정보를 조회했습니다.",
  "data": {
    "yearWeek": "2026-W10",
    "weekStartDate": "2026-03-02",
    "weekEndDate": "2026-03-08",
    "plannedSavingAmount": 400000,
    "finalSavingAmount": 350000,
    "savings": [
      {
        "planTitle": "Travel Fund",
        "priority": 1,
        "plannedAmount": 250000,
        "finalAmount": 230000,
        "savedAmount": 900000,
        "adjustment": {
          "count": 2,
          "latestAdjustmentType": "PLAN_SUBTRACT",
          "latestReason": "일정 변경"
        }
      }
    ]
  }
}
```

- Error Responses:
  - `401 Unauthorized` (인증 필요)
- Notes:
  - 조정 이력이 없는 목표는 `adjustment.count=0`으로 응답된다.

## 10) 주간 자유비 상세 조회

- 기능명: 주간 자유비 상세 조회
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/budgets/weeks/{yearWeek}/free-spending`
- Path: `/api/budgets/weeks/{yearWeek}/free-spending`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables:
  - `yearWeek` (예: `2026-W10`)
- Query Parameters: 없음
- Request Body: 없음
- Response Body:

```json
{
  "status": "ok",
  "message": "주간 자유비 상세 정보를 조회했습니다.",
  "data": {
    "yearWeek": "2026-W10",
    "weekStartDate": "2026-03-02",
    "weekEndDate": "2026-03-08",
    "allocatedAmount": 180000,
    "spentAmount": 55000,
    "remainingAmount": 125000,
    "transactions": [
      {
        "transactionId": 401,
        "sourceType": "CARD",
        "sourceId": "998877",
        "occurredAt": "2026-03-06T20:15:00",
        "merchantText": "Movie Theater",
        "amount": 22000,
        "category": {
          "id": 15,
          "name": "Leisure"
        },
        "expenseType": "VARIABLE",
        "analysisIncluded": false,
        "excludedReason": "NON_ESSENTIAL"
      }
    ]
  }
}
```

- Error Responses:
  - `401 Unauthorized` (인증 필요)
- Notes:
  - 거래 목록은 `occurredAt` 기준 최신순 조회가 기본이다.

## 11) 주간 필수비 조회

- 기능명: 주간 필수비 조회
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/budgets/weeks/{yearWeek}/essential-expenses`
- Path: `/api/budgets/weeks/{yearWeek}/essential-expenses`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables:
  - `yearWeek` (예: `2026-W10`)
- Query Parameters: 없음
- Request Body: 없음
- Response Body:

```json
{
  "status": "ok",
  "data": {
    "yearWeek": "2026-W10",
    "weekStartDate": "2026-03-02",
    "weekEndDate": "2026-03-08",
    "baseMonths": 3,
    "essentialAmount": 155000,
    "fixedEssentialAmount": 72000,
    "variableEssentialAmount": 83000,
    "fixedExpenseCategories": [
      {
        "categoryId": 2,
        "categoryName": "Subscription",
        "amount": 17000
      }
    ],
    "variableExpenseCategories": [
      {
        "categoryId": 10,
        "categoryName": "Food",
        "amount": 83000
      }
    ],
    "lastAnalyzedAt": "2026-03-13T15:20:00"
  }
}
```

- Error Responses:
  - `401 Unauthorized` (인증 필요)
- Notes:
  - 컨트롤러 메시지 문자열이 깨진 상태로 정의돼 있어 테스트에서는 메시지 검증을 하지 않는다.

## 12) 주간 필수비 상세 조회

- 기능명: 주간 필수비 상세 조회
- Method: `GET`
- Full URL: `https://j14d101.p.ssafy.io/api/budgets/weeks/{yearWeek}/essential-expenses/detail`
- Path: `/api/budgets/weeks/{yearWeek}/essential-expenses/detail`
- Headers:
  - `Authorization: Bearer {accessToken}`
- Path Variables:
  - `yearWeek` (예: `2026-W10`)
- Query Parameters: 없음
- Request Body: 없음
- Response Body:

```json
{
  "status": "ok",
  "message": "주간 필수비 상세 정보를 조회했습니다.",
  "data": {
    "yearWeek": "2026-W10",
    "weekStartDate": "2026-03-02",
    "weekEndDate": "2026-03-08",
    "plannedEssentialAmount": 155000,
    "actualSpentAmount": 72000,
    "remainingAmount": 83000,
    "fixedExpenseCategories": [
      {
        "categoryId": 2,
        "categoryName": "Subscription",
        "plannedAmount": 17000,
        "actualSpentAmount": 15000,
        "remainingAmount": 2000,
        "transactions": [
          {
            "transactionId": 301,
            "sourceType": "CARD",
            "sourceId": "998877",
            "occurredAt": "2026-03-03T09:30:00",
            "merchantText": "Netflix",
            "amount": 15000,
            "category": {
              "id": 2,
              "name": "Subscription"
            },
            "expenseType": "FIXED"
          }
        ]
      }
    ],
    "variableExpenseCategories": [
      {
        "categoryId": 10,
        "categoryName": "Food",
        "plannedAmount": 138000,
        "actualSpentAmount": 57000,
        "remainingAmount": 81000,
        "transactions": [
          {
            "transactionId": 302,
            "sourceType": "ACCOUNT",
            "sourceId": "ACCOUNT-1",
            "occurredAt": "2026-03-05T18:10:00",
            "merchantText": "Market",
            "amount": 57000,
            "category": {
              "id": 10,
              "name": "Food"
            },
            "expenseType": "VARIABLE"
          }
        ]
      }
    ]
  }
}
```

- Error Responses:
  - `401 Unauthorized` (인증 필요)
- Notes:
  - 카테고리별 상세와 거래 drill-down을 함께 제공한다.
