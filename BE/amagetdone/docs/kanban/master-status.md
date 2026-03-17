# Master Status

## Maintenance Rule

- 이 파일은 `docs/kanban/master-status.md` 고정 경로를 계속 업데이트하는 canonical status 문서다.
- 새로운 날짜별 status 파일을 만들지 말고, 현재 상태를 이 파일에 누적 반영한다.
- 작업을 완료한 뒤에는 반드시 이 문서를 최신 브랜치 상태 기준으로 갱신한다.

## Snapshot

- Branch: `feat/back/auth`
- Remote relation: `origin/feat/back/auth` 대비 `ahead 1`
- HEAD: `19c083e` (`docs: record budget engine absorption results`)
- Working tree: docs 작업 중
- HEAD와 원격 브랜치의 차이: 원격보다 앞선 기존 docs 커밋 1개 위에 로컬 문서 변경이 추가된 상태

## Current Interpretation

이 브랜치는 2026-03-17 기준으로 크게 세 작업축을 포함한다.

1. 금융회원 재연결 규칙 정리
2. 테스트 소셜 로그인과 금융회원 연동 연결
3. budget-engine / budget-home 관련 워킹트리 변경의 선택 흡수

여기에 추가로, 같은 날 API 문서 동기화 작업이 진행됐다.

4. `docs/api/` 로컬 API 명세 작성
5. Notion API 명세의 `account`, `member`, `budget` 도메인 동기화
6. scope 확정 전에 생성된 non-target Notion 페이지를 `issues/`로 추적
7. Notion API 명세 레이아웃을 기존 도메인 페이지 + inline 데이터베이스 형식으로 재정렬

세 작업 모두 "설계 또는 구현 중"이라기보다, 이미 브랜치에 반영된 결과를 문서와 커밋으로 정리한 상태에 가깝다. 따라서 후속 에이전트는 새 기능을 시작하기 전에 "무엇이 이미 끝났는지"부터 확인해야 한다.

## Recent Timeline

### 1. 계좌 검증 흐름 선행 변경이 먼저 들어왔다

- `a7221d6` `refactor: register account in DB only after 1-won verification succeeds`
- `2347d86` `fix: update verification tests to match refactored UseCase signatures`

이 구간에서 계좌 등록/검증 흐름이 크게 바뀌었다. 이후 member-link 작업들은 이 흐름 위에서 돌아간다.

### 2. 금융회원 재연결 규칙이 member flow에 반영됐다

관련 문서:

- `docs/plans/2026-03-17-fin-member-relink-design.md`
- `docs/plans/2026-03-17-fin-member-relink.md`

핵심 내용:

- `EnsureFinMemberLinkedUseCase.execute(currentUserId, email)`로 current user aware 하게 변경
- SSAFY 연동 순서는 그대로 `create -> duplicate면 search`
- 찾은 `userKey`는 `UserRepository.findByUserKey(...)`로 로컬 소유권 검사
- 미점유이거나 현재 사용자 본인이면 허용
- 다른 로컬 사용자가 이미 점유 중이면 `FINANCIAL_MEMBER_ALREADY_LINKED (MEMBER_020)`로 실패

관련 코드:

- `src/main/java/com/ssafy/amagetdone/domain/member/application/usecase/EnsureFinMemberLinkedUseCase.java`
- `src/main/java/com/ssafy/amagetdone/domain/member/application/usecase/CompleteMemberProfileUseCase.java`
- `src/main/java/com/ssafy/amagetdone/domain/member/exception/MemberErrorCode.java`
- `src/main/java/com/ssafy/amagetdone/domain/member/model/repository/UserRepository.java`

주의:

- 문서는 설계/구현 계획 형태지만, 실제 코드는 이미 위 방향으로 반영돼 있다.
- 다만 이 요약 작성 시점에는 해당 targeted test를 다시 돌리지는 않았다.

### 3. 테스트 소셜 로그인도 같은 금융회원 연동 경로를 쓰도록 정리됐다

관련 문서:

- `docs/plans/2026-03-17-test-social-fin-link.md`

완료 커밋:

- `2000c1e` `feat: link test social login to fin member`

핵심 내용:

- TEST 소셜 로그인 시 synthetic `userKey`를 미리 심지 않음
- 고정 이메일 `test@test.com`으로 SSAFY 금융회원 생성/조회 후 결과 `userKey` 저장
- `MemberAuthService.authenticateTest()`가 `EnsureFinMemberLinkedUseCase`를 재사용
- 테스트 소셜 사용자 bootstrap은 nickname/email 중심으로 단순화

관련 코드:

- `src/main/java/com/ssafy/amagetdone/domain/member/application/service/MemberAuthService.java`
- `src/main/java/com/ssafy/amagetdone/domain/member/application/usecase/FindOrCreateSocialUserUseCase.java`
- `src/main/resources/application.properties`
- member 관련 테스트들

검증 메모:

- 문서에는 관련 테스트 묶음 실행 결과가 `BUILD SUCCESSFUL`로 기록돼 있다.
- 최근 git log상 이 변경은 budget 관련 커밋들보다 앞에 있고, 현재 브랜치 히스토리에 포함돼 있다.

### 4. budget-engine 변경은 이미 선택 흡수되어 커밋됐다

관련 문서:

- `docs/plans/2026-03-17-budget-engine-selective-absorption.md`

완료 커밋:

- `d7e2c6c` `feat: absorb budget engine working tree changes`
- `19c083e` `docs: record budget engine absorption results`

핵심 내용:

- budget 도메인 변경 대량 반영
- 필요한 최소 cross-domain support만 함께 유지
- 허용된 support 범위는 주로 `plan`, `transaction`, `global/docs`, 공용 테스트 지원

대표 경로:

- `src/main/java/com/ssafy/amagetdone/domain/budget/**`
- `src/test/java/com/ssafy/amagetdone/domain/budget/**`
- `src/main/java/com/ssafy/amagetdone/domain/plan/**`
- `src/main/java/com/ssafy/amagetdone/domain/transaction/**`
- `src/main/java/com/ssafy/amagetdone/global/docs/controller/BudgetDocsController.java`
- `src/test/java/com/ssafy/amagetdone/support/TestSystemPropertySupport.java`

검증 메모:

- budget service 테스트 3종 실행
- `BudgetControllerTest` 실행
- 전체 검증은 plain `build`가 아니라 `build -x test`로 확인
- WSL mounted path에서 JUnit XML report 쓰기 문제가 있어 init script로 XML 생성을 끄는 우회가 있었다

## What Is Already Done

- 금융회원 재연결 규칙은 코드 기준 반영된 상태로 보인다.
- 테스트 소셜 로그인과 금융회원 연동 연결은 커밋 `2000c1e`로 완료 기록이 있다.
- budget-engine 선택 흡수는 커밋 `d7e2c6c`로 완료됐고, 후속 문서 기록 커밋 `19c083e`가 추가됐다.
- 현재 브랜치가 원격보다 앞선 내용은 docs 커밋 하나뿐이다.
- `docs/api/README.md`와 도메인별 API 문서 8개가 작성됐다.
- Notion API 명세는 `account`, `member`, `budget` 기준으로 request/response 정합성이 맞춰졌다.
- `plan`, `transaction`, `global` Notion 페이지는 scope 확정 전에 일부 생성/수정되었고, 후속 판단은 `issues/`에 남겼다.
- `소비 분석 관리`는 기존 양식에 맞게 도메인 페이지 안의 inline 데이터베이스 구조로 재정렬됐다.
- `헬스 체크`는 top-level stray 상태가 아니라 `설정 관리` inline 데이터베이스 아래로 이동됐다.

## What Is Not Proven By This Summary

- member 관련 targeted test를 "지금 이 시점"에 재실행한 결과
- budget 전체 테스트 포함 `./gradlew build --no-daemon`의 성공
- 문서에 적힌 과거 실행 결과 외의 새로운 검증

## Known Caveats

- `docs/plans/2026-03-17-budget-engine-selective-absorption.md`는 `.sisyphus/plans/budget-engine.md`, `.sisyphus/plans/budget-home-aggregate.md`를 참조하지만, 현재 워크트리에서는 해당 경로를 찾지 못했다.
- budget 문서는 "새 작업 계획"이 아니라 "선택 흡수 완료 기록"으로 읽어야 한다.
- 후속 작업 전에는 항상 현재 `git status`와 `git log`를 다시 확인해야 한다. 위 본문은 2026-03-17 기준으로 마지막 크게 정리된 스냅샷이다.
- `docs/api/` 문서 중 일부 error response 예시는 controller test가 아닌 DTO/기존 명세 기반 보강이라, 추후 통합 테스트 기준 재검증 여지가 있다.
- `GET /api/budgets/weeks/{yearWeek}/essential-expenses`는 코드 내 응답 메시지 문자열이 깨져 있어 Notion/로컬 문서 모두 `data` 구조 중심으로 정리했다.
- non-target Notion 작업 이슈는 아래 문서에 남겼다.
  - `issues/2026-03-17-non-target-notion-api-pages.md`
- Notion API 명세를 수정할 때는 개별 엔드포인트 페이지를 최상위 `API 명세서` 데이터베이스에 직접 두지 말고, 반드시 도메인 페이지 내부 inline 데이터베이스 아래로 배치해야 한다.
- `issues/` 디렉토리는 해결된 이슈를 누적 보관하는 곳이 아니라 현재 후속 판단이 필요한 이슈만 유지하는 working set으로 관리해야 한다.

## Practical Takeaway For Another Agent

- member 영역을 다룬다면 먼저 `EnsureFinMemberLinkedUseCase` 계약이 `execute(userId, email)`로 바뀌었다는 점을 전제로 시작해야 한다.
- test social login을 만진다면 `test@test.com` 기반 실제 금융회원 연동 흐름을 깨지 말아야 한다.
- budget 영역을 만진다면 `d7e2c6c`가 이미 대규모 흡수를 끝낸 상태라는 점을 기억하고, 중복 흡수나 범위 재분류를 다시 하지 말아야 한다.
- API 문서 작업을 이어받는다면 Notion 수정 범위는 우선 `account`, `member`, `budget`만 유지하고, 그 외 도메인은 `issues/` 판단 원칙을 따른다.
- Notion에서 layout을 손댈 때는 먼저 도메인 페이지와 inline 데이터베이스가 맞는지부터 확인한다.
- 이슈를 하나 해결했다면 관련 `issues/` 문서와 handoff 참조를 바로 갱신하거나 제거한다.
- 로컬 API 문서의 canonical 경로는 `docs/api/`다.
