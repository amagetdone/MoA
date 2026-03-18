# Master Status

## Maintenance Rule

- 이 파일은 `docs/kanban/master-status.md` 고정 경로를 계속 업데이트하는 canonical status 문서다.
- 새로운 날짜별 status 파일을 만들지 말고, 현재 상태를 이 파일에 누적 반영한다.
- 작업을 완료한 뒤에는 반드시 이 문서를 최신 브랜치 상태 기준으로 갱신한다.

## Snapshot

- Branch: `ref/back/account`
- Remote relation: `origin/ref/back/account` tracking, 현재 로컬 브랜치는 upstream과 동일하다
- HEAD: `544904a` (`chore: externalize swagger base url`)
- Working tree: account 관련 tracked source 변경이 남아 있고, 여기에 `domain/mypage` 신규 코드/테스트, `SwaggerConfigIntegrationTest` 수정, `docs/issues/`, `docs/api/mypage.md`, `docs/plans/2026-03-18-mypage-profile*.md`가 함께 있는 dirty 상태다
- 브랜치 관계: `origin/dev/backend`는 `56b7be5`에서 `ref/back/account`의 핵심 account 작업을 이미 머지했고, 현재 브랜치에는 후속 정리 커밋 `e1ac6ae`, `544904a`가 추가로 올라가 있다

## Current Interpretation

현재 브랜치는 원래 `feat/back/auth`에서 진행되던 account-link refactor만 분리해 `origin/dev/backend` 기준으로 다시 세운 작업 브랜치였다. 다만 지금은 "로컬 워크트리에서 account 작업이 계속 진행 중"인 상태가 아니라, 핵심 account 작업이 이미 `origin/dev/backend`의 `56b7be5`로 흡수된 뒤 후속 정리 커밋 두 개가 추가된 상태로 보는 편이 맞다.

따라서 아래의 member/budget/API 문서 타임라인은 여전히 유효한 배경 맥락이지만, 문서 해석 시에는 아래 세 층위를 구분해야 한다.

1. account 브랜치 분기 전후의 배경 맥락
2. `dev/backend`에 이미 흡수된 account-link refactor 결과
3. 현재 `ref/back/account`에만 남아 있는 후속 정리 커밋 (`e1ac6ae`, `544904a`)

사용자 확인 기준 현재 account 작업은 일단락된 상태로 간주하고, 후속 에이전트는 "미완성 로컬 구현"을 이어받는다는 전제로 시작하지 말아야 한다.

배경 브랜치(`feat/back/auth`)에는 2026-03-17 기준으로 크게 세 작업축이 있었다.

1. 금융회원 재연결 규칙 정리
2. 테스트 소셜 로그인과 금융회원 연동 연결
3. budget-engine / budget-home 관련 워킹트리 변경의 선택 흡수

여기에 추가로, 같은 날 API 문서 동기화 작업이 진행됐고, 이후 account-link refactor 설계/구현과 후속 정리가 별도 브랜치에서 마무리됐다.

4. `docs/api/` 로컬 API 명세 작성
5. Notion API 명세의 `account`, `member`, `budget` 도메인 동기화
6. scope 확정 전에 생성된 non-target Notion 페이지를 `issues/`로 추적
7. Notion API 명세 레이아웃을 기존 도메인 페이지 + inline 데이터베이스 형식으로 재정렬
8. account / accountverification 리팩터링 설계 문서 추가
9. account / accountverification 리팩터링 구현 및 후속 정리
10. `GET /v1/mypage` 단일 조회 설계/구현/문서화 작업 추가

위 항목들은 이제 모두 브랜치 히스토리와 문서에 남아 있는 완료 맥락으로 읽는 편이 맞다. 따라서 후속 에이전트는 새 기능을 시작하기 전에 "이미 `dev/backend`에 들어간 것", "현재 `ref/back/account`에만 남아 있는 후속 정리 커밋", "아직 손대지 않은 새 요구사항"을 구분해야 한다.

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

- 현재 실행 plan 문서는 제거했고, 아래 요약과 커밋 기록을 canonical history로 유지한다.

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
- 완료된 test-social-fin-link 실행 plan 문서는 정리했고, 해당 작업 이력은 이 문서와 commit history에만 남긴다.
- budget-engine 선택 흡수는 커밋 `d7e2c6c`로 완료됐고, 후속 문서 기록 커밋 `19c083e`가 추가됐다.
- 현재 로컬 브랜치는 `origin/ref/back/account`와 동기화되어 있다.
- `docs/api/README.md`와 도메인별 API 문서 9개가 작성됐다.
- `domain/mypage`에 `GET /v1/mypage` 조회 코드와 테스트, Swagger 노출이 추가됐다.
- `docs/api/mypage.md`와 `docs/plans/2026-03-18-mypage-profile*.md`가 추가되어 `GET /v1/mypage` 작업축이 코드/문서로 분리됐다.
- Notion API 명세는 `account`, `member`, `budget` 기준 정합화를 마쳤고, `mypage`는 기존 `마이페이지` 도메인 페이지 아래에서 실제 구현 계약으로 갱신 대상이 되었다.
- `plan`, `transaction`, `global` Notion 페이지는 scope 확정 전에 일부 생성/수정되었고, 후속 판단은 `issues/`에 남겼다.
- `소비 분석 관리`는 기존 양식에 맞게 도메인 페이지 안의 inline 데이터베이스 구조로 재정렬됐다.
- `헬스 체크`는 top-level stray 상태가 아니라 `설정 관리` inline 데이터베이스 아래로 이동됐다.
- 계좌 도메인에 대해서는 "SSAFY 외부 계좌"와 "우리 서비스에 연동된 계좌"를 분리하는 리팩터링 설계가 새로 정리됐다.
- 관련 설계/계획 문서는 아래 두 파일이다.
  - `docs/plans/2026-03-18-account-link-refactor-design.md`
  - `docs/plans/2026-03-18-account-link-refactor.md`
- account-link refactor 구현은 브랜치 `ref/back/account`에 반영되어 있고, 핵심 축은 `origin/dev/backend`의 `56b7be5`로 이미 머지됐다.
  - `POST /v1/accounts/confirm` 추가
  - `POST /v1/accounts/me/sync` 제거
  - `GET /v1/accounts/verification/{accountNo}` 제거
  - `POST /v1/admin/accounts` 제거
  - `GET /v1/accounts/me`가 linked-account reconciliation으로 재정렬
  - `Account`에 `is_active` 상태 추가
  - member/transaction가 active linked account 기준으로 의미를 맞추도록 수정
- account 후속 정리 커밋도 현재 브랜치에 반영돼 있다.
  - `e1ac6ae` `fix: restore admin data init merge wiring`
  - `544904a` `chore: externalize swagger base url`
- 로컬 `docs/api/account.md`, `docs/api/account-verification.md`도 현재 API surface에 맞게 다시 작성됐다.
- `GET /v1/mypage`는 새 `domain/mypage` 조회로 구현됐고, `MypageControllerTest`, `MypageQueryServiceTest`, `SwaggerConfigIntegrationTest` 대상 검증이 성공했다.
- `./gradlew test --tests 'com.ssafy.amagetdone.domain.account..*' --tests 'com.ssafy.amagetdone.domain.accountverification..*' --tests 'com.ssafy.amagetdone.domain.ErdSchemaAlignmentTest'`는 새 브랜치 분기 직전 성공했다.
- Notion `계좌 관리 api` inline 데이터베이스도 실제 구현 기준으로 다시 정리됐다.
  - 삭제 API: `GET /v1/accounts/bank`, `POST /v1/admin/accounts/products`, `GET /v1/admin/accounts/products`
  - 유지 API: 계좌 실소유 확인, 연동 계좌 단건/목록 조회, 1원 인증 요청, 1원 인증 코드 확인
- product 관리 전용 endpoint만 담고 있던 `AccountAdminController`는 현재 브랜치에서 제거됐다.
- 현재 기준으로 account 작업은 "진행 중 로컬 구현"이 아니라 "일단락된 브랜치 상태"로 본다.

## What Is Not Proven By This Summary

- budget 전체 테스트 포함 `./gradlew build --no-daemon`의 성공
- 전체 프로젝트 full regression의 성공
- `56b7be5` 이후 후속 커밋 `e1ac6ae`, `544904a`까지 포함한 회귀 검증의 최신 실행 결과

## Known Caveats

- `docs/plans/2026-03-17-budget-engine-selective-absorption.md`는 `.sisyphus/plans/budget-engine.md`, `.sisyphus/plans/budget-home-aggregate.md`를 참조하지만, 현재 워크트리에서는 해당 경로를 찾지 못했다.
- budget 문서는 "새 작업 계획"이 아니라 "선택 흡수 완료 기록"으로 읽어야 한다.
- 후속 작업 전에는 항상 현재 `git status`와 `git log`를 다시 확인해야 한다. 위 본문은 2026-03-17 기준으로 마지막 크게 정리된 스냅샷이다.
- `docs/api/` 문서 중 일부 error response 예시는 controller test가 아닌 DTO/기존 명세 기반 보강이라, 추후 통합 테스트 기준 재검증 여지가 있다.
- `GET /api/budgets/weeks/{yearWeek}/essential-expenses`는 코드 내 응답 메시지 문자열이 깨져 있어 Notion/로컬 문서 모두 `data` 구조 중심으로 정리했다.
- account/accountverification의 핵심 리팩터링은 더 이상 "현재 워크트리에서 정리 중" 상태가 아니다. 다만 `dev/backend`에는 `56b7be5`까지만 보이므로, 후속 커밋 `e1ac6ae`, `544904a`를 어디까지 기준선으로 볼지는 작업 전 다시 확인해야 한다.
- account/accountverification 구현과 Notion 명세는 현재 시점 기준으로 맞춰졌지만, `docs/kanban/*`는 `.gitignore` 대상이라 로컬 handoff 기록으로만 유지된다.
- mypage 작업은 코드 구현까지 들어간 상태지만, 검증과 Notion 반영이 아직 남아 있을 수 있다. 후속 에이전트는 `GET /v1/mypage` 계약을 기준으로 테스트 결과와 Notion 페이지 내용을 먼저 맞춰야 한다.
- `tmp/`가 현재 untracked 상태로 보이므로, mypage 작업 범위와 무관한 산출물인지 후속 작업 전 한 번 확인하는 편이 안전하다.
- 완료된 실행 plan은 계속 적치하지 않고 정리한다. 현재 test-social-fin-link plan 문서는 제거했고, 완료 이력은 이 문서의 타임라인과 commit reference로 유지한다.
- `origin/dev/backend`에는 이미 `ref/back/account` 병합 커밋 `56b7be5`가 있다. 따라서 새 account 작업을 다시 열 경우에는 "기존 머지 이후 follow-up"인지 "완전히 새 범위"인지부터 구분해야 한다.
- `GET /v1/accounts/bank`와 관리자용 상품 API(`.../products`)는 삭제된 API이므로, 후속 작업자는 이를 유지 범위로 되돌리지 말아야 한다.
- `Account.active`는 soft-invalid 최소 상태로 추가된 로컬 모델이므로, 후속 쿼리/기능은 `verified && active` 의미를 기준으로 읽어야 한다.
- non-target Notion 작업 이슈는 아래 문서에 남겼다.
  - `issues/2026-03-17-non-target-notion-api-pages.md`
- Notion API 명세를 수정할 때는 개별 엔드포인트 페이지를 최상위 `API 명세서` 데이터베이스에 직접 두지 말고, 반드시 도메인 페이지 내부 inline 데이터베이스 아래로 배치해야 한다.
- `issues/` 디렉토리는 해결된 이슈를 누적 보관하는 곳이 아니라 현재 후속 판단이 필요한 이슈만 유지하는 working set으로 관리해야 한다.

## Practical Takeaway For Another Agent

- member 영역을 다룬다면 먼저 `EnsureFinMemberLinkedUseCase` 계약이 `execute(userId, email)`로 바뀌었다는 점을 전제로 시작해야 한다.
- test social login을 만진다면 `test@test.com` 기반 실제 금융회원 연동 흐름을 깨지 말아야 한다.
- budget 영역을 만진다면 `d7e2c6c`가 이미 대규모 흡수를 끝낸 상태라는 점을 기억하고, 중복 흡수나 범위 재분류를 다시 하지 말아야 한다.
- API 문서 작업을 이어받는다면 `mypage`는 새 target 도메인으로 취급하고, `docs/api/mypage.md`와 기존 `마이페이지` Notion 페이지를 우선 기준으로 삼아야 한다.
- Notion에서 layout을 손댈 때는 먼저 도메인 페이지와 inline 데이터베이스가 맞는지부터 확인한다.
- 이슈를 하나 해결했다면 관련 `issues/` 문서와 handoff 참조를 바로 갱신하거나 제거한다.
- account 영역을 다시 건드린다면 먼저 새 설계 문서 2개와 커밋 `56b7be5`, `e1ac6ae`, `544904a`의 범위를 읽고, `confirm -> verification request -> verification check -> linked accounts list` 모델을 현재 기준선으로 삼아야 한다.
- member/transaction 영역을 이어받는다면 `existsLinkedAccountByUserId`, `findAllLinkedAccountsByUserId`, `Account.active`를 기준 용어로 사용해야 한다.
- 로컬 API 문서의 canonical 경로는 `docs/api/`다.
