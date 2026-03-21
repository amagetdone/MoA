# 소비 분석 프론트 영향 사항 정리

이 문서는 2026-03-21 세션에서 반영된 소비 분석 관련 변경 사항 중 프론트엔드에 직접 영향이 가는 항목만 정리한 문서입니다.

## 1. `/transactions/analysis` 요청 바디에서 `relabel` 필드가 제거되었습니다

### 변경 전
```json
{
  "months": 6,
  "relabel": false
}
```

### 변경 후
```json
{
  "months": 6
}
```

### 영향 범위
- `POST /api/transactions/analysis`
- 요청 DTO에서 `relabel` 필드가 제거되었습니다.

### 프론트 수정 사항
- 분석 실행 요청 body에서 `relabel` 필드를 제거해 주셔야 합니다.
- request model, serializer, mock payload, API 문서 예시에서도 `relabel`을 제거해 주셔야 합니다.

### 비고
- `relabel`은 기존 코드에서도 실제로 사용되지 않던 필드였습니다.

---

## 2. 특정 월 소비 분석 실행 엔드포인트가 추가되었습니다

### 신규 엔드포인트
- `POST /api/transactions/analysis/monthly?month=YYYY-MM`

### 요청 방식
- request body는 없습니다.
- query parameter `month`는 필수입니다.

### 요청 예시
```http
POST /api/transactions/analysis/monthly?month=2026-02
Authorization: Bearer {token}
```

### 응답 형식
- 기존 `POST /api/transactions/analysis`와 동일한 응답 구조를 사용합니다.

```json
{
  "status": "ok",
  "message": "해당 월 거래 분석을 완료했습니다.",
  "data": {
    "summaryText": "2026-02 소비 분석을 완료했어요.",
    "averageIncome": 2100000,
    "averageExpense": 1320000,
    "remainingAmount": 780000,
    "fixedExpenseRatio": 40.0,
    "variableExpenseRatio": 60.0,
    "insightMessage": "현재 소비 흐름은 안정적이에요.",
    "categoryExpenses": []
  }
}
```

### 응답 의미
- 해당 월 1개월만 분석하므로 `averageIncome`, `averageExpense`, `categoryExpenses.amount`는 실질적으로 그 달 총액처럼 해석하셔도 됩니다.
- `summaryText`는 `"YYYY-MM 소비 분석을 완료했어요."` 형식으로 내려옵니다.

### 프론트 수정 사항
- 월 선택 기반 분석 실행 시 기존 온보딩 분석 endpoint 대신 신규 endpoint를 호출해 주셔야 합니다.
- `month`는 body가 아니라 query parameter로 전달해 주셔야 합니다.
- 성공 메시지와 `summaryText`가 월 기준 문구로 내려오므로, 화면 copy 처리도 함께 확인해 주셔야 합니다.

---

## 3. 주간 소비분석 조회 파라미터와 응답 형식이 변경되었습니다

### 변경 대상
- `GET /api/transactions/analysis/weeks/{yearWeek}`

### 변경 전 형식
- 요청: `YYYY-Www`
- 응답 `data.yearWeek`: `YYYY-Www`

예시:
- `2026-W10`

### 변경 후 형식
- 요청: `YYYY-MM-w`
- 응답 `data.yearWeek`: `YYYY-MM-w`

예시:
- `2026-03-2`
- `2026-03-4`

### 요청 예시
```http
GET /api/transactions/analysis/weeks/2026-03-4
Authorization: Bearer {token}
```

### 응답 예시
```json
{
  "status": "ok",
  "message": "주간 분석 결과를 조회했습니다.",
  "data": {
    "yearWeek": "2026-03-4",
    "weekStartDate": "2026-03-23",
    "weekEndDate": "2026-03-29",
    "expenseAmount": 0,
    "essentialAmount": 0,
    "fixedEssentialAmount": 0,
    "variableEssentialAmount": 0,
    "categoryExpenses": [],
    "lastAnalyzedAt": null
  }
}
```

### 주차 해석 기준
- `w`는 해당 월의 월요일 시작 기준 주차입니다.
- 예를 들어 `2026-03-4`는 2026년 3월의 4번째 월요일 시작 주간을 의미합니다.

### 백엔드 내부 처리
- API 경계에서는 `YYYY-MM-w` 형식을 사용합니다.
- 내부 snapshot 저장 및 조회 키는 기존 ISO week 형식인 `YYYY-Www`를 그대로 유지합니다.
- 따라서 프론트에서는 새 형식에 맞춰 요청과 응답 처리만 변경해 주시면 됩니다.

### 프론트 수정 사항
- 주간 분석 조회 path parameter 생성 로직을 `YYYY-Www`에서 `YYYY-MM-w`로 변경해 주셔야 합니다.
- 응답 `data.yearWeek`를 파싱하는 UI 및 상태 모델도 새 형식 기준으로 수정해 주셔야 합니다.
- 기존에 `W10`, `W12` 같은 문자열 표시 또는 가공 로직이 있다면 함께 수정해 주셔야 합니다.

---

## 프론트 체크리스트

- `/transactions/analysis` 요청 body에서 `relabel` 필드를 제거해 주세요.
- 월별 분석 실행 시 `/transactions/analysis/monthly?month=YYYY-MM` endpoint를 사용해 주세요.
- 주간 분석 조회 path parameter를 `YYYY-MM-w` 형식으로 생성해 주세요.
- 주간 분석 응답의 `data.yearWeek` 표시 및 파싱 로직을 `YYYY-MM-w` 기준으로 수정해 주세요.
- API mock, fixture, Swagger example 기반 프론트 테스트 데이터도 함께 갱신해 주세요.
