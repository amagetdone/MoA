# Domain Entity ERD Spec

작성일: 2026-03-10

## 1. 목적

이 문서는 `/mnt/c/ssafy/특화/docs/erd/erd.png` 와 `/mnt/c/ssafy/특화/docs/erd/2026-03-06-예산편성-erd-설계안.md` 를 기준으로 도메인 엔티티 구현에 필요한 물리 스키마를 전사한 문서다.

구현 시 다음 원칙을 따른다.

- 이 문서에 적힌 테이블과 컬럼은 임의로 생략하지 않는다.
- 타입, `NULL` 여부, 기본값, FK 관계는 ERD 표기를 우선한다.
- ERD 이미지에 enum 상세값이 없고 보조 설계안에만 있는 경우, 그 값을 함께 기록한다.
- ERD 이미지에 enum 상세값이 없고 다른 문서에도 없는 경우, `값 미기재`로 표기한다.

## 2. 도메인별 테이블 목록

| 도메인 | 테이블 |
| --- | --- |
| member | `users`, `social_account`, `setting`, `user_agreements`, `policy` |
| asset | `account`, `card`, `pocket`, `category`, `merchant` |
| transaction | `transaction_override`, `card_transaction_override`, `account_transaction_override` |
| plan | `plan`, `plan_week_allocation` |
| budget | `budget_month`, `essential_expense_month`, `budget_week`, `essential_week_allocation`, `budget_adjustment`, `budget_rollover` |
| common | `base_entity` |

## 3. 공통 스키마

### 3.1 `base_entity`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `created_at` | 생성일 | `DATETIME` | `NOT NULL` | `CURRENT_DATETIME` | 공통 생성 시각 |
| `updated_at` | 수정일 | `DATETIME` | `NOT NULL` | `CURRENT_DATETIME` | 공통 수정 시각 |
| `is_deleted` | 삭제 여부 | `TINYINT(1)` | `NOT NULL` | `0` | 공통 소프트 삭제 플래그 |

## 4. Member 도메인

### 4.1 `users`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 유저 아이디 | `INT` | `NOT NULL` | 없음 | PK |
| `email` | 유저 이메일 | `VARCHAR(255)` | `NULL` | 없음 | 구글에서 넘어오는 값은 기본으로 채우고 카카오는 추가 정보 입력 |
| `nickname` | 닉네임 | `VARCHAR(50)` | `NOT NULL` | 없음 | 추가 정보 입력 |
| `birth_date` | 생년월일 | `DATETIME` | `NULL` | 없음 |  |
| `job_status` | 직업 상태 | `ENUM` | `NULL` | 없음 | 값 미기재 |
| `status` | 활성화 여부 | `ENUM` | `NULL` | `'ACTIVE'` | 값 미기재 |
| `user_api_key` | 유저 api 키 | `VARCHAR(30)` | `NOT NULL` | 없음 |  |
| `profile_url` | 프로필이미지url | `VARCHAR(255)` | `NULL` | 없음 | 사용자가 등록한 프로필이미지 S3 URL 예정 |

관계

- `users 1:N social_account`
- `users 1:N user_agreements`
- `users 1:1 setting`
- `users 1:N account`
- `users 1:N plan`
- `users 1:N budget_month`
- `users 1:N budget_week`

### 4.2 `social_account`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 소셜 계정 아이디 | `INT` | `NOT NULL` | 없음 | PK |
| `users_id` | 유저 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `users.id` |
| `provider` | 소셜 공급자 | `VARCHAR(20)` | `NULL` | 없음 | 소셜 공급자 |
| `provider_uid` | 공급자 제공 고유 ID | `VARCHAR(255)` | `NOT NULL` | 없음 | 공급자 제공 고유 ID |

### 4.3 `setting`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 설정 id | `INT` | `NOT NULL` | 없음 | PK |
| `users_id` | 유저 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `users.id` |
| `alarm` | 알림 | `TINYINT(1)` | `NOT NULL` | `0` |  |

### 4.4 `policy`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 약관 아이디 | `INT` | `NOT NULL` | 없음 | PK |
| `content` | 약관 내용 | `TEXT` | `NOT NULL` | 없음 |  |
| `agreement_type` | 약관 종류 | `ENUM('TERMS','PRIVACY','MARKETING')` | `NOT NULL` | 없음 |  |
| `required` | 필수 여부 | `TINYINT(1)` | `NOT NULL` | 없음 |  |
| `version` | 약관 버전 | `INT` | `NOT NULL` | 없음 |  |

### 4.5 `user_agreements`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 사용자 약관 동의 아이디 | `INT` | `NOT NULL` | 없음 | PK |
| `agreed` | 동의 여부 | `TINYINT(1)` | `NOT NULL` | 없음 |  |
| `agreed_at` | 동의 날짜 | `DATETIME` | `NULL` | 없음 |  |
| `policy_id` | 약관 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `policy.id` |
| `users_id` | 유저 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `users.id` |

## 5. Asset 도메인

### 5.1 `account`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 계좌 아이디 | `VARCHAR(30)` | `NOT NULL` | `0` | 계좌id 내부 PK |
| `users_id` | 유저 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `users.id` |
| `account_no` | 계좌번호 | `VARCHAR(20)` | `NOT NULL` | 없음 | 계좌번호 기본 16자리 string |
| `account_type_unique_no` | 상품고유번호 | `VARCHAR(30)` | `NULL` | 없음 | SSAFY API `accountTypeUniqueNo` - 계좌 상품 고유번호 |
| `is_verified` | 1원인증여부 | `TINYINT` | `NOT NULL` | `0` | 1원인증여부 0=미인증 1=인증완료 |
| `alias` | 별칭 | `VARCHAR(30)` | `NULL` | 없음 | 별칭 |
| `bank_code` | 은행코드 | `VARCHAR(30)` | `NOT NULL` | 없음 | 3자리 은행코드 |
| `bank_name` | 은행명 | `VARCHAR(30)` | `NOT NULL` | 없음 | 은행명 |

### 5.2 `card`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `cardUniqueNo` | 카드고유값 | `BIGINT` | `NOT NULL` | 없음 | PK |
| `withdrawalAccountNo` | 계좌 id | `VARCHAR(30)` | `NOT NULL` | `0` | FK 성격, 계좌id |
| `cvc` | cvc | `VARCHAR(3)` | `NOT NULL` | 없음 | cvc번호 |
| `cardNo` | 카드아이디 | `VARCHAR(20)` | `NOT NULL` | 없음 | 카드id |
| `name` | 카드명 | `VARCHAR(20)` | `NOT NULL` | 없음 | 카드명 |
| `issuerCode` | 카드사 코드 | `VARCHAR(10)` | `NOT NULL` | 없음 | 카드사코드 |
| `cardIssuerName` | 카드사 명 | `VARCHAR(20)` | `NOT NULL` | 없음 | 카드사명 |
| `cardExpiryDate` | 카드만료일 | `DATETIME` | `NOT NULL` | 없음 | 카드만료일 |
| `cardDescription` | 카드설명 | `VARCHAR(255)` | `NOT NULL` | 없음 | 카드설명 |
| `withdrawalDate` | 카드납부일 | `VARCHAR(10)` | `NOT NULL` | 없음 | 출금날짜 |
| `baselinePerformance` | 기준실적 | `BIGINT` | `NOT NULL` | 없음 | 기준실적 |
| `maxBenefitLimit` | 최대혜택한도 | `BIGINT` | `NOT NULL` | 없음 | 최대혜택한도 |

### 5.3 `category`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 카테고리 아이디 | `INT` | `NOT NULL` | `0` | PK |
| `name` | 카테고리 명 | `VARCHAR(20)` | `NOT NULL` | 없음 | 카테고리 이름 |

### 5.4 `merchant`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 가맹점 아이디 | `INT` | `NOT NULL` | `0` | PK |
| `name` | 가맹점명 | `VARCHAR(100)` | `NOT NULL` | 없음 | 가맹점명 |

### 5.5 `pocket`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 포켓 아이디 | `VARCHAR(50)` | `NOT NULL` | 없음 | 계좌id-카테고리id |
| `account_id` | 계좌 아이디 | `VARCHAR(30)` | `NOT NULL` | 없음 | FK -> `account.id` |
| `budget_week_id` | `budget_week_id` | `INT` | `NOT NULL` | 없음 | FK -> `budget_week.id` |
| `category_id` | 카테고리 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `category.id` |
| `name` | 포켓명 | `VARCHAR(30)` | `NOT NULL` | 없음 | 포켓 이름 |
| `allocated_amount` | 배정금액 | `BIGINT` | `NOT NULL` | `0` | 각 포켓별 배정한 금액 |

## 6. Transaction 도메인

### 6.1 `transaction_override`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 거래내역 아이디 | `INT` | `NOT NULL` | `0` | PK |
| `category_id` | 카테고리 아이디 | `BIGINT` | `NOT NULL` | `0` | FK 성격, 카테고리id |
| `transactionDateTime` | 거래일시 | `DATETIME` | `NOT NULL` | 없음 | 거래일시 |
| `transactionBalance` | 거래금액 | `BIGINT` | `NOT NULL` | 없음 | 거래금액 |
| `transactionMemo` | 거래메모 | `TEXT` | `NOT NULL` | 없음 | 거래메모 |

관계

- `transaction_override` 는 상속 루트 테이블로 보이며, `card_transaction_override.id`, `account_transaction_override.id` 가 이를 참조한다.

### 6.2 `card_transaction_override`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 거래내역 아이디 | `INT` | `NOT NULL` | 없음 | PK/FK -> `transaction_override.id` |
| `transactionUniqueNo` | 거래내역고유값(`ssafy api`) | `BIGINT` | `NOT NULL` | 없음 | 외부 거래고유아이디 |
| `merchant_id` | 가맹점 아이디 | `BIGINT` | `NOT NULL` | `0` | FK -> `merchant.id` |
| `cardStatus` | 카드승인상태 | `BOOLEAN` | `NOT NULL` | 없음 | 카드승인상태 |
| `billStatementsYn` | 청구여부 | `BOOLEAN` | `NOT NULL` | 없음 | 청구여부 |
| `billStatementsStatus` | 납부상태 | `VARCHAR(20)` | `NOT NULL` | 없음 | 납부상태 |
| `cardUniqueNo` | 카드유일값 | `BIGINT` | `NOT NULL` | 없음 | FK -> `card.cardUniqueNo` |

### 6.3 `account_transaction_override`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 거래내역 아이디 | `INT` | `NOT NULL` | 없음 | PK/FK -> `transaction_override.id` |
| `transactionUniqueNo` | 거래내역고유값(`ssafy api`) | `BIGINT` | `NOT NULL` | 없음 | 외부 거래내역 아이디 |
| `transactionType` | 거래유형번호 | `INT` | `NOT NULL` | 없음 | 거래유형번호 |
| `transactionTypeName` | 거래유형이름 | `VARCHAR(20)` | `NOT NULL` | 없음 | 거래유형이름 |
| `transactionAfterBalance` | 거래후계좌금액 | `BIGINT` | `NOT NULL` | 없음 | 거래후계좌금액 |
| `transactionSummary` | 거래 내용 요약 | `VARCHAR(30)` | `NOT NULL` | 없음 | 거래 내용 요약 |
| `account_id` | 계좌 id | `INT` | `NOT NULL` | `0` | FK 성격, 계좌id |

## 7. Plan 도메인

### 7.1 `plan`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 목표 아이디 | `INT` | `NOT NULL` | 없음 | PK |
| `users_id` | 사용자 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `users.id` |
| `title` | 목표명 | `VARCHAR(100)` | `NOT NULL` | 없음 | 목표명 |
| `description` | 목표 설명 | `VARCHAR(255)` | `NULL` | 없음 | 목표 설명 |
| `target_amount` | 목표 금액 | `BIGINT` | `NOT NULL` | 없음 | 목표 금액 |
| `saved_amount` | 현재 누적 금액 | `BIGINT` | `NOT NULL` | `0` | 현재 누적 금액 |
| `start_date` | 목표 시작일 | `DATE` | `NOT NULL` | 없음 | 목표 시작일 |
| `target_weeks` | 목표 기간(주) | `INT` | `NOT NULL` | 없음 | 목표 기간(주) |
| `priority` | 우선 순위 | `TINYINT` | `NOT NULL` | 없음 | 우선순위(숫자가 작을수록 우선) |
| `status` | 상태 | `ENUM` | `NOT NULL` | `'ACTIVE'` | `(ACTIVE, PAUSED, COMPLETED, CANCELLED)` |
| `auto_allocate_enabled` | 자동 배정 여부 | `TINYINT` | `NOT NULL` | `TRUE` | 자동 배정 여부 |
| `plan_url` | 이미지 url | `VARCHAR(255)` | `NULL` | 없음 |  |
| `end_date` | 목표 종료일 | `DATE` | `NOT NULL` | 없음 | 목표 종료일 |

### 7.2 `plan_week_allocation`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 주간 목표 배정 아이디 | `INT` | `NOT NULL` | 없음 | PK |
| `budget_week_id` | 주간 예산 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `budget_week.id` |
| `plan_id` | 목표 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `plan.id` |
| `allocation_type` | 배정 유형 | `VARCHAR(30)` | `NOT NULL` | `'AUTO'` | `(AUTO, MANUAL)` |
| `planned_amount` | 기본 배정 금액 | `BIGINT` | `NOT NULL` | `0` | 기본 배정 금액 |
| `final_amount` | 조정 반영 금액 | `BIGINT` | `NOT NULL` | `0` | 조정 반영 금액 |
| `note` | 비고 | `VARCHAR(255)` | `NULL` | 없음 | 비고 |

## 8. Budget 도메인

### 8.1 `budget_month`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 월 예산 아이디 | `INT` | `NOT NULL` | 없음 | PK |
| `users_id` | `users_id` | `INT` | `NOT NULL` | 없음 | FK -> `users.id` |
| `base_date` | 기준 연, 월 | `DATE` | `NOT NULL` | 없음 | 기준 연, 월 |
| `status` | 상태 | `ENUM` | `NOT NULL` | `'OPEN'` | `(OPEN, CLOSED, LOCKED)` |
| `total_income_amount` | 월 총수입 | `BIGINT` | `NOT NULL` | `0` | 월 총수입 |
| `total_fixed_essential_amount` | 월 총고정비 | `BIGINT` | `NOT NULL` | `0` | 월 총고정비 |
| `total_variable_essential_amount` | 월 총변동비 | `BIGINT` | `NOT NULL` | `0` | 월 총변동비 |
| `memo` | 비고 | `VARCHAR(255)` | `NULL` | 없음 | 비고 |

### 8.2 `essential_expense_month`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 월 필수비 아이디 | `INT` | `NOT NULL` | 없음 | PK |
| `category_id` | 카테고리 아이디 | `INT` | `NOT NULL` | `0` | FK -> `category.id` |
| `budget_month_id` | 월 예산 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `budget_month.id` |
| `expense_scope` | 필수비 구분 | `VARCHAR(20)` | `NOT NULL` | 없음 | `(FIXED, VARIABLE)` |
| `amount` | 금액 | `BIGINT` | `NOT NULL` | 없음 | 금액 |
| `source_provider` | 분석 출처 | `ENUM` | `NOT NULL` | `'ANALYZER'` | `(ANALYZER, USER_INPUT)` |

### 8.3 `budget_week`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 주간 예산 아이디 | `INT` | `NOT NULL` | 없음 | PK |
| `users_id` | `users_id` | `INT` | `NOT NULL` | 없음 | FK -> `users.id` |
| `week_sequence` | 회원 기준 주차 순번 | `INT` | `NOT NULL` | 없음 | 회원 기준 주차 순번 |
| `week_start_date` | 주 시작일 | `DATE` | `NOT NULL` | 없음 | 주 시작일 |
| `week_end_date` | 주 종료일 | `DATE` | `NOT NULL` | 없음 | 주 종료일 |
| `period_days` | 실제 포함 일수 | `TINYINT` | `NOT NULL` | `7` | 실제 포함 일수(부분 주차 허용) |
| `is_partial` | 부분 주차 여부 | `TINYINT` | `NOT NULL` | `FALSE` | 부분 주차 여부 |
| `status` | 상태 | `ENUM` | `NOT NULL` | `'PLANNED'` | `(PLANNED, ACTIVE, CLOSED, ADJUSTED)` |
| `planned_income_amount` | 계획 수입 | `BIGINT` | `NOT NULL` | `0` | 계획 수입 |
| `planned_essential_amount` | 계획 필수비 | `BIGINT` | `NOT NULL` | `0` | 계획 필수비 |
| `planned_saving_amount` | 계획 저축비 | `BIGINT` | `NOT NULL` | `0` | 계획 저축비 |
| `rollover_in_amount` | 이전 주 초과 사용으로 차감될 금액 | `BIGINT` | `NOT NULL` | `0` | 이전 주 초과 사용으로 차감될 금액 |
| `manual_adjustment_amount` | 수동 증감 합계 | `BIGINT` | `NOT NULL` | `0` | 수동 증감 합계 |
| `final_free_amount` | 최종 자유비 | `BIGINT` | `NOT NULL` | `0` | 최종 자유비 |
| `actual_spent_amount` | 실제 사용액 | `BIGINT` | `NOT NULL` | `0` | 실제 사용액 |

### 8.4 `essential_week_allocation`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `essential_week_allocation_id` | 주간 필수비 배정 아이디 | `INT` | `NOT NULL` | 없음 | PK |
| `essential_expense_month_id` | 월 필수비 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `essential_expense_month.id` |
| `category_id` | 카테고리 아이디 | `INT` | `NOT NULL` | `0` | FK -> `category.id` |
| `budget_week_id` | 주간 예산 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `budget_week.id` |
| `expense_scope` | 필수비 구분 | `ENUM` | `NOT NULL` | 없음 | `(FIXED, VARIABLE)` |
| `planned_amount` | 주 카테고리별 필수비 배정 금액 | `BIGINT` | `NOT NULL` | 없음 | 해당 주 카테고리별 필수비 배정 금액 |
| `actual_spent_amount` | 주간 누적금액 | `BIGINT` | `NOT NULL` | 없음 | 주간 누적금액 |
| `note` | 비고 | `VARCHAR(255)` | `NULL` | 없음 | 비고 |

### 8.5 `budget_adjustment`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 예산 조정 아이디 | `INT` | `NOT NULL` | 없음 | PK |
| `budget_week_id` | 주간 예산 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `budget_week.id` |
| `plan_id` | 목표 아이디 | `INT` | `NULL` | 없음 | FK -> `plan.id`, 자유비 조정 시 `NULL` |
| `adjustment_type` | `adjustment_type` | `ENUM` | `NOT NULL` | 없음 | 보조 설계안 확정값 사용 |
| `adjustment_amount` | 조정 금액 | `BIGINT` | `NOT NULL` | `0` | 조정 금액 |
| `reason` | 조정 사유 | `VARCHAR(255)` | `NULL` | 없음 | 조정 사유 |

보조 설계안 확정 enum 값

- `FREE_BUDGET_ADD`
- `FREE_BUDGET_SUBTRACT`
- `PLAN_ADD`
- `PLAN_SUBTRACT`
- `PLAN_DELAY`
- `PLAN_EARLY_CLOSE`
- `ROLLOVER_APPLIED`

### 8.6 `budget_rollover`

| 컬럼 | 논리명 | 타입 | NULL | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | 예산 이월 아이디 | `INT` | `NOT NULL` | 없음 | PK |
| `source_budget_week_id` | 주간 예산 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `budget_week.id` |
| `target_budget_week_id` | 주간 예산 아이디 | `INT` | `NOT NULL` | 없음 | FK -> `budget_week.id` |
| `rollover_step` | 이월 단계(1-4) | `TINYINT` | `NOT NULL` | 없음 | 이월 단계(1-4) |
| `rollover_amount` | 이월 차감 금액 | `BIGINT` | `NOT NULL` | 없음 | 이월 차감 금액 |
| `status` | 상태 | `ENUM` | `NOT NULL` | `'APPLIED'` | `(PLANNED, APPLIED, EXHAUSTED, PLAN_DEDUCTED)` |
| `resolved_at` | 해소일시 | `DATETIME` | `NULL` | 없음 | 해소일시 |
| `note` | 비고 | `VARCHAR(255)` | `NULL` | 없음 | 비고 |

## 9. 관계 요약

### 9.1 Member

- `social_account.users_id -> users.id`
- `setting.users_id -> users.id`
- `user_agreements.policy_id -> policy.id`
- `user_agreements.users_id -> users.id`

### 9.2 Asset / Transaction

- `account.users_id -> users.id`
- `card.withdrawalAccountNo -> account.id` 로 해석된다. ERD 코멘트도 `계좌id` 로 적혀 있다.
- `pocket.account_id -> account.id`
- `pocket.budget_week_id -> budget_week.id`
- `pocket.category_id -> category.id`
- `transaction_override.category_id -> category.id`
- `card_transaction_override.id -> transaction_override.id`
- `card_transaction_override.merchant_id -> merchant.id`
- `card_transaction_override.cardUniqueNo -> card.cardUniqueNo`
- `account_transaction_override.id -> transaction_override.id`
- `account_transaction_override.account_id -> account.id` 로 해석된다.

### 9.3 Plan / Budget

- `plan.users_id -> users.id`
- `plan_week_allocation.budget_week_id -> budget_week.id`
- `plan_week_allocation.plan_id -> plan.id`
- `budget_month.users_id -> users.id`
- `essential_expense_month.category_id -> category.id`
- `essential_expense_month.budget_month_id -> budget_month.id`
- `budget_week.users_id -> users.id`
- `essential_week_allocation.essential_expense_month_id -> essential_expense_month.id`
- `essential_week_allocation.category_id -> category.id`
- `essential_week_allocation.budget_week_id -> budget_week.id`
- `budget_adjustment.budget_week_id -> budget_week.id`
- `budget_adjustment.plan_id -> plan.id`
- `budget_rollover.source_budget_week_id -> budget_week.id`
- `budget_rollover.target_budget_week_id -> budget_week.id`

## 10. 구현 메모

- 엔티티 클래스명은 도메인 설계 문서의 합의대로 사용한다.
  - `users -> User`
  - `user_agreements -> Agreement`
  - `social_account -> SocialAccount`
  - `transaction_override -> TransactionOverride`
  - `card_transaction_override -> CardTransactionOverride`
  - `account_transaction_override -> AccountTransactionOverride`
- `base_entity` 는 ERD에 독립 테이블로 표현되어 있지만, JPA 구현에서는 `@MappedSuperclass` 기반 공통 컬럼으로 흡수할지 검토가 필요하다.
- `budget_adjustment.adjustment_type` 은 ERD 이미지에 세부값이 없으므로 반드시 보조 설계안의 확정 enum 값을 사용한다.
- `users.job_status`, `users.status`, `budget_month.status`, `budget_week.status`, `essential_week_allocation.expense_scope` 등은 이미지에 값이 모두 적히지 않은 경우가 있으므로, 이미지/보조 설계안에 나온 값만 우선 반영하고 나머지는 추가 설계 없이 임의 확장하지 않는다.
