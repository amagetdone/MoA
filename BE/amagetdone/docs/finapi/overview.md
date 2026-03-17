# SSAFY 교육용 금융망 API — Overview

> 이 문서는 전체 9개 파일의 핵심 내용을 한 페이지로 요약한 진입점입니다.
> 각 섹션에 원본 파일 링크가 있으며, 세부 명세는 해당 파일을 참조하세요.
>
> **Base URL:** `https://finopenapi.ssafy.io/ssafy/api/v1`  
> **HTTP Method:** 모든 API `POST`

---

## 목차

- [SSAFY 교육용 금융망 API — Overview](#ssafy-교육용-금융망-api--overview)
  - [목차](#목차)
  - [1. 인증 플로우 (시작 전 필독)](#1-인증-플로우-시작-전-필독)
  - [2. 공통 Header 구조](#2-공통-header-구조)
    - [요청 Header](#요청-header)
    - [요청 예시](#요청-예시)
    - [응답 Header 추가 필드](#응답-header-추가-필드)
  - [3. 도메인별 API 목록](#3-도메인별-api-목록)
    - [MANAGER — 키 발급](#manager--키-발급)
    - [MEMBER — 사용자 계정](#member--사용자-계정)
    - [COMMON — 공통 코드](#common--공통-코드)
    - [DEMAND DEPOSIT — 수시입출금](#demand-deposit--수시입출금)
      - [원화 수시입출금](#원화-수시입출금)
      - [외화 수시입출금 (prefix: `/edu/demandDeposit/foreignCurrency/`)](#외화-수시입출금-prefix-edudemanddepositforeigncurrency)
    - [DEPOSIT — 예금](#deposit--예금)
    - [SAVINGS — 적금](#savings--적금)
    - [LOAN — 대출](#loan--대출)
    - [CREDIT CARD — 신용카드](#credit-card--신용카드)
    - [AUTH — 1원 인증](#auth--1원-인증)
    - [EXCHANGE — 환율/환전](#exchange--환율환전)
    - [MEMO — 거래내역 메모](#memo--거래내역-메모)
  - [4. 공통 에러코드](#4-공통-에러코드)
    - [Header 에러 (H코드) — 공통 Header 검증 실패](#header-에러-h코드--공통-header-검증-실패)
    - [계정/API KEY 에러 (E코드)](#계정api-key-에러-e코드)
    - [계좌/인증 에러 (A코드)](#계좌인증-에러-a코드)
    - [일반 에러 (Q코드)](#일반-에러-q코드)
  - [5. 파일 인덱스](#5-파일-인덱스)

---

## 1. 인증 플로우 (시작 전 필독)

API를 사용하기 위해 반드시 아래 순서를 따릅니다.

```
[Step 1] 앱 관리자 API KEY 발급
  POST /edu/app/issuedApiKey
  Body: { "managerId": "your@email.com" }
  → apiKey 반환 (이후 모든 요청 Header에 포함)

[Step 2] 사용자 계정 생성 (사용자별 1회)
  POST /member
  Body: { "apiKey": "...", "userId": "user@email.com" }
  → userKey 반환 (이후 모든 요청 Header에 포함)

[Step 3] 이후 모든 API 호출 시 공통 Header 포함
  → 섹션 2 참조
```

> **재발급:** `POST /edu/app/reIssuedApiKey` — 기존 키 무효화 후 신규 발급

상세: [1_APP관리자 키발급.md](./1_APP관리자%20키발급.md) · [2_사용자로그인.md](./2_사용자로그인.md)

---

## 2. 공통 Header 구조

모든 금융 API는 요청/응답 Body에 `Header` 객체를 포함합니다.  
`userKey`는 사용자 인증이 필요없는 일부 API(`inquireBankCodes` 등)에서 생략됩니다.

### 요청 Header

| 필드 | 타입 | 길이 | 설명 | 고정값 |
|---|---|---|---|---|
| `apiName` | String | 30 | 호출 API URI 마지막 path 명 | - |
| `transmissionDate` | String | 8 | 전송일자 `YYYYMMDD` | - |
| `transmissionTime` | String | 6 | 전송시각 `HHMMSS` (±5분 허용) | - |
| `institutionCode` | String | 5 | 기관코드 | `"00100"` |
| `fintechAppNo` | String | 3 | 핀테크 앱 일련번호 | `"001"` |
| `apiServiceCode` | String | 30 | `apiName`과 동일 | - |
| `institutionTransactionUniqueNo` | String | 20 | 기관거래고유번호 (매 요청 고유) | `YYYYMMDD+HHMMSS+6자리` |
| `apiKey` | String | 40 | Step 1에서 발급받은 API KEY | - |
| `userKey` | String | 40 | Step 2에서 발급받은 USER KEY | - |

### 요청 예시

```json
{
  "Header": {
    "apiName": "inquireDemandDepositAccountList",
    "transmissionDate": "20240101",
    "transmissionTime": "121212",
    "institutionCode": "00100",
    "fintechAppNo": "001",
    "apiServiceCode": "inquireDemandDepositAccountList",
    "institutionTransactionUniqueNo": "20240101121212000001",
    "apiKey": "<REDACTED>",
    "userKey": "<REDACTED>"
  }
}
```

### 응답 Header 추가 필드

| 필드 | 설명 |
|---|---|
| `responseCode` | 정상: `H0000` |
| `responseMessage` | 정상: `정상처리 되었습니다.` |

상세: [3_공통.md](./3_공통.md)

---

## 3. 도메인별 API 목록

### MANAGER — 키 발급

> 상세: [1_APP관리자 키발급.md](./1_APP관리자%20키발급.md)

| API 명 | URL (Base 이하) | 주요 파라미터 |
|---|---|---|
| API KEY 발급 | `/edu/app/issuedApiKey` | `managerId` |
| API KEY 재발급 | `/edu/app/reIssuedApiKey` | `managerId` |

---

### MEMBER — 사용자 계정

> 상세: [2_사용자로그인.md](./2_사용자로그인.md)

| API 명 | URL (Base 이하) | 주요 파라미터 |
|---|---|---|
| 계정 생성 | `/member` | `apiKey`, `userId` |
| 계정 조회 | `/member/search` | `userId`, `apiKey` |

---

### COMMON — 공통 코드

> 상세: [3_공통.md](./3_공통.md)

| API 명 | URL (Base 이하) | 응답 |
|---|---|---|
| 은행코드 조회 | `/edu/bank/inquireBankCodes` | `bankCode`, `bankName` 목록 |
| 통화코드 조회 | `/edu/bank/inquireBankCurrency` | `currency`, `currencyName` 목록 |

**주요 은행코드:** `001` 한국은행 / `004` 국민 / `020` 우리 / `081` KEB하나 / `088` 신한 / `090` 카카오 / `999` 싸피은행  
**주요 통화코드:** `KRW` / `USD` / `EUR` / `JPY` / `CNY` / `GBP`

---

### DEMAND DEPOSIT — 수시입출금

> 상세: [4_수시입출금.md](./4_수시입출금.md) — 원화 / 외화 두 종류 존재

#### 원화 수시입출금

| API 명 | URL (Base 이하) |
|---|---|
| 상품 등록 | `/edu/demandDeposit/createDemandDeposit` |
| 상품 조회 | `/edu/demandDeposit/inquireDemandDepositList` |
| 계좌 생성 | `/edu/demandDeposit/createDemandDepositAccount` |
| 계좌 목록 조회 | `/edu/demandDeposit/inquireDemandDepositAccountList` |
| 계좌 조회 (단건) | `/edu/demandDeposit/inquireDemandDepositAccount` |
| 예금주 조회 | `/edu/demandDeposit/inquireDemandDepositAccountHolderName` |
| 잔액 조회 | `/edu/demandDeposit/inquireDemandDepositAccountBalance` |
| 출금 | `/edu/demandDeposit/updateDemandDepositAccountWithdrawal` |
| 입금 | `/edu/demandDeposit/updateDemandDepositAccountDeposit` |
| 이체 | `/edu/demandDeposit/updateDemandDepositAccountTransfer` |
| 이체 한도 변경 | `/edu/demandDeposit/updateTransferLimit` |
| 거래내역 조회 | `/edu/demandDeposit/inquireTransactionHistoryList` |
| 거래내역 조회 (단건) | `/edu/demandDeposit/inquireTransactionHistory` |
| 계좌 해지 | `/edu/demandDeposit/deleteDemandDepositAccount` |

#### 외화 수시입출금 (prefix: `/edu/demandDeposit/foreignCurrency/`)

원화와 동일한 패턴, 함수명에 `ForeignCurrency` 포함.  
예: `createForeignCurrencyDemandDeposit`, `inquireForeignCurrencyTransactionHistoryList`

---

### DEPOSIT — 예금

> 상세: [5_예금.md](./5_예금.md)

| API 명 | URL (Base 이하) |
|---|---|
| 상품 등록 | `/edu/deposit/createDepositProduct` |
| 상품 조회 | `/edu/deposit/inquireDepositProducts` |
| 계좌 생성 | `/edu/deposit/createDepositAccount` |
| 계좌 목록 조회 | `/edu/deposit/inquireDepositInfoList` |
| 계좌 조회 (단건) | `/edu/deposit/inquireDepositInfoDetail` |
| 납입 상세 조회 | `/edu/deposit/inquireDepositPayment` |
| 만기 이자 조회 | `/edu/deposit/inquireDepositExpiryInterest` |
| 중도 해지 이자 조회 | `/edu/deposit/inquireDepositEarlyTerminationInterest` |
| 계좌 해지 | `/edu/deposit/deleteDepositAccount` |

---

### SAVINGS — 적금

> **API 존재, 세부 문서 없음.** URL 참조만 가능.

| API 명 | URL (Base 이하) |
|---|---|
| 상품 등록 | `/edu/savings/createProduct` |
| 상품 조회 | `/edu/savings/inquireSavingsProducts` |
| 계좌 생성 | `/edu/savings/createAccount` |
| 계좌 목록 조회 | `/edu/savings/inquireAccountList` |
| 계좌 조회 (단건) | `/edu/savings/inquireAccount` |
| 납입 회차 조회 | `/edu/savings/inquirePayment` |
| 만기 이자 조회 | `/edu/savings/inquireExpiryInterest` |
| 중도 해지 이자 조회 | `/edu/savings/inquireEarlyTerminationInterest` |
| 계좌 해지 | `/edu/savings/deleteAccount` |

---

### LOAN — 대출

> **API 존재, 세부 문서 없음.** URL 참조만 가능.

| API 명 | URL (Base 이하) |
|---|---|
| 신용등급 기준 조회 | `/edu/loan/inquireAssetBasedCreditRatingList` |
| 상품 등록 | `/edu/loan/createLoanProduct` |
| 상품 조회 | `/edu/loan/inquireLoanProductList` |
| 내 신용등급 조회 | `/edu/loan/inquireMyCreditRating` |
| 대출심사 신청 | `/edu/loan/createLoanApplication` |
| 대출심사 목록 조회 | `/edu/loan/inquireLoanApplicationList` |
| 대출 상품 가입 | `/edu/loan/createLoanAccount` |
| 대출 상품 가입 목록 조회 | `/edu/loan/inquireLoanAccountList` |
| 대출 상환 내역 조회 | `/edu/loan/inquireRepaymentRecords` |
| 대출 일시납 상환 | `/edu/loan/updateRepaymentLoanBalanceInFull` |

---

### CREDIT CARD — 신용카드

> 상세: [6_카드.md](./6_카드.md)

| API 명 | URL (Base 이하) |
|---|---|
| 카테고리 조회 | `/edu/creditCard/inquireCategoryList` |
| 가맹점 등록 | `/edu/creditCard/createMerchant` |
| 카드사 조회 | `/edu/creditCard/inquireCardIssuerCodesList` |
| 카드 상품 등록 | `/edu/creditCard/createCreditCardProduct` |
| 카드 상품 조회 | `/edu/creditCard/inquireCreditCardList` |
| 카드 생성 | `/edu/creditCard/createCreditCard` |
| 내 카드 목록 조회 | `/edu/creditCard/inquireSignUpCreditCardList` |
| 가맹점 목록 조회 | `/edu/creditCard/inquireMerchantList` |
| 카드 결제 | `/edu/creditCard/createCreditCardTransaction` |
| 카드 결제 내역 조회 | `/edu/creditCard/inquireCreditCardTransactionList` |
| 카드 결제 취소 | `/edu/creditCard/deleteTransaction` |
| 청구서 조회 | `/edu/creditCard/inquireBillingStatements` |
| 결제 계좌 수정 | `/edu/creditCard/updateWithdrawalAccount` |

---

### AUTH — 1원 인증

> 상세: [7_1원인증.md](./7_1원인증.md)  
> 원화 수시입출금 계좌 실명 확인 전용. 외화 계좌 불가.

| API 명 | URL (Base 이하) | 주요 파라미터 |
|---|---|---|
| 1원 송금 | `/edu/accountAuth/openAccountAuth` | `accountNo`, `authText`(기업명) |
| 1원 송금 검증 | `/edu/accountAuth/checkAuthCode` | `accountNo`, `authText`, `authCode`(4자리) |

**플로우:**
```
1. openAccountAuth 호출 → 계좌로 "기업명 XXXX" 형식의 1원 입금
2. 사용자가 거래내역에서 인증코드(XXXX) 확인
3. checkAuthCode 호출 → status: "SUCCESS" / "FAIL" 반환
```

---

### EXCHANGE — 환율/환전

> **API 존재, 세부 문서 없음.** URL 참조만 가능.

| API 명 | URL (Base 이하) |
|---|---|
| 환율 전체 조회 | `/edu/exchangeRate` |
| 환율 단건 조회 | `/edu/exchangeRate/exchangeRateSearch` |
| 환전 예상 금액 조회 | `/edu/exchange/estimate` |
| 환전 신청 | `/edu/exchange` |
| 환전 신청 내역 조회 | `/edu/exchange/exchangeHistory` |

---

### MEMO — 거래내역 메모

> 상세: [8_거래내역메모.md](./8_거래내역메모.md)  
> 원화 및 외화 수시입출금 거래에 메모 작성·수정 가능.

| API 명 | URL (Base 이하) | 주요 파라미터 |
|---|---|---|
| 거래내역 메모 | `/edu/transactionMemo` | `accountNo`, `transactionUniqueNo`, `transactionMemo`(255자) |

---

## 4. 공통 에러코드

### Header 에러 (H코드) — 공통 Header 검증 실패

| 코드 | 설명 |
|---|---|
| `H0000` | 정상 처리 |
| `H1000` | HEADER 정보 유효하지 않음 |
| `H1001` | API 이름 유효하지 않음 |
| `H1002` | 전송일자 형식 오류 |
| `H1003` | 전송시각 형식 오류 |
| `H1004` | 기관코드 유효하지 않음 |
| `H1005` | 핀테크 앱 일련번호 유효하지 않음 |
| `H1006` | API 서비스코드 유효하지 않음 |
| `H1007` | 기관거래고유번호 중복 |
| `H1008` | API_KEY 유효하지 않음 |
| `H1009` | USER_KEY 유효하지 않음 |
| `H1010` | 기관거래고유번호 유효하지 않음 |

### 계정/API KEY 에러 (E코드)

| 코드 | 설명 |
|---|---|
| `E3000` | 이미 존재하는 관리자 ID |
| `E3001` | 존재하지 않는 관리자 ID |
| `E3002` | 관리자 ID 형식 오류 |
| `E3003` | API KEY 만료로 재발급 불가 |
| `E4001` | 빈 데이터 또는 형식 오류 |
| `E4002` | 이미 존재하는 사용자 ID |
| `E4003` | 존재하지 않는 사용자 ID |
| `E4004` | 존재하지 않는 API KEY |

### 계좌/인증 에러 (A코드)

| 코드 | 설명 |
|---|---|
| `A1001` | 은행코드 유효하지 않음 |
| `A1003` | 계좌번호 유효하지 않음 |
| `A1010` | 거래고유번호 유효하지 않음 |
| `A1080` | 등록되지 않은 관리자 이메일 |
| `A1086` | 인증코드 발급 기록 없음 |
| `A1087` | 인증 시간 만료 |
| `A1088` | 인증코드 불일치 |
| `A1089` | 기업명 유효하지 않음 |
| `A1090` | 인증코드 유효하지 않음 |
| `A1091` | 거래 메모 길이 초과 |

### 일반 에러 (Q코드)

| 코드 | 설명 |
|---|---|
| `Q1000` | 기타 서버 에러 |
| `Q1001` | 요청 Body 형식 오류 (JSON/타입 확인) |

---

## 5. 파일 인덱스

| 파일 | 내용 | 크기 |
|---|---|---|
| [0_API 개발가이드.md](./0_API%20개발가이드.md) | 전체 API 목록 테이블 + 용어 정리 | 13 KB |
| [1_APP관리자 키발급.md](./1_APP관리자%20키발급.md) | API KEY 발급/재발급 | 6 KB |
| [2_사용자로그인.md](./2_사용자로그인.md) | 사용자 계정 생성/조회 | 8 KB |
| [3_공통.md](./3_공통.md) | 공통 Header, 은행코드/통화코드 조회 | 29 KB |
| [4_수시입출금.md](./4_수시입출금.md) | 원화/외화 수시입출금 전체 명세 | 128 KB |
| [5_예금.md](./5_예금.md) | 예금 상품/계좌 전체 명세 | 88 KB |
| [6_카드.md](./6_카드.md) | 신용카드 전체 명세 | 129 KB |
| [7_1원인증.md](./7_1원인증.md) | 실명 계좌 인증 (1원 송금) | 14 KB |
| [8_거래내역메모.md](./8_거래내역메모.md) | 거래내역 메모 작성/수정 | 7 KB |
| ~~적금~~ | API 존재, 세부 문서 없음 | - |
| ~~대출~~ | API 존재, 세부 문서 없음 | - |
| ~~환율/환전~~ | API 존재, 세부 문서 없음 | - |
