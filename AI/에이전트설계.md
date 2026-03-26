# 에이전트형 챗봇 조회 툴 제안서

## 목적

현재 DB 구조를 기준으로, 에이전트형 챗봇이 사용자에게 상태 설명과 피드백을 줄 수 있도록 조회 중심 툴 후보를 정리한다.

기준:

- 쓰기/변경 툴 제외
- 사용자별 범위 강제
- 챗봇이 바로 피드백하기 좋은 구조 우선
- 원시 조회 툴과 설명용 요약 툴을 함께 둔 혼합형 구성

## DB 구조 상세

### 공통 사항

- 대부분의 주요 테이블은 `BaseEntity`를 상속한다.
- 공통 관리 컬럼:
  - `created_at`: 생성 시각
  - `updated_at`: 수정 시각
  - `is_deleted`: 소프트 삭제 여부
- 챗봇 관점에서는 최근성 판단, 비활성 데이터 제외, 변경 이력 해석에 활용 가능하다.

### 1. 자산 도메인

#### 관계도

```text
users (1) ---- (N) account (1) ---- (N) card

account_transaction_override (N) ---- (1) account
card_transaction_override    (N) ---- (1) card
card_transaction_override    (N) ---- (1) merchant

transaction_override         (N) ---- (1) category
```

#### 테이블별 핵심 컬럼 역할

##### `account`

- `id`: 계좌 식별자. 현재 계좌번호를 PK로 사용
- `users_id`: 사용자 소유 계좌 구분
- `account_no`: 실계좌 번호
- `account_type_unique_no`: 상품 유형 식별
- `account_name`, `account_type_name`: 챗봇 표시용 계좌 이름/유형
- `is_verified`: 인증 완료 여부
- `is_active`: 현재 연결 활성 여부
- `bank_code`, `bank_name`: 은행 식별
- `alias`: 사용자 친화 별칭

##### `card`

- `cardUniqueNo`: 카드 PK
- `withdrawalAccountNo`: 출금 계좌 FK
- `cardNo`: 카드 번호
- `name`: 카드명
- `issuerCode`, `cardIssuerName`: 카드사 정보
- `withdrawalDate`: 결제일
- `baselinePerformance`, `maxBenefitLimit`: 카드 혜택 기준/한도

##### `merchant`

- `id`: 가맹점 PK
- `name`: 카드 거래 설명용 가맹점명

##### `category`

- `id`: 카테고리 PK
- `name`: 소비 분석, 필수지출 분류, 챗봇 피드백의 기본 라벨

#### 챗봇 관점 해석 포인트

- 연결 상태 설명은 `account.is_verified`, `account.is_active`, `card` 존재 여부로 가능하다.
- 어떤 카드가 어떤 계좌에 연결되어 있는지 바로 설명할 수 있다.
- 계좌/카드는 거래 테이블과 결합하면 최근 활동 여부까지 판단 가능하다.

### 2. 예산 도메인

#### 관계도

```text
users (1) ---- (N) budget_month
users (1) ---- (N) budget_week

budget_week (1) ---- (N) budget_adjustment
plan       (1) ---- (N) budget_adjustment   [optional FK]

budget_week (1) ---- (N) budget_rollover [source_budget_week_id]
budget_week (1) ---- (N) budget_rollover [target_budget_week_id]

note:
budget_month 와 budget_week 사이에는 현재 직접 FK가 없고,
같은 users_id + 날짜 범위 기준으로 논리적으로 연결된다.
```

#### 테이블별 핵심 컬럼 역할

##### `budget_month`

- `id`: 월 예산 PK
- `users_id`: 사용자별 월 예산 구분
- `base_date`: 어떤 월 예산인지 판단하는 기준일
- `status`: 월 예산 상태
- `total_income_amount`: 월 총 수입
- `total_fixed_essential_amount`: 월 고정 필수지출
- `total_variable_essential_amount`: 월 변동 필수지출
- `memo`: 예산 메모

##### `budget_week`

- `id`: 주간 예산 PK
- `users_id`: 사용자별 주차 구분
- `week_sequence`: 월 내 혹은 계획 기준 주차 순번
- `week_start_date`, `week_end_date`: 주차 기간
- `period_days`: 주차 일수
- `is_partial`: 부분 주차 여부
- `status`: 주간 예산 상태
- `planned_income_amount`: 계획 수입
- `planned_essential_amount`: 계획 필수지출
- `planned_saving_amount`: 계획 저축
- `rollover_in_amount`: 이전 주 이월 유입액
- `manual_adjustment_amount`: 수동 조정 누적액
- `final_free_amount`: 최종 자유예산
- `actual_spent_amount`: 실제 사용액

##### `budget_adjustment`

- `id`: 예산 조정 이력 PK
- `budget_week_id`: 어떤 주차 예산에 대한 조정인지
- `plan_id`: 특정 목표와 연관된 조정인지
- `adjustment_type`: 조정 종류
- `adjustment_amount`: 조정 금액
- `reason`: 조정 사유

##### `budget_rollover`

- `id`: 이월 이력 PK
- `source_budget_week_id`: 원천 주차
- `target_budget_week_id`: 대상 주차
- `rollover_step`: 이월 단계
- `rollover_amount`: 이월 금액
- `status`: 적용/해결 상태
- `resolved_at`: 처리 완료 시점
- `note`: 이월 설명

#### 챗봇 관점 해석 포인트

- 이번 주 예산 건강도는 대부분 `budget_week` 한 테이블에서 바로 계산 가능하다.
- 조정/이월 이유 설명은 append-only 이력인 `budget_adjustment`, `budget_rollover`로 보강할 수 있다.
- `budget_month`와 `budget_week`가 직접 연결되어 있지 않으므로 월-주 비교 툴은 기간 계산 로직이 필요하다.

### 3. 목표 도메인

#### 관계도

```text
users (1) ---- (N) plan

plan        (1) ---- (N) plan_week_allocation
budget_week (1) ---- (N) plan_week_allocation
```

#### 테이블별 핵심 컬럼 역할

##### `plan`

- `id`: 목표 PK
- `users_id`: 사용자 소유 목표 구분
- `title`, `description`: 목표 설명
- `target_amount`: 목표 금액
- `saved_amount`: 현재까지 모은 금액
- `start_date`, `end_date`: 목표 기간
- `target_weeks`: 기간 기반 주차 수
- `priority`: 우선순위
- `status`: 목표 상태
- `auto_allocate_enabled`: 자동 배정 여부
- `plan_img_url`, `plan_video_url`: AI 생성 결과
- `ai_pipeline_status`, `ai_retry_count`: AI 파이프라인 상태
- `version`: 낙관적 락 버전

##### `plan_week_allocation`

- `id`: 주간 목표 배정 PK
- `budget_week_id`: 어떤 주차 예산과 연결되는지
- `plan_id`: 어떤 목표에 대한 배정인지
- `allocation_type`: 자동/수동 등 배정 유형
- `planned_amount`: 계획 배정 금액
- `final_amount`: 최종 반영 금액
- `actual_amount`: 실제 집행 금액
- `note`: 설명 메모

#### 챗봇 관점 해석 포인트

- 목표 진행률은 `saved_amount / target_amount`로 즉시 계산 가능하다.
- 남은 기간 대비 위험 판단은 `end_date`, `saved_amount`, 최근 `plan_week_allocation` 추세를 같이 봐야 한다.
- "왜 이 목표에 돈이 덜 들어갔는가" 같은 질문은 `plan_week_allocation` 없이는 설명력이 떨어진다.

### 4. 거래/분석 도메인

#### 관계도

```text
transaction_override (N) ---- (1) category

account_transaction_override (1) ---- (1) transaction_override
account_transaction_override (N) ---- (1) account

card_transaction_override    (1) ---- (1) transaction_override
card_transaction_override    (N) ---- (1) card
card_transaction_override    (N) ---- (1) merchant

transaction_analysis_result  (N) ---- (1) transaction_override
transaction_analysis_result  (N) ---- (1) category

users (1) ---- (N) transaction_analysis_metadata

note:
transaction_analysis_metadata 는 사용자별 분석 실행 메타데이터이며,
transaction_analysis_result 와 직접 FK로 연결되지는 않는다.
```

#### 테이블별 핵심 컬럼 역할

##### `transaction_override`

- `id`: 통합 거래 PK
- `category_id`: 원본 혹은 보정 카테고리
- `transactionDateTime`: 거래 시각
- `transactionBalance`: 거래 금액
- `transactionMemo`: 거래 메모

##### `account_transaction_override`

- `id`: `transaction_override`와 1:1로 연결되는 계좌 거래 PK
- `transactionUniqueNo`: 외부 거래 고유번호
- `transactionType`, `transactionTypeName`: 입금/출금 유형
- `transactionAfterBalance`: 거래 후 잔액
- `transactionSummary`: 은행 거래 요약 문구
- `account_id`: 계좌 FK

##### `card_transaction_override`

- `id`: `transaction_override`와 1:1로 연결되는 카드 거래 PK
- `transactionUniqueNo`: 외부 거래 고유번호
- `merchant_id`: 가맹점 FK
- `cardStatus`: 승인 상태
- `billStatementsYn`: 청구 반영 여부
- `billStatementsStatus`: 청구 상태 설명
- `cardUniqueNo`: 카드 FK

##### `transaction_analysis_result`

- `id`: 거래 분석 결과 PK
- `transaction_override_id`: 어떤 거래를 분석했는지
- `category_id`: 최종 분석 카테고리
- `analysis_included`: 분석 포함 여부
- `excluded_reason`: 제외 사유
- `expense_scope`: 소비 범위 분류
- `raw_label`, `final_label`: 모델 원시 라벨, 최종 라벨
- `confidence`: 예측 신뢰도
- `fallback_applied`: 규칙 기반 보정 적용 여부
- `threshold_used`: 분류 기준치
- `analyzed_at`: 분석 시각

##### `transaction_analysis_metadata`

- `id`: 분석 실행 메타데이터 PK
- `users_id`: 분석 대상 사용자
- `requested_months`: 요청 분석 개월 수
- `window_start_at`, `window_end_at`: 분석 기간
- `transaction_count`: 분석 대상 거래 수
- `latest_transaction_at`: 분석 시점 기준 최신 거래
- `analyzed_at`: 분석 실행 시각

#### 챗봇 관점 해석 포인트

- 소비 요약은 `transaction_override` + `transaction_analysis_result` 조합이 핵심이다.
- 계좌/카드별 드릴다운은 하위 override 테이블이 있어야 가능하다.
- 분석 결과 최신성 안내는 `transaction_analysis_metadata.analyzed_at` 없이는 신뢰도 설명이 어렵다.

### 5. 필수지출 스냅샷 도메인

#### 관계도

```text
users (1) ---- (N) essential_expense_snapshot_week
essential_expense_snapshot_week (1) ---- (N) essential_expense_snapshot_week_category
essential_expense_snapshot_week_category (N) ---- (1) category
```

#### 테이블별 핵심 컬럼 역할

##### `essential_expense_snapshot_week`

- `id`: 주간 필수지출 스냅샷 PK
- `users_id`: 사용자 구분
- `year_week`: 연-주차 키
- `week_start_date`, `week_end_date`: 기준 기간
- `base_months`: 기준 산정에 사용한 개월 수
- `essential_amount`: 총 필수지출 기준선
- `fixed_essential_amount`: 고정 필수지출 기준선
- `variable_essential_amount`: 변동 필수지출 기준선
- `analyzed_at`: 산정 시각

##### `essential_expense_snapshot_week_category`

- `id`: 카테고리별 필수지출 스냅샷 PK
- `snapshot_week_id`: 상위 주간 스냅샷 FK
- `category_id`: 카테고리 FK
- `expense_scope`: 고정/변동 범위
- `amount`: 카테고리 기준 금액

#### 챗봇 관점 해석 포인트

- "필수지출이 왜 이 정도로 잡혔는지"는 이 스냅샷 테이블이 핵심 근거다.
- 카테고리별 기준 금액까지 있기 때문에 단순 합계가 아니라 구성 설명이 가능하다.

## DB 구조에서 보이는 핵심 축

- 자산 연결 상태: `account`, `card`
- 예산 집계와 이력: `budget_month`, `budget_week`, `budget_adjustment`, `budget_rollover`
- 목표와 주간 배정: `plan`, `plan_week_allocation`
- 거래 원본과 분석 결과: `transaction_override`, `account_transaction_override`, `card_transaction_override`, `transaction_analysis_result`, `transaction_analysis_metadata`
- 필수지출 기준선: `essential_expense_snapshot_week`, `essential_expense_snapshot_week_category`

## 추천 방향

추천은 `혼합형`이다.

- 기초 조회 툴은 사실 확인용으로 사용한다.
- 요약 조회 툴은 챗봇이 바로 피드백을 생성할 수 있게 만든다.
- "왜 이렇게 판단했는지"를 설명할 수 있도록 이력과 분석 메타데이터를 같이 제공한다.

## 우선순위별 추천 툴

### P1. 바로 도입할 툴

#### 1. `get_weekly_budget_status`

- 목적: 이번 주 예산이 안전한지, 초과 위험이 있는지 설명
- 주요 질문:
  - "이번 주 예산 괜찮아?"
  - "자유예산이 얼마나 남았어?"
  - "이번 주에 조정된 내용이 있어?"
- DB 근거:
  - `budget_week`: 계획 수입, 필수지출, 저축, 이월, 수동조정, 자유예산, 실제지출
  - `budget_adjustment`: 조정 이력
  - `budget_rollover`: 이전 주/다음 주 이월 이력
- 권장 입력:
  - `userId`
  - `baseDate`
  - `includeHistory` 여부
- 권장 출력:
  - 현재 주차 정보
  - `finalFreeAmount`, `actualSpentAmount`
  - 계획 대비 사용률
  - 최근 조정/이월 요약
  - 피드백용 위험 신호 배열
- 챗봇 활용:
  - 예산 소진 속도 피드백
  - 이번 주 주의 메시지 생성
  - 이월/조정이 예산에 미친 영향 설명

#### 2. `get_goal_progress_overview`

- 목적: 목표별 진행률과 달성 속도를 설명
- 주요 질문:
  - "내 목표들 진행 상황 보여줘"
  - "어떤 목표가 가장 위험해?"
  - "지금 속도로 달성 가능해?"
- DB 근거:
  - `plan`: 목표금액, 저장금액, 기간, 우선순위, 상태
  - `plan_week_allocation`: 주차별 배정 계획/최종 반영 금액
- 권장 입력:
  - `userId`
  - `statusFilter`
  - `limit`
- 권장 출력:
  - 목표별 진행률
  - 남은 기간
  - 최근 주차 배정 금액
  - 우선순위와 자동배정 여부
  - 위험/지연 신호
- 챗봇 활용:
  - 목표 달성 가능성 피드백
  - 우선순위 재점검 제안
  - 목표별 설명형 요약

#### 3. `get_spending_summary`

- 목적: 최근 소비 흐름을 요약해서 설명
- 주요 질문:
  - "최근 소비 어때?"
  - "이번 달 소비가 늘었어?"
  - "어디에 돈을 많이 썼어?"
- DB 근거:
  - `transaction_override`: 거래 시각, 금액, 메모
  - `transaction_analysis_result`: 분석 포함 여부, 카테고리, 라벨, 신뢰도, 지출 범위
  - `transaction_analysis_metadata`: 분석 윈도우, 거래 수, 최신 분석 시점
- 권장 입력:
  - `userId`
  - `dateFrom`
  - `dateTo`
  - `scope`
- 권장 출력:
  - 총 지출
  - 분석 포함 거래 수
  - 최근 분석 시점
  - 상위 지출 카테고리
  - 전기 대비 증감 요약
- 챗봇 활용:
  - 최근 소비 총평
  - 분석 데이터 최신성 안내
  - 변동성 요약

#### 4. `get_spending_by_category`

- 목적: 카테고리별 소비 비중과 증가 카테고리를 설명
- 주요 질문:
  - "식비가 얼마나 늘었어?"
  - "가장 많이 쓰는 카테고리는 뭐야?"
- DB 근거:
  - `transaction_analysis_result`
  - `transaction_override`
  - `category`
- 권장 입력:
  - `userId`
  - `dateFrom`
  - `dateTo`
  - `includeComparison`
- 권장 출력:
  - 카테고리별 지출 금액
  - 비중
  - 이전 기간 대비 증감
  - 설명에 사용할 대표 거래 예시
- 챗봇 활용:
  - 카테고리별 코칭
  - 급증 카테고리 중심 피드백

#### 5. `get_recent_spending_anomalies`

- 목적: 평소와 다른 지출 패턴을 짚어줌
- 주요 질문:
  - "최근 이상 지출 있어?"
  - "갑자기 늘어난 소비가 뭐야?"
- DB 근거:
  - `transaction_override`
  - `transaction_analysis_result`
  - 필요 시 `transaction_analysis_metadata`
- 권장 입력:
  - `userId`
  - `lookbackDays`
  - `limit`
- 권장 출력:
  - 이상 지출 후보 목록
  - 카테고리
  - 금액
  - 기준 대비 증가량 또는 특이 사유
  - 최근성
- 챗봇 활용:
  - 경고성 피드백
  - 예산 리스크 설명

### P2. 설명 품질을 높이는 툴

#### 6. `get_budget_change_history`

- 목적: 예산이 왜 바뀌었는지 설명
- DB 근거:
  - `budget_adjustment`
  - `budget_week`
  - 선택적으로 `plan`
- 출력 핵심:
  - 조정 종류
  - 금액
  - 사유
  - 연관 목표
- 활용:
  - "왜 자유예산이 줄었어?" 같은 질문 대응

#### 7. `get_budget_rollover_history`

- 목적: 주간 이월 흐름 설명
- DB 근거:
  - `budget_rollover`
  - `budget_week`
- 출력 핵심:
  - source/target 주차
  - 이월 금액
  - 상태
  - 노트
- 활용:
  - 주간 마감 후 금액 이동 설명

#### 8. `get_goal_allocation_breakdown`

- 목적: 특정 목표에 얼마가 어떻게 배정되었는지 설명
- DB 근거:
  - `plan`
  - `plan_week_allocation`
  - 필요 시 `budget_week`
- 출력 핵심:
  - 주차별 planned/final/actual 금액
  - 자동/수동 배정 구분
  - note
- 활용:
  - "왜 이 목표에 돈이 적게 들어갔어?" 대응

#### 9. `get_essential_spending_baseline`

- 목적: 필수지출 기준선과 현재 상태 비교
- DB 근거:
  - `essential_expense_snapshot_week`
  - `budget_month`
  - `budget_week`
- 출력 핵심:
  - 고정/변동 필수지출 기준선
  - 분석 기준 주차
  - 현재 예산과 차이
- 활용:
  - 필수지출이 과소/과대 책정되었는지 설명

#### 10. `get_asset_linkage_status`

- 목적: 연결된 계좌/카드 상태와 최근 활동 가능성 확인
- DB 근거:
  - `account`
  - `card`
  - 필요 시 거래 override 테이블의 최근 거래 시각
- 출력 핵심:
  - 활성/비활성 계좌 수
  - 검증 여부
  - 연결 카드 수
  - 최근 활동 시각
- 활용:
  - 데이터 신뢰도 안내
  - "왜 거래가 안 보이지?" 같은 질문 대응

### P3. 후속 대화용 상세 툴

#### 11. `get_recent_account_activity`

- 목적: 계좌 거래 흐름 드릴다운
- DB 근거:
  - `account_transaction_override`
  - `account`
- 출력 핵심:
  - 최근 거래 목록
  - 입출금 유형
  - 거래 후 잔액
  - 요약 문구

#### 12. `get_recent_card_activity`

- 목적: 카드 사용 흐름 드릴다운
- DB 근거:
  - `card_transaction_override`
  - `card`
  - `merchant`
- 출력 핵심:
  - 최근 승인 거래
  - 가맹점
  - 청구 여부
  - 카드별 사용 흐름

#### 13. `get_transaction_drilldown`

- 목적: 특정 기간의 대표 거래를 근거로 설명
- DB 근거:
  - `transaction_override`
  - `transaction_analysis_result`
  - 하위 override 테이블
- 출력 핵심:
  - 거래 시각
  - 금액
  - 메모
  - 카테고리
  - 분석 라벨/신뢰도

#### 14. `get_essential_category_breakdown`

- 목적: 필수지출이 어떤 카테고리로 구성되는지 설명
- DB 근거:
  - `essential_expense_snapshot_week_category`
  - `essential_expense_snapshot_week`
  - `category`
- 출력 핵심:
  - 카테고리별 기준 금액
  - fixed/variable essential 구분

#### 15. `get_goal_risk_signals`

- 목적: 목표 지연 가능성을 정리해서 보여줌
- DB 근거:
  - `plan`
  - `plan_week_allocation`
- 출력 핵심:
  - 남은 기간 대비 부족 금액
  - 최근 배정 추세
  - 비활성/취소/대기 상태 여부

## 툴 설계 원칙

### 1. 모든 툴은 사용자 범위를 강제

- `userId` 또는 인증 컨텍스트 기준으로만 조회
- 다른 사용자 데이터 혼합 금지

### 2. 모든 응답에 시간 범위를 넣기

- `asOf`
- `dateFrom`
- `dateTo`
- `analyzedAt`

챗봇이 "최근", "이번 주", "지난달"을 설명할 때 근거를 명시할 수 있어야 한다.

### 3. 요약과 근거를 함께 반환

예:

- `summary`: 총지출, 진행률, 남은 자유예산
- `evidence`: 상위 카테고리, 최근 조정 이력, 대표 거래 3건

이 구조가 있어야 챗봇이 과한 추측 없이 설명 가능하다.

### 4. 데이터 부족 상태를 명시

- 거래 분석 미실행
- 최근 데이터 없음
- 연결 계좌 없음
- 비활성 목표만 존재

이런 상태는 `warnings`로 직접 반환하는 것이 안전하다.

### 5. 돈 변경 액션은 분리

이번 문서의 툴은 조회 전용으로 제한한다.

- 목표 추가 저축
- 목표 간 이동
- 예산 조정
- 주간 마감/이월

이런 기능은 별도 트랜잭션 설계와 확인 절차가 필요하다.

## 추천 MVP 조합

처음 도입은 아래 5개면 충분하다.

1. `get_weekly_budget_status`
2. `get_goal_progress_overview`
3. `get_spending_summary`
4. `get_spending_by_category`
5. `get_recent_spending_anomalies`

이 조합이면 챗봇이 아래 영역의 피드백을 바로 줄 수 있다.

- 예산 상태 피드백
- 목표 달성 피드백
- 최근 소비 요약
- 과소비/이상지출 경고

## 구현 시 권장 응답 형태

```json
{
  "asOf": "2026-03-26T10:30:00+09:00",
  "window": {
    "dateFrom": "2026-03-01",
    "dateTo": "2026-03-26"
  },
  "summary": {},
  "evidence": [],
  "warnings": []
}
```

## 결론

현재 DB는 단순 조회보다 `설명 가능한 조회`에 더 잘 맞는다.

따라서 에이전트형 챗봇 툴은 다음 방향이 적합하다.

- 원시 데이터만 주는 툴 몇 개
- 예산/목표/소비를 설명 가능한 요약으로 주는 툴 몇 개
- 변경 기능은 제외하고 조회 기반 피드백에 집중

이 구조로 가면 챗봇이 "상태 조회"를 넘어서 "왜 이런 상태인지"까지 일관되게 답할 수 있다.
