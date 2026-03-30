# Spring AI MCP Wrapper Design

**Date:** 2026-03-28

## Goal

Spring Boot 서버 내부의 기존 조회 로직을 그대로 재사용해서, 예산, 자산, 소비분석, 거래내역을 LangGraph 에이전트가 원격 호출할 수 있는 MCP 서버로 노출한다. 기존 REST API, 인증 흐름, 서비스 로직에는 영향을 주지 않고, MCP 전용 입력으로 `userId`를 직접 받는다.

## Scope

조회 전용 MCP만 추가한다.

- 예산
  - 현재 월 예산 수입
  - 예산 대시보드
  - 예산 홈
  - 예산 요약
  - 주간 저축 상세
  - 주간 자유지출 상세
  - 주간 필수지출 스냅샷
  - 주간 필수지출 상세
- 자산
  - 연결 계좌 목록
  - 계좌 상세
  - 연결 카드 목록
- 거래내역
  - 거래 목록
- 소비분석
  - 월간 지출 요약
  - 주간 지출 그래프
  - 월간 지출 그래프
  - 온보딩 소비분석
  - 주간 소비분석
  - 월간 소비분석

제외 범위:

- 거래 동기화 강제 실행용 MCP
- 분석 실행용 MCP
- 예산 조정, 저축 이동, 초기화 같은 쓰기 기능
- 기존 컨트롤러 인증 방식 변경

## Dependency Decision

프로젝트는 현재 Spring Boot `3.5.11`, Java 17, MVC 기반 서버에 `spring-webflux`가 보조적으로 포함된 구조다. MCP 추가는 기존 MVC 서버와 분리된 read-only 어댑터 계층으로 붙이는 것이 가장 안전하다.

Spring AI 의존성은 다음 원칙으로 추가한다.

1. `spring-ai-bom`으로 버전을 정렬한다.
2. HTTP 원격 MCP 서버 구성이므로 `spring-ai-starter-mcp-server-webmvc`만 사용한다.
3. Boot, Spring Framework, Jackson, WebMVC/WebFlux 전이 의존성 충돌 여부를 `dependencyInsight`로 먼저 확인한다.
4. 충돌이 있으면 starter 종류를 바꾸지 않고 BOM/버전 정렬부터 해결한다.

확인 기준:

- 기존 `spring-boot-starter-web`과 라우팅 충돌이 없어야 한다.
- `spring-webflux`가 이미 포함돼 있어도 MCP는 MVC starter만 사용해야 한다.
- 기존 `ChatbotApiClient`의 WebClient 사용에는 영향이 없어야 한다.

공식 참고:

- Spring AI Getting Started
- Spring AI MCP Server Boot Starter docs
- Spring AI Streamable HTTP Server Boot Starter docs

## Architecture

MCP는 새 도메인 `domain/mcp`로 분리한다.

권장 패키지 구조:

```text
domain/mcp
├── api
│   ├── config
│   └── dto
├── application
│   ├── service
│   └── tool
└── support
    └── util
```

구성 요소:

1. `McpProperties`
   - MCP 활성 여부, base path, server name 같은 전용 설정 보관
2. `McpServerConfig`
   - Spring AI MCP server bean 등록
   - 기본값은 비활성
3. `BudgetMcpQueryAdapter`
   - 기존 budget usecase 호출 전용
4. `AssetMcpQueryAdapter`
   - `userId` 기반 자산 조회 전용
   - 기존 token/controller 경로를 우회하고 service/usecase/repository만 재사용
5. `TransactionMcpQueryAdapter`
   - 거래내역 및 소비분석 조회 전용
6. `FinanceReadMcpTools`
   - Spring AI tool 메서드 집합
7. `McpExceptionTranslator`
   - MCP 호출자가 이해할 수 있는 예외 메시지 정리

## Data Flow

### Budget

`FinanceReadMcpTools` -> `BudgetMcpQueryAdapter` -> 기존 `Get...UseCase`

Budget는 read-sync를 포함한 기존 조회 동작을 그대로 사용한다. 따라서 MCP 조회에서도 현재 서비스와 동일하게 `SyncBudgetStateOnReadUseCase`가 호출될 수 있다. 이는 신규 부작용이 아니라 기존 조회 규칙 재사용이다.

사용 대상:

- `GetBudgetIncomeUseCase`
- `GetBudgetDashboardUseCase`
- `GetBudgetHomeUseCase`
- `GetBudgetSummaryUseCase`
- `GetWeeklySavingsDetailUseCase`
- `GetWeeklyFreeSpendingDetailUseCase`
- `GetWeeklyEssentialExpensesUseCase`
- `GetWeeklyEssentialExpenseDetailUseCase`

### Asset

자산은 기존 `AccountService`를 그대로 쓰면 HTTP 인증 전제를 끌어오기 쉬우므로 MCP 전용 adapter를 둔다.

흐름:

`FinanceReadMcpTools` -> `AssetMcpQueryAdapter` -> 기존 usecase/service/repository 조합

재사용 대상:

- `ReconcileLinkedAccountsUseCase`
- `ListAccountsUseCase`
- `FindUserAccountUseCase`
- `InquireAccountBalanceUseCase`
- `CardRepository`

핵심 원칙:

- 기존 controller는 건드리지 않는다.
- 계좌/카드 조회 조건은 기존 서비스와 동일하게 맞춘다.
- `userId`는 MCP 입력에서 직접 받는다.

### Transaction / Spending Analysis

`FinanceReadMcpTools` -> `TransactionMcpQueryAdapter` -> 기존 query facade/usecase

재사용 대상:

- `ListTransactionsUseCase`
- `TransactionAnalysisResponseFacade`

이 계층은 현재 `transaction.analysis-response-mode=real` 설정 기준의 실제 서비스 로직을 그대로 따른다.

## MCP Tool Contract

모든 tool은 `userId`를 첫 입력으로 받는다.

예시:

- `getBudgetDashboard(int userId)`
- `getAccountDetail(int userId, String accountNo)`
- `getTransactionList(int userId, LocalDate fromDate, LocalDate toDate, String analysisInclusion, Integer page, Integer size)`
- `getMonthlySpendingAnalysis(int userId, String yearMonth)`

응답 전략:

- 가능하면 기존 DTO를 그대로 반환한다.
- MCP를 위해 별도 summary/raw 래퍼를 덧씌우지 않는다.
- 이유: "Spring 서버 기준으로 래핑" 요구사항을 만족하려면 service 결과 shape를 최대한 그대로 유지하는 편이 안전하다.

## Endpoint Exposure

전송 방식은 WebMVC 기반 Streamable HTTP를 사용한다.

원칙:

- 기존 REST endpoint와 분리된 `/mcp` 계열 경로 사용
- 기본값 비활성
- 명시적 프로퍼티 활성화 시에만 MCP 서버 노출

예상 설정:

- `mcp.finance.enabled=false`
- `spring.ai.mcp.server.enabled=${mcp.finance.enabled:false}`
- `spring.ai.mcp.server.name=finance-read-mcp`

세부 키 이름은 실제 Spring AI starter가 요구하는 프로퍼티 명세에 맞춘다.

## Error Handling

기존 예외 체계를 존중하되, MCP 호출자는 HTTP controller 계층이 아니라 tool 호출자이므로 메시지를 좀 더 직접적으로 노출한다.

처리 원칙:

- 잘못된 입력 포맷: `IllegalArgumentException`
- 조회 대상 없음: 기존 `NotFound` 계열 또는 명확한 runtime exception 유지
- 외부 API 불가용:
  - Asset 조회 중 실시간 잔액 조회 실패 시 기존 예외를 그대로 노출하거나, adapter에서 어떤 account 조회가 실패했는지 문맥을 덧붙인다
- 예산 read-sync 중 실패:
  - 기존 usecase 예외를 그대로 전달

기존 `GlobalExceptionHandler`는 MCP tool 호출 경로에서 핵심 진입점이 아니므로, adapter 또는 tool layer에서 최소한의 예외 정리만 수행한다.

## Non-Impact Guarantees

기존 로직에 영향이 없어야 하므로 다음을 지킨다.

1. 기존 controller, service, usecase의 공개 메서드 시그니처는 바꾸지 않는다.
2. 기존 인증 필터, `AuthenticatedMember`, JWT 처리 로직은 수정하지 않는다.
3. MCP는 새 패키지와 새 설정만 추가한다.
4. 기존 엔드포인트 경로를 공유하지 않는다.
5. 기존 feature flag 기본값을 바꾸지 않는다.

## Testing Strategy

TDD로 구현한다.

우선순위:

1. 의존성 충돌 검증
   - `build.gradle` 변경 후 dependency graph 확인
2. MCP 비활성 시 기존 컨텍스트 보존 테스트
3. MCP 활성 시 tool bean 등록 테스트
4. adapter 단위 테스트
5. 대표 통합 테스트

대표 통합 테스트 범위:

- 예산 1건
  - MCP budget dashboard -> 기존 `GetBudgetDashboardUseCase`와 동일 값
- 자산 1건
  - MCP linked accounts -> 기존 자산 조회 조합과 동일 값
- 거래내역 1건
  - MCP transaction list -> 기존 `ListTransactionsUseCase`와 동일 값
- 소비분석 1건
  - MCP monthly spending analysis -> 기존 facade 결과와 동일 값

회귀 확인:

- 기존 controller/service 테스트가 깨지지 않아야 한다.
- MCP 비활성 상태에서 기존 API 테스트가 동일하게 통과해야 한다.

## Open Decisions Resolved

- 전송 방식: `Streamable HTTP`
- 노출 범위: `조회 전용만`
- 구현 방식: `Spring AI MCP WebMVC Server + 전용 adapter service`
- 입력 방식: `AccessToken` 대신 MCP input의 `userId`

## Implementation Handoff

다음 단계는 구현 계획 문서 작성이다. 계획은 TDD 순서, 파일 단위 작업, 검증 명령, 커밋 분리 전략까지 포함해서 작성한다.
