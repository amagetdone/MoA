### Embedded Content 1

 

[Skip to content](https://project.ssafy.com/docs/ssafy-finance#ssafy-api)

# SSAFY 교육용 금융망 API 개발 가이드[¶](https://project.ssafy.com/docs/ssafy-finance#ssafy-api "Permanent link")

## 1\. API 목록[¶](https://project.ssafy.com/docs/ssafy-finance#1-api "Permanent link")

| **구분** | **API 명** | **URL** | **HTTP Method** |
| --- | --- | --- | --- |
| MANAGER | APP KEY 발급 API | https://finopenapi.ssafy.io/ssafy/api/v1/edu/app/issuedApiKey | POST |
| MANAGER | APP KEY 재발급 API | https://finopenapi.ssafy.io/ssafy/api/v1/edu/app/reIssuedApiKey | POST |
| MEMBER | 계정 생성 | https://finopenapi.ssafy.io/ssafy/api/v1/member | POST |
| MEMBER | 계정 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/member/search | POST |
| COMMON | 은행코드 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/bank/inquireBankCodes | POST |
| COMMON | 통화코드 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/bank/inquireBankCurrency | POST |
| DEMAND DEPOSIT | 상품 등록 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/createDemandDeposit | POST |
| DEMAND DEPOSIT | 상품 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositList | POST |
| DEMAND DEPOSIT | 계좌 생성 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/createDemandDepositAccount | POST |
| DEMAND DEPOSIT | 계좌 목록 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositAccountList | POST |
| DEMAND DEPOSIT | 계좌 조회(단건) | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositAccount | POST |
| DEMAND DEPOSIT | 예금주 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositAccountHolderName | POST |
| DEMAND DEPOSIT | 계좌 잔액 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositAccountBalance | POST |
| DEMAND DEPOSIT | 계좌 출금 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/updateDemandDepositAccountWithdrawal | POST |
| DEMAND DEPOSIT | 계좌 입금 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/updateDemandDepositAccountDeposit | POST |
| DEMAND DEPOSIT | 계좌 이체 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/updateDemandDepositAccountTransfer | POST |
| DEMAND DEPOSIT | 이체 한도 변경 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/updateTransferLimit | POST |
| DEMAND DEPOSIT | 계좌 거래 내역 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireTransactionHistoryList | POST |
| DEMAND DEPOSIT | 계좌 거래 내역 조회(단건) | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireTransactionHistory | POST |
| DEMAND DEPOSIT | 계좌 해지 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/deleteDemandDepositAccount | POST |
| DEPOSIT | 예금 상품 등록 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/deposit/createDepositProduct | POST |
| DEPOSIT | 예금 상품 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/deposit/inquireDepositProducts | POST |
| DEPOSIT | 예금 계좌 생성 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/deposit/createDepositAccount | POST |
| DEPOSIT | 예금 계좌 목록 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/deposit/inquireDepositInfoList | POST |
| DEPOSIT | 예금 계좌 조회(단건) | https://finopenapi.ssafy.io/ssafy/api/v1/edu/deposit/inquireDepositInfoDetail | POST |
| DEPOSIT | 예금 납입 상세 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/deposit/inquireDepositPayment | POST |
| DEPOSIT | 예금 만기 이자 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/deposit/inquireDepositExpiryInterest | POST |
| DEPOSIT | 예금 중도 해지 이자 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/deposit/inquireDepositEarlyTerminationInterest | POST |
| DEPOSIT | 예금 계좌 해지 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/deposit/deleteDepositAccount | POST |
| SAVINGS | 적금 상품 등록 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/savings/createProduct | POST |
| SAVINGS | 적금 상품 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/savings/inquireSavingsProducts | POST |
| SAVINGS | 적금 계좌 생성 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/savings/createAccount | POST |
| SAVINGS | 적금 계좌 목록 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/savings/inquireAccountList | POST |
| SAVINGS | 적금 계좌 조회(단건) | https://finopenapi.ssafy.io/ssafy/api/v1/edu/savings/inquireAccount | POST |
| SAVINGS | 적금 납입 회차 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/savings/inquirePayment | POST |
| SAVINGS | 적금 만기 이자 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/savings/inquireExpiryInterest | POST |
| SAVINGS | 적금 중도 해지 이자 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/savings/inquireEarlyTerminationInterest | POST |
| SAVINGS | 적금 계좌 해지 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/savings/deleteAccount | POST |
| LOAN | 신용등급 기준 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/loan/inquireAssetBasedCreditRatingList | POST |
| LOAN | 대출 상품 등록 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/loan/createLoanProduct | POST |
| LOAN | 대출 상품 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/loan/inquireLoanProductList | POST |
| LOAN | 내 신용등급 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/loan/inquireMyCreditRating | POST |
| LOAN | 대출심사 신청 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/loan/createLoanApplication | POST |
| LOAN | 대출심사 목록 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/loan/inquireLoanApplicationList | POST |
| LOAN | 대출 상품 가입 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/loan/createLoanAccount | POST |
| LOAN | 대출 상품 가입 목록 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/loan/inquireLoanAccountList | POST |
| LOAN | 대출 상환 내역 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/loan/inquireRepaymentRecords | POST |
| LOAN | 대출 일시납 상환 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/loan/updateRepaymentLoanBalanceInFull | POST |
| CREDIT CARD | 카테고리 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireCategoryList | POST |
| CREDIT CARD | 가맹점 등록 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/createMerchant | POST |
| CREDIT CARD | 카드사 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireCardIssuerCodesList | POST |
| CREDIT CARD | 카드 상품 등록 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/createCreditCardProduct | POST |
| CREDIT CARD | 카드 상품 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireCreditCardList | POST |
| CREDIT CARD | 카드 생성 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/createCreditCard | POST |
| CREDIT CARD | 내 카드 목록 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireSignUpCreditCardList | POST |
| CREDIT CARD | 가맹점 목록 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireMerchantList | POST |
| CREDIT CARD | 카드 결제 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/createCreditCardTransaction | POST |
| CREDIT CARD | 카드 결제 내역 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireCreditCardTransactionList | POST |
| CREDIT CARD | 카드 결제 취소 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/deleteTransaction | POST |
| CREDIT CARD | 청구서 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireBillingStatements | POST |
| CREDIT CARD | 카드 결제 계좌 수정 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/updateWithdrawalAccount | POST |
| AUTH | 1원 송금 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/accountAuth/openAccountAuth | POST |
| AUTH | 1원 송금 검증 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/accountAuth/checkAuthCode | POST |
| EXCHANGE RATE | 환율 전체 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/exchangeRate | POST |
| EXCHANGE RATE | 환율 단건 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/exchangeRate/exchangeRateSearch | POST |
| EXCHANGE | 환전 예상 금액 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/exchange/estimate | POST |
| EXCHANGE | 환전 신청 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/exchange | POST |
| EXCHANGE | 환전 신청 내역 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/exchange/exchangeHistory | POST |
| DEMAND DEPOSIT FOREIGN | 외화 상품 등록 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/createForeignCurrencyDemandDeposit | POST |
| DEMAND DEPOSIT FOREIGN | 외화 상품 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/inquireForeignCurrencyDemandDepositList | POST |
| DEMAND DEPOSIT FOREIGN | 외화 계좌 생성 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/createForeignCurrencyDemandDepositAccount | POST |
| DEMAND DEPOSIT FOREIGN | 외화 계좌 목록 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/inquireForeignCurrencyDemandDepositAccountList | POST |
| DEMAND DEPOSIT FOREIGN | 외화 계좌 조회(단건) | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/inquireForeignCurrencyDemandDepositAccount | POST |
| DEMAND DEPOSIT FOREIGN | 외화 예금주 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/inquireForeignCurrencyDemandDepositAccountHolderName | POST |
| DEMAND DEPOSIT FOREIGN | 외화 계좌 잔액 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/inquireForeignCurrencyDemandDepositAccountBalance | POST |
| DEMAND DEPOSIT FOREIGN | 외화 계좌 출금 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/updateForeignCurrencyDemandDepositAccountWithdrawal | POST |
| DEMAND DEPOSIT FOREIGN | 외화 계좌 입금 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/updateForeignCurrencyDemandDepositAccountDeposit | POST |
| DEMAND DEPOSIT FOREIGN | 외화 계좌 이체 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/updateForeignCurrencyDemandDepositAccountTransfer | POST |
| DEMAND DEPOSIT FOREIGN | 외화 이체 한도 변경 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/updateForeignCurrencyTransferLimit | POST |
| DEMAND DEPOSIT FOREIGN | 외화 계좌 거래 내역 조회 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/inquireForeignCurrencyTransactionHistoryList | POST |
| DEMAND DEPOSIT FOREIGN | 외화 계좌 거래 내역 조회(단건) | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/inquireForeignCurrencyTransactionHistory | POST |
| DEMAND DEPOSIT FOREIGN | 외화 계좌 해지 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/foreignCurrency/deleteForeignCurrencyDemandDepositAccount | POST |
| MEMO | 거래내역 메모 | https://finopenapi.ssafy.io/ssafy/api/v1/edu/transactionMemo | POST |

---

### 1.1 개요[¶](https://project.ssafy.com/docs/ssafy-finance#11 "Permanent link")

#### 목적[¶](https://project.ssafy.com/docs/ssafy-finance#_1 "Permanent link")

본 문서는 SSAFY 교육용 금융망 API 명세에 대한 기술을 담고 있습니다.  
SSAFY 교육용 금융망 이용승인을 받은 핀테크 기업(이하 "이용기관")은 SSAFY 교육용 금융망이 제공하는 API를 활용하여 이용기관용 응용프로그램(이하 "앱")을 개발할 수 있습니다.

---

### 1.2 용어 정리[¶](https://project.ssafy.com/docs/ssafy-finance#12 "Permanent link")

| **용어** | **설명** |
| --- | --- |
| **API** | API(애플리케이션 프로그래밍 인터페이스)는 SSAFY 교육용 금융망과 데이터 보유자 간 개인 신용정보를 주고받기 위한 전송 규격을 따름 |
| **URI** | 웹 상의 자원을 유일하게 식별할 수 있는 식별자 또는 주소를 의미 |
| **사용자** | SSAFY 교육용 금융망 업무를 통해 제공되는 서비스를 활용하는 개인 고객 |
| **이용기관 (앱 관리자)** | SSAFY 교육용 금융망 업무 이용 승인을 받아 SSAFY와 이용 계약을 체결한 자 (앱 관리자라 함은 앱 개발자를 뜻함) |
| **SSAFY 교육용 금융망** | SSAFY 교육용 금융망 업무를 위하여 SSAFY에 이용기관 및 정보제공자의 처리시스템을 연결하여 상호간에 정보를 교환하는 시스템. 금융정보 제공 등의 업무를 처리하는 기관. |
| **기관코드** | 기관 약정 시 발급된 핀테크 기관코드를 말함 (‘00100’으로 고정) |
| **핀테크 앱 일렬번호** | 핀테크 서비스 약정 시 발급된 핀테크 앱 일렬번호를 말함 (‘001’로 고정) |
| **기관거래고유번호** | 핀테크 서비스별 거래 고유번호. 새로운 번호로 임의 채번 (YYYYMMDD + HHMMSS + 일련번호 6자리) 또는 20자리의 난수. API 요청 시 항상 새로운 번호로 임의 채번해야 함 |
| **apiKey** | 앱 관리자(개발자)가 발급받은 API KEY |
| **userKey** | 앱 사용자가 회원가입할 때 발급받은 USER KEY |

Back to top

---
Source: [SSAFY_WebDRM[cH3]](https://project.ssafy.com/docs/ssafy-finance)