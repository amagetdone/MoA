# LangGraph Read Tools Design

**Date:** 2026-03-28

## Goal

Spring 백엔드의 조회 엔드포인트 중 자산, 소비 분석(거래내역), 예산 도메인 로직을 LangGraph 서버에서 직접 호출 가능한 Python tool 집합으로 추출한다. LangGraph 서버는 별도 프로세스로 동작하므로 Spring 인증 컨텍스트 대신 `user_id`를 직접 입력받고, DB는 Spring과 동일한 PostgreSQL을 사용한다.

## Scope

다음 조회 계열만 대상으로 한다.

- 자산: `AccountController`의 조회 기능
- 소비 분석: `TransactionController`의 조회 기능
- 예산: `BudgetController`의 조회 기능

제외 범위:

- 쓰기/조정/동기화 엔드포인트
- 외부 SSAFY API 재호출
- Spring 보안/토큰 처리

## Architecture

프로젝트 루트에 단일 `tools.py` 파일을 둔다. 이 파일은 LangGraph 서버에서 바로 import 가능해야 하므로 별도 패키징 없이 독립 실행 가능한 구조를 사용한다.

핵심 구조는 다음과 같다.

1. 환경변수 기반 DB 연결 생성
2. 공통 파싱/응답 헬퍼
3. 도메인별 내부 조회 함수
4. `@tool` 데코레이터가 붙은 공개 tool 함수

DB 접근은 `sqlalchemy.create_engine` + `text()` 기반의 읽기 전용 쿼리로 구성한다. Spring 엔티티를 Python ORM으로 다시 만들지 않고, 현재 조회 로직에서 실제로 사용하는 테이블만 직접 조회한다.

## Tool Strategy

Spring 엔드포인트와 1:1에 가까운 tool 세트를 유지하되, 응답은 LangGraph 친화적으로 통일한다.

예정 tool 목록:

- 자산
  - `get_linked_accounts(user_id, include_recent_activity=False)`
  - `get_account_detail(user_id, account_no, include_recent_activity=True)`
  - `get_linked_cards(user_id)`
  - `get_asset_linkage_status(user_id, lookback_days=30)`
- 소비 분석
  - `get_transaction_list(user_id, from_date=None, to_date=None, analysis_inclusion="ALL", page=0, size=20)`
  - `get_monthly_expense_summary(user_id, month=None)`
  - `get_weekly_expense_graph(user_id, year_week)`
  - `get_monthly_expense_graph(user_id, year_month)`
  - `get_onboarding_spending_analysis(user_id)`
  - `get_weekly_spending_analysis(user_id, year_week)`
  - `get_monthly_spending_analysis(user_id, year_month)`
- 예산
  - `get_budget_income(user_id)`
  - `get_budget_dashboard(user_id)`
  - `get_budget_home(user_id)`
  - `get_budget_summary(user_id)`
  - `get_weekly_savings_detail(user_id, year_week)`
  - `get_weekly_free_spending_detail(user_id, year_week)`
  - `get_weekly_essential_expenses(user_id, year_week)`
  - `get_weekly_essential_expense_detail(user_id, year_week)`

## Unified Response Shape

모든 tool은 다음 구조를 반환한다.

```json
{
  "tool_name": "get_budget_dashboard",
  "as_of": "2026-03-28T12:00:00+09:00",
  "window": {
    "date_from": "2026-03-24",
    "date_to": "2026-03-30"
  },
  "summary": {},
  "evidence": [],
  "warnings": [],
  "raw": {}
}
```

의도는 다음과 같다.

- `summary`: LLM이 바로 답변에 사용할 핵심 정보
- `evidence`: 요약을 뒷받침하는 근거 목록
- `warnings`: 데이터 부족, 분석 미완료, 현재 예산 주차 부재 같은 상태 표시
- `raw`: 세부 구조화 데이터

## Domain Decisions

### Asset

Spring은 일부 조회에서 외부 SSAFY API로 현재 잔액을 다시 조회한다. 이번 도구는 DB 직접 조회만 허용되므로 외부 호출은 제외한다.

대신 다음 기준을 사용한다.

- 계좌/카드 연결 상태
- 계좌 검증 여부
- 계좌 활성 여부
- 최근 거래 시점
- 최근 거래 샘플
- 잔액은 제공하지 않거나, 최근 거래 기반 추정치가 불가능하면 `warnings`로 명시

### Transaction

Spring `TransactionQueryService`와 `RealTransactionAnalysisResponseFacade`가 사용하는 규칙을 Python으로 옮긴다.

- 계좌/카드 거래를 합쳐 unified projection 구성
- 최신 분석 메타데이터를 기준으로 분석 결과 결합
- `INCOME`, `INTERNAL_TRANSFER`, `NON_ESSENTIAL` 등의 제외 규칙 반영
- 월간 요약, 주간/월간 그래프, 온보딩/주간/월간 분석 응답 재구성

### Budget

Spring 조회 서비스들의 계산 규칙을 그대로 따른다.

- 현재 주차 조회
- 주간 자유지출 잔액, 일일 사용 가능 금액
- 주간 저축 배정/실적 집계
- 필수지출 스냅샷과 실제 거래 매핑
- 예산 요약과 월 수입 조회

## Parsing Rules

파싱 규칙은 Spring과 동일하게 유지한다.

- 예산 상세용 `year_week`: `YYYY-Www`
- 거래 월: `YYYY-MM`
- 거래 목록 기본 날짜:
  - `to_date = today`
  - `from_date = to_date - 1 month`

형식 오류는 예외로 처리한다.

## Error Handling

정책은 "답변 가능하면 경고 반환, 진행 불가면 예외"다.

예외:

- 필수 파라미터 형식 오류
- 특정 계좌/주차를 찾을 수 없음
- DB 연결 실패

경고:

- 분석 메타데이터 없음
- 최근 거래 없음
- 잔액 산출 불가
- 현재 예산 주차 없음
- 카테고리/분석 결과 일부 누락

## Security

- `.env`, 토큰, 자격증명 출력 금지
- DB 연결값은 환경변수만 사용
- SQL은 모두 읽기 전용
- 로그가 필요하더라도 민감정보는 마스킹

## Testing Approach

Python 테스트 인프라가 없는 저장소이므로 최소 범위의 `unittest` 기반 테스트를 추가한다.

테스트 대상:

- 날짜/주차 파싱
- 공통 응답 빌더
- 경고 생성 규칙
- 대표 tool의 요약 조립 함수

DB 연결이 필요한 통합 검증은 구현 후 수동 스모크 테스트로 확인한다.

## Implementation Notes

- `tools.py`는 루트에 생성
- SQL은 도메인별 섹션으로 정리
- 공개 함수 목록은 `__all__`에 명시
- LangGraph 서버가 리스트로 가져갈 수 있도록 `TOOLS` 상수도 함께 제공
