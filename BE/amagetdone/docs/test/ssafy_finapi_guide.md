# SSAFY 금융망 API 가이드 (AI 에이전트용)

> AI 에이전트가 SSAFY 교육용 금융망(계정계) API를 직접 호출하여 테스트할 때 필요한 정보를 정리한 문서입니다.
> 상세 API 명세는 [finapi/](../finapi/) 디렉토리를 참조하세요.

---

## 1. 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL | `https://finopenapi.ssafy.io/ssafy/api/v1` |
| HTTP 메서드 | **모든 API POST** |
| Content-Type | `application/json` |
| 인증 방식 | 요청 Body 내 `apiKey` + `userKey` (HTTP Header가 아님) |

---

## 2. 키 구조

### apiKey (앱 API 키)

- **환경변수**: `SSAFY_FIN_API_KEY`
- **실제 값**: `.env` 파일에서 확인 (예: `3eba20d5b7a4430bb53c505cce0c4f18`)
- **용도**: 프로젝트 단위 인증. 모든 API 요청에 필수
- **서버 설정**: `application.properties` → `ssafy.fin-api.api-key=${SSAFY_FIN_API_KEY:}`

### userKey (사용자 키)

- **형태**: UUID (예: `04fa9ec5-3e42-4847-84ae-34c855cdd1b6`)
- **용도**: 사용자 식별. 금융 API 호출 시 필수 (member 생성/조회 API는 예외)
- **발급**: `/member` API로 사용자 생성 시 응답에서 반환
- **조회**: `/member/search` API로 기존 사용자의 userKey 조회 가능
- **서버 저장 위치**: `User.userKey` 필드 (DB에 저장)

> **주의**: userKey는 API 키가 아니다. apiKey는 프로젝트 인증용이고, userKey는 사용자 식별용이다.

### 테스트 사용자 관련 환경변수

| 환경변수 | 용도 | 예시 값 |
|---------|------|---------|
| `SSAFY_FIN_API_KEY` | 계정계 apiKey (프로젝트 인증용) | `.env`에서 확인 |
| `TEST_EMAIL_PREFIX` | 테스트 사용자 이메일 prefix | `moa` |
| `TEST_EMAIL_DOMAIN` | 테스트 사용자 이메일 domain | `tester.com` |
| `TEST_SOCIAL_USER_API_KEY` | 테스트 사용자의 userKey | `.env`에서 확인 |

> **환경변수명 주의**: `TEST_SOCIAL_USER_API_KEY`는 변수명에 "API_KEY"가 들어있지만, 실제 저장하는 값은 **사용자 식별용 userKey (UUID)** 이다. 계정계의 apiKey(`SSAFY_FIN_API_KEY`)와 혼동하지 말 것.

---

## 3. 공통 Header 구조

member 생성/조회를 **제외한** 모든 금융 API는 요청 Body에 `Header` 객체를 포함해야 한다.

```json
{
  "Header": {
    "apiName": "{{API명 — URL 마지막 path}}",
    "transmissionDate": "{{YYYYMMDD — 오늘 날짜}}",
    "transmissionTime": "{{HHMMSS — 현재 시각, KST 기준, ±5분 허용}}",
    "institutionCode": "00100",
    "fintechAppNo": "001",
    "apiServiceCode": "{{apiName과 동일}}",
    "institutionTransactionUniqueNo": "{{YYYYMMDDHHMMSS + 6자리 난수, 총 20자리, 매 요청 고유}}",
    "apiKey": "{{SSAFY_FIN_API_KEY}}",
    "userKey": "{{사용자 userKey — 일부 API에서는 생략}}"
  }
}
```

### institutionTransactionUniqueNo 생성 규칙

- 형식: `YYYYMMDDHHMMSS` + 6자리 숫자 = **총 20자리**
- 예: `20260321143025482913`
- **매 요청마다 새 값 생성 필수** (중복 시 `H1007` 에러)
- 서버 구현: `SsafyApiRequestFactory`에서 `ThreadLocalRandom.nextInt(0, 1_000_000)`으로 생성

### userKey가 불필요한 API

- `inquireDemandDepositList` (수시입출금 상품 조회)
- `inquireCreditCardList` (카드 상품 조회)
- 이런 API에서는 Header에서 `userKey` 필드를 생략하면 된다

---

## 4. Member API (Header 객체 불필요)

Member 생성/조회는 공통 Header 없이, `apiKey`와 `userId`만 전달한다.

### 사용자 조회

```
POST https://finopenapi.ssafy.io/ssafy/api/v1/member/search
```

```json
{
  "userId": "moa1@tester.com",
  "apiKey": "{{SSAFY_FIN_API_KEY}}"
}
```

**성공 응답:**
```json
{
  "userId": "moa1@tester.com",
  "userName": "ssafy123",
  "institutionCode": "00100",
  "userKey": "04fa9ec5-3e42-4847-84ae-34c855cdd1b6",
  "created": "2026-03-01T12:00:00.000000+09:00",
  "modified": "2026-03-01T12:00:00.000000+09:00"
}
```

- 존재하지 않으면 `E4003` 에러

### 사용자 생성

```
POST https://finopenapi.ssafy.io/ssafy/api/v1/member/
```

```json
{
  "apiKey": "{{SSAFY_FIN_API_KEY}}",
  "userId": "moa1@tester.com"
}
```

- 이미 존재하면 `E4002` 에러
- 성공 시 `userKey`가 응답에 포함

---

## 5. 주요 금융 API 호출 예시

### 5-1. 수시입출금 상품 조회 (userKey 불필요)

```
POST https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositList
```

```json
{
  "Header": {
    "apiName": "inquireDemandDepositList",
    "transmissionDate": "20260321",
    "transmissionTime": "143025",
    "institutionCode": "00100",
    "fintechAppNo": "001",
    "apiServiceCode": "inquireDemandDepositList",
    "institutionTransactionUniqueNo": "20260321143025000001",
    "apiKey": "{{SSAFY_FIN_API_KEY}}"
  }
}
```

**응답 REC 예시:**
```json
{
  "accountTypeUniqueNo": "004-1-xxxxxxxxxx",
  "bankCode": "004",
  "bankName": "국민은행",
  "accountTypeCode": "1",
  "accountTypeName": "수시입출금",
  "accountName": "국민은행 입출금상품커피",
  "accountDescription": "...",
  "accountType": "DOMESTIC"
}
```

- 기본 선택: **"국민은행 입출금상품커피"** (`bankCode: "004"`, `accountName`에 "입출금상품커피" 포함)
- `accountTypeUniqueNo` 값을 계좌 생성에 사용

### 5-2. 계좌 생성

```
POST https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/createDemandDepositAccount
```

```json
{
  "Header": {
    "apiName": "createDemandDepositAccount",
    "transmissionDate": "20260321",
    "transmissionTime": "143026",
    "institutionCode": "00100",
    "fintechAppNo": "001",
    "apiServiceCode": "createDemandDepositAccount",
    "institutionTransactionUniqueNo": "20260321143026000001",
    "apiKey": "{{SSAFY_FIN_API_KEY}}",
    "userKey": "{{userKey}}"
  },
  "accountTypeUniqueNo": "{{상품조회에서 얻은 accountTypeUniqueNo}}"
}
```

**응답 REC:**
```json
{
  "bankCode": "004",
  "accountNo": "0041234567890123",
  "currency": {
    "currency": "KRW",
    "currencyName": "원화"
  }
}
```

- `accountNo`를 카드 발급 시 `withdrawalAccountNo`로 사용

### 5-3. 계좌 목록 조회

```
POST https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositAccountList
```

```json
{
  "Header": {
    "apiName": "inquireDemandDepositAccountList",
    "transmissionDate": "20260321",
    "transmissionTime": "143027",
    "institutionCode": "00100",
    "fintechAppNo": "001",
    "apiServiceCode": "inquireDemandDepositAccountList",
    "institutionTransactionUniqueNo": "20260321143027000001",
    "apiKey": "{{SSAFY_FIN_API_KEY}}",
    "userKey": "{{userKey}}"
  }
}
```

### 5-4. 카드 상품 조회 (userKey 불필요)

```
POST https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireCreditCardList
```

```json
{
  "Header": {
    "apiName": "inquireCreditCardList",
    "transmissionDate": "20260321",
    "transmissionTime": "143028",
    "institutionCode": "00100",
    "fintechAppNo": "001",
    "apiServiceCode": "inquireCreditCardList",
    "institutionTransactionUniqueNo": "20260321143028000001",
    "apiKey": "{{SSAFY_FIN_API_KEY}}"
  }
}
```

- 아무 상품의 `cardUniqueNo`를 선택

### 5-5. 카드 발급

```
POST https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/createCreditCard
```

```json
{
  "Header": {
    "apiName": "createCreditCard",
    "transmissionDate": "20260321",
    "transmissionTime": "143029",
    "institutionCode": "00100",
    "fintechAppNo": "001",
    "apiServiceCode": "createCreditCard",
    "institutionTransactionUniqueNo": "20260321143029000001",
    "apiKey": "{{SSAFY_FIN_API_KEY}}",
    "userKey": "{{userKey}}"
  },
  "cardUniqueNo": "{{카드 상품 조회에서 얻은 cardUniqueNo}}",
  "withdrawalAccountNo": "{{생성한 계좌의 accountNo}}",
  "withdrawalDate": "4"
}
```

> **withdrawalDate 주의**: 날짜가 아닌 **요일**이다.
> 월=1, 화=2, 수=3, 목=4, 금=5, 토=6, 일=7

**응답 REC:**
```json
{
  "cardNo": "1003622654847049",
  "cvc": "713",
  "cardUniqueNo": "1003-a139e9f23f1a4cc",
  "cardIssuerCode": "1003",
  "cardIssuerName": "롯데카드",
  "cardName": "디지로카 SEOUL",
  "cardExpiryDate": "20310321",
  "withdrawalAccountNo": "0041234567890123",
  "withdrawalDate": "4"
}
```

### 5-6. 내 카드 목록 조회

```
POST https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireSignUpCreditCardList
```

```json
{
  "Header": {
    "apiName": "inquireSignUpCreditCardList",
    "transmissionDate": "20260321",
    "transmissionTime": "143030",
    "institutionCode": "00100",
    "fintechAppNo": "001",
    "apiServiceCode": "inquireSignUpCreditCardList",
    "institutionTransactionUniqueNo": "20260321143030000001",
    "apiKey": "{{SSAFY_FIN_API_KEY}}",
    "userKey": "{{userKey}}"
  }
}
```

---

## 6. 에러 처리

에러는 HTTP 상태코드가 아닌 **응답 Body의 `Header.responseCode`**로 판별한다.

```json
{
  "Header": {
    "responseCode": "H1007",
    "responseMessage": "기관거래고유번호가 중복된 값입니다."
  }
}
```

### 자주 발생하는 에러

| 코드 | 의미 | 대응 |
|------|------|------|
| `H0000` | 정상 처리 | - |
| `H1007` | 거래고유번호 중복 | `institutionTransactionUniqueNo`의 난수 부분 변경 후 재시도 |
| `H1008` | apiKey 유효하지 않음 | `.env`의 `SSAFY_FIN_API_KEY` 확인 |
| `H1009` | userKey 유효하지 않음 | `/member/search`로 userKey 재조회 |
| `E4002` | 이미 존재하는 사용자 | `/member/search`로 기존 userKey 조회해서 사용 |
| `E4003` | 존재하지 않는 사용자 | `/member/`로 사용자 생성 |
| `A1003` | 계좌번호 유효하지 않음 | 계좌번호 재확인 |
| `A1019` | 없는 상품 | 상품 조회 API로 유효한 상품번호 재확인 |

---

## 7. 우리 서버와 계정계의 관계

| 우리 서버 | 계정계 |
|-----------|--------|
| `user.email` | `userId` |
| `user.userKey` (DB 필드) | `userKey` |
| `.env`의 `SSAFY_FIN_API_KEY` | `apiKey` |
| `institutionCode` | `"00100"` (고정) |
| `fintechAppNo` | `"001"` (고정) |

### 서버가 자동으로 처리하는 것

- **소셜 로그인 시**: 계정계에 `/member` 생성 또는 `/member/search` 조회 → `userKey`를 DB에 저장
- **1원 인증 성공 시**: 해당 계좌를 로컬 DB에 저장 + `SyncLinkedCardsUseCase` 실행하여 연결된 카드도 자동 저장
- **금융 API 호출 시**: `SsafyApiRequestFactory`에서 공통 Header 자동 생성 (날짜, 시각, 거래고유번호 등)

### AI 에이전트가 직접 해야 하는 것

1. 계정계에 해당 사용자의 계좌/카드가 **존재하는지 확인**
2. 없으면 계정계에 **직접 생성** (상품 조회 → 계좌 생성 → 카드 발급)
3. 이후 우리 서버 API를 [test_flow.md](./test_flow.md) 순서대로 호출

---

## 8. 상세 명세 참조

| 문서 | 내용 |
|------|------|
| [overview.md](../finapi/overview.md) | 전체 API 목록, 공통 Header 상세, 에러코드 전체 |
| [2_사용자로그인.md](../finapi/2_사용자로그인.md) | Member 생성/조회 상세 |
| [4_수시입출금.md](../finapi/4_수시입출금.md) | 수시입출금 계좌 API 상세 (상품 조회, 계좌 생성, 거래내역 등) |
| [6_카드.md](../finapi/6_카드.md) | 신용카드 API 상세 (상품 조회, 카드 발급, 결제 등) |
| [7_1원인증.md](../finapi/7_1원인증.md) | 1원 인증 상세 |
