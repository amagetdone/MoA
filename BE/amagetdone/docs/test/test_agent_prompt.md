# 통합 테스트 에이전트 프롬프트

아래 프롬프트를 AI 에이전트에게 전달하여 통합 테스트를 실행합니다.

---

## 프롬프트

```
당신은 금융 서비스 API 통합 테스트를 수행하는 에이전트입니다.
신규 사용자를 생성하고, 계좌/카드를 준비한 뒤, 과거 기준 목표 3개를 세워 backfill로 진행도 33%/66%/100% 상태를 만드는 것이 최종 목표입니다.

모든 API 호출은 curl 또는 python으로 수행하세요. 각 단계의 응답을 파싱하여 다음 단계에 필요한 값을 추출하세요.
에러가 발생하면 응답 Body의 responseCode를 확인하고, 아래 에러 대응 표에 따라 처리하세요.

---

## 환경 변수

아래 값을 사용하세요. .env 파일 경로: BE/amagetdone/.env

| 변수 | 값 |
|------|-----|
| SSAFY_FIN_API_KEY | .env 파일에서 `SSAFY_FIN_API_KEY` 값을 읽어 사용 |
| TEST_EMAIL_PREFIX | .env 파일에서 `TEST_EMAIL_PREFIX` 값을 읽어 사용 |
| TEST_EMAIL_DOMAIN | .env 파일에서 `TEST_EMAIL_DOMAIN` 값을 읽어 사용 |
| TEST_SOCIAL_USER_API_KEY | .env 파일에서 `TEST_SOCIAL_USER_API_KEY` 값을 읽어 사용 (이것은 userKey임) |
| TEST_SOCIAL_USER_NICKNAME | .env 파일에서 `TEST_SOCIAL_USER_NICKNAME` 값을 읽어 사용 |

---

## Base URL

| 대상 | URL |
|------|-----|
| 계정계 (SSAFY 금융망) | https://finopenapi.ssafy.io/ssafy/api/v1 |
| 우리 서버 | https://j14d101.p.ssafy.io/api |

## 우리 서버 API 경로 매핑

| 도메인 | Base Path | 비고 |
|--------|-----------|------|
| 인증 | `/v1/auth` | |
| 프로필 | `/v1/members` | PATCH 메서드 |
| 계좌/인증 | `/v1/accounts` | 금융망 조회: `/v1/accounts/bank`, 연동 계좌: `/v1/accounts/me` |
| 거래내역 | `/transactions` | 버전 prefix 없음 |
| 예산 | `/budgets` | 버전 prefix 없음 |
| 테스트 예산 | `/test/budget` | 버전 prefix 없음 |
| 목표 | `/v1/plans` | |

## 우리 서버 응답 형식

모든 응답은 아래 래퍼 구조를 따릅니다:
```json
{
  "status": "ok",
  "message": "...",
  "data": { ... }
}
```
- 성공 여부: `status == "ok"` && HTTP 2xx
- 실제 데이터는 `data` 필드 안에 있음

---

## 계정계 공통 Header 생성 규칙

계정계 금융 API 호출 시 (member API 제외) 요청 Body에 아래 형태의 Header 객체를 포함하세요:

```json
{
  "Header": {
    "apiName": "{{API명 — URL 마지막 path segment}}",
    "transmissionDate": "{{YYYYMMDD — 오늘 날짜}}",
    "transmissionTime": "{{HHMMSS — 현재 KST 시각}}",
    "institutionCode": "00100",
    "fintechAppNo": "001",
    "apiServiceCode": "{{apiName과 동일한 값}}",
    "institutionTransactionUniqueNo": "{{YYYYMMDDHHMMSS + 6자리 난수 = 총 20자리, 매 요청마다 새 값}}",
    "apiKey": "{{SSAFY_FIN_API_KEY}}",
    "userKey": "{{userKey — 일부 API에서는 생략}}"
  }
}
```

- `institutionTransactionUniqueNo`는 매 요청마다 반드시 새 값을 생성하세요.
- userKey가 불필요한 API: `inquireDemandDepositList`, `inquireCreditCardList`

---

## 에러 대응

| 코드 | 의미 | 대응 |
|------|------|------|
| H0000 | 정상 | 계속 진행 |
| H1007 | 거래고유번호 중복 | 난수 부분 변경 후 재시도 |
| E4002 | 이미 존재하는 사용자 | /member/search로 userKey 조회 |
| E4003 | 존재하지 않는 사용자 | /member/로 사용자 생성 |

---

## 실행 순서

### Preflight: 테스트 사용자 리셋 (권장)

> 기존 목표/예산 데이터가 남아있으면 예산이 분산되어 진행도가 기대치와 달라집니다.
> 깨끗한 상태에서 시작하려면 리셋 API를 먼저 호출하세요.
> 리셋 후에는 Phase 2(계좌 연동)부터 다시 필요합니다.

```
POST https://j14d101.p.ssafy.io/api/v1/auth/social/test/reset
```
- 이 API는 기존 목표, 계좌 연동, 예산 데이터를 모두 초기화하고 사용자 상태를 PENDING으로 되돌림
- 실패해도 진행 가능 (기존 데이터 위에 추가됨)

---

### Phase 0: 계정계 사전 준비

**0-1. 사용자 검색**
```
POST https://finopenapi.ssafy.io/ssafy/api/v1/member/search
Body: { "userId": "{{TEST_EMAIL_PREFIX}}1@{{TEST_EMAIL_DOMAIN}}", "apiKey": "{{SSAFY_FIN_API_KEY}}" }
```
- 존재하면: 응답의 `userKey`를 저장
- E4003이면: 0-1-1에서 생성

**0-1-1. 사용자 생성 (없을 경우)**
```
POST https://finopenapi.ssafy.io/ssafy/api/v1/member/
Body: { "apiKey": "{{SSAFY_FIN_API_KEY}}", "userId": "{{TEST_EMAIL_PREFIX}}1@{{TEST_EMAIL_DOMAIN}}" }
```
- 응답의 `userKey`를 저장

**0-2. 수시입출금 상품 조회** (userKey 불필요)
```
POST https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositList
```
- 응답 REC에서 `bankCode: "004"` + accountName에 "입출금상품커피" 포함하는 상품의 `accountTypeUniqueNo`를 저장

**0-3. 계좌 목록 조회**
```
POST https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositAccountList
```
- 계좌가 있으면: `accountNo`를 저장하고 0-5로 이동
- 없으면: 0-4에서 생성

**0-4. 계좌 생성** (계좌가 없을 경우)
```
POST https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/createDemandDepositAccount
Body: { "Header": {...}, "accountTypeUniqueNo": "{{0-2에서 얻은 값}}" }
```
- 응답 `REC.accountNo`를 저장

**0-5. 카드 목록 조회**
```
POST https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireSignUpCreditCardList
```
- 카드가 있으면: Phase 1로 이동
- 없으면: 0-6, 0-7 실행

**0-6. 카드 상품 조회** (userKey 불필요)
```
POST https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireCreditCardList
```
- 아무 상품의 `cardUniqueNo`를 저장

**0-7. 카드 발급**
```
POST https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/createCreditCard
Body: { "Header": {...}, "cardUniqueNo": "{{0-6에서 얻은 값}}", "withdrawalAccountNo": "{{accountNo}}", "withdrawalDate": "4" }
```

---

### Phase 1: 인증 & 프로필

**1-1. 테스트 소셜 로그인**
```
POST https://j14d101.p.ssafy.io/api/v1/auth/social/test
Body: { "id": 4 }
```
- 응답의 `data.accessToken`을 저장
- `data.status`가 `"ACTIVE"` 또는 `"PENDING"`
- **이후 모든 우리 서버 요청에 `Authorization: Bearer {{accessToken}}` 헤더 추가**

**1-2. 프로필 완성**
```
PATCH https://j14d101.p.ssafy.io/api/v1/members/me/profile
Body: {
  "nickname": "{{TEST_SOCIAL_USER_NICKNAME}}",
  "email": "{{TEST_EMAIL_PREFIX}}1@{{TEST_EMAIL_DOMAIN}}",
  "birthDate": "1995-06-15",
  "hasFixedIncome": true
}
```

---

### Phase 2: 계좌 연동 & 1원 인증

**2-1. 계좌 목록 조회 (금융망 계좌)**
```
GET https://j14d101.p.ssafy.io/api/v1/accounts/bank
```

**2-2. 1원 인증 요청**
```
POST https://j14d101.p.ssafy.io/api/v1/accounts/verification/requests
Body: { "accountNo": "{{accountNo}}" }
```
- 응답의 `data.verificationId`, `data.authText` 메모

**2-3. 인증코드 확인** — 계정계에서 거래내역 조회하여 입금자명에서 인증코드 추출
```
POST https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireTransactionHistoryList
Body: { "Header": {...}, "accountNo": "{{accountNo}}", "startDate": "{{7일전 YYYYMMDD}}", "endDate": "{{오늘 YYYYMMDD}}", "transactionType": "A", "orderByType": "DESC" }
```
- 가장 최근 입금 거래의 입금자명(transactionSummary 또는 transactionMemo)에서 4자리 이상 숫자를 추출

**2-4. 1원 인증 확인**
```
POST https://j14d101.p.ssafy.io/api/v1/accounts/verification/check
Body: { "accountNo": "{{accountNo}}", "authCode": "{{인증코드}}" }
```

---

### Phase 3: 거래내역 동기화 & 분석

**3-1. 거래내역 동기화**
```
POST https://j14d101.p.ssafy.io/api/transactions/sync
Body: { "months": 6, "forceFullSync": true }
```

**3-2. 거래 분석 실행**
```
POST https://j14d101.p.ssafy.io/api/transactions/analysis
Body: { "months": 6 }
```

---

### Phase 4: 예산 설정

**4-1. 수입 등록**
```
POST https://j14d101.p.ssafy.io/api/budgets/income
Body: { "monthlyIncome": 3000000 }
```

**4-2. 예산 배분**
```
POST https://j14d101.p.ssafy.io/api/budgets/allocate
```

---

### Phase 5: 과거 기준 목표 3개 생성 & Backfill

> 핵심: 과거 시작일로 목표를 만들고, backfill API로 과거 저축 적립 데이터를 한번에 채운다.
> 오늘 날짜를 기준으로 3개월 전, 2개월 전, 1개월 전 시작일을 동적으로 계산하세요.

**날짜 계산 (오늘이 YYYY-MM-DD라고 할 때)**

| 목표 | 시작일 | 종료일 | 예상 진행도 |
|------|--------|--------|-------------|
| A | 오늘 - 3개월 | 오늘 | ~100% |
| B | 오늘 - 2개월 | 오늘 + 1개월 | ~66% |
| C | 오늘 - 1개월 | 오늘 + 2개월 | ~33% |

**5-1. 목표 A 생성**
```
POST https://j14d101.p.ssafy.io/api/v1/plans
Body: {
  "title": "맥북 프로 구매",
  "targetAmount": 900000,
  "startDate": "{{오늘 - 3개월}}",
  "endDate": "{{오늘}}",
  "priority": 1
}
```
- 응답의 `data.planId` 저장 → planId_A

**5-2. 목표 B 생성**
```
POST https://j14d101.p.ssafy.io/api/v1/plans
Body: {
  "title": "여행 자금 모으기",
  "targetAmount": 600000,
  "startDate": "{{오늘 - 2개월}}",
  "endDate": "{{오늘 + 1개월}}",
  "priority": 2
}
```
- 응답의 `data.planId` 저장 → planId_B

**5-3. 목표 C 생성**
```
POST https://j14d101.p.ssafy.io/api/v1/plans
Body: {
  "title": "자격증 교재 구매",
  "targetAmount": 300000,
  "startDate": "{{오늘 - 1개월}}",
  "endDate": "{{오늘 + 2개월}}",
  "priority": 3
}
```
- 응답의 `data.planId` 저장 → planId_C

**5-4. Backfill 실행 (1회만!)**

> **중요**: backfill은 **반드시 1회만** 호출하세요.
> backfill은 해당 유저의 **모든 목표를 한번에** 채웁니다 (응답의 targetType이 "ALL").
> 여러 번 호출하면 이중 적립이나 500 에러가 발생합니다.

```
POST https://j14d101.p.ssafy.io/api/test/budget/savings-accrual/backfill
```
- 응답 확인: `data.operationType == "BACKFILL"`, `data.targetType == "ALL"`
- 응답의 `data.plans` 배열에 **모든 목표**가 포함됨
- 각 목표의 `savedAmount` / `targetAmount` 비율로 진행도 확인:
  - 목표 A: savedAmount ≈ targetAmount (~100%)
  - 목표 B: savedAmount ≈ targetAmount × 2/3 (~66%)
  - 목표 C: savedAmount ≈ targetAmount × 1/3 (~33%)

---

### Phase 6: 결과 검증

**6-1. 홈 화면 확인**
```
GET https://j14d101.p.ssafy.io/api/budgets/home
```
- 3개 목표의 진행 상태 확인

**6-2. 목표 목록 확인**
```
GET https://j14d101.p.ssafy.io/api/v1/plans
```
- 각 목표의 `savedAmount` / `targetAmount` 비율로 진행도 검증

---

## 실행 규칙

1. **순서 엄수**: Preflight → Phase 0 → 1 → 2 → 3 → 4 → 5 → 6 순서를 반드시 지키세요.
2. **응답 파싱**: 모든 우리 서버 응답은 `{ status, data }` 래퍼 구조입니다. 실제 값은 `data` 안에 있습니다.
3. **에러 처리**: 에러 발생 시 에러 대응 표를 참고하여 자동 복구를 시도하세요. 3회 재시도 후에도 실패하면 해당 단계에서 중단하고 에러를 보고하세요.
4. **조건 분기**: "이미 존재하면 건너뛰기" 로직을 준수하세요 (계좌가 이미 있으면 생성 스킵 등).
5. **날짜 동적 계산**: 오늘 날짜를 기준으로 시작일/종료일을 동적으로 계산하세요. 하드코딩하지 마세요.
6. **Backfill 1회 제한**: Phase 5의 backfill API는 반드시 **1회만** 호출하세요. 전체 목표를 한번에 처리합니다.
7. **결과 리포트**: 모든 단계 완료 후 각 Phase의 성공/실패 여부와 최종 진행도 수치를 요약 보고하세요.
```
