# Agent Handoff Plan

## Maintenance Rule

- 이 파일은 `docs/kanban/agent-handoff-plan.md` 고정 경로를 계속 업데이트하는 canonical handoff 문서다.
- 새로운 날짜별 handoff 파일을 만들지 말고, 후속 에이전트가 바로 이어받을 수 있게 이 파일을 최신화한다.
- 작업을 완료한 뒤에는 반드시 `docs/kanban/master-status.md`와 함께 이 파일도 갱신한다.

## Purpose

이 문서는 새 에이전트가 현재 브랜치 상태를 10~20분 안에 따라잡고, 이미 끝난 작업을 다시 하지 않도록 돕기 위한 온보딩 플랜이다.

## First 5 Minutes

1. 현재 브랜치와 워크트리부터 확인한다.

```bash
git status --short --branch
git log --oneline --decorate -n 12
```

기대 해석:

- 브랜치는 `ref/back/account`
- upstream은 `origin/ref/back/account`
- 현재 로컬 브랜치는 upstream과 동기화돼 있다
- `origin/dev/backend`는 `56b7be5`에서 이미 `ref/back/account`를 머지한 상태다
- 현재 `ref/back/account`에는 후속 정리 커밋 `e1ac6ae`, `544904a`가 추가로 올라가 있다
- account-link refactor 문맥은 여전히 중요하지만, 현재 HEAD를 "아직 정리되지 않은 account 작업 브랜치"로 가정하면 안 된다
- 필요하면 `feat/back/auth`는 배경 문맥 확인용으로만 참고한다
- account 관련 새 수정 전에는 기존 머지 이후 follow-up인지 완전히 새 범위인지 먼저 확인한다

2. 마스터 현황 문서를 먼저 읽는다.

- `docs/kanban/master-status.md`

이 문서로 작업축과 커밋 맥락을 먼저 잡고, 세부 문서로 내려간다.

## Read Order

### If you need the API docs context

다음 순서로 읽는다.

1. `docs/plans/2026-03-17-api-spec-sync-design.md`
2. `docs/plans/2026-03-17-api-spec-sync.md`
3. `docs/api/README.md`
4. 필요한 도메인별 문서
   - `docs/api/auth.md`
   - `docs/api/member.md`
   - `docs/api/mypage.md`
   - `docs/api/account.md`
   - `docs/api/account-verification.md`
   - `docs/api/budget.md`
   - `docs/api/plan.md`
   - `docs/api/transaction.md`
   - `docs/api/global.md`
5. mypage 작업 설계/계획 문서
   - `docs/plans/2026-03-18-mypage-profile-design.md`
   - `docs/plans/2026-03-18-mypage-profile.md`
6. non-target Notion 관련 후속 이슈
   - `issues/2026-03-17-non-target-notion-api-pages.md`
7. Notion의 `계좌 관리` 도메인 페이지와 inline 데이터베이스
   - 현재 구현된 API만 `완료`
   - 삭제된 `GET /v1/accounts/bank`, `POST /v1/admin/accounts/products`, `GET /v1/admin/accounts/products`는 문서에서 제거했다

### If you need the account-link refactor context

다음 순서로 읽는다.

1. `docs/plans/2026-03-18-account-link-refactor-design.md`
2. `docs/plans/2026-03-18-account-link-refactor.md`
3. 아래 실제 코드
   - `src/main/java/com/ssafy/amagetdone/domain/account/api/controller/AccountController.java`
   - `src/main/java/com/ssafy/amagetdone/domain/account/api/controller/AdminDataInitController.java`
   - `src/main/java/com/ssafy/amagetdone/domain/accountverification/api/controller/AccountVerificationController.java`
   - `src/main/java/com/ssafy/amagetdone/domain/account/application/service/AccountService.java`
   - `src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/ConfirmAccountOwnershipUseCase.java`
   - `src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/ReconcileLinkedAccountsUseCase.java`
   - `src/main/java/com/ssafy/amagetdone/domain/accountverification/application/usecase/RequestAccountVerificationUseCase.java`
   - `src/main/java/com/ssafy/amagetdone/domain/accountverification/application/usecase/CheckAccountVerificationUseCase.java`
   - `src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionSyncService.java`
   - `src/main/java/com/ssafy/amagetdone/domain/transaction/application/service/TransactionAnalysisService.java`

### If you need the member-link context

다음 순서로 읽는다.

1. `docs/plans/2026-03-17-fin-member-relink-design.md`
2. `docs/plans/2026-03-17-fin-member-relink.md`
3. `docs/kanban/master-status.md`의 "테스트 소셜 로그인도 같은 금융회원 연동 경로를 쓰도록 정리됐다" 섹션
4. 아래 실제 코드
   - `src/main/java/com/ssafy/amagetdone/domain/member/application/usecase/EnsureFinMemberLinkedUseCase.java`
   - `src/main/java/com/ssafy/amagetdone/domain/member/application/usecase/CompleteMemberProfileUseCase.java`
   - `src/main/java/com/ssafy/amagetdone/domain/member/application/service/MemberAuthService.java`
   - `src/main/java/com/ssafy/amagetdone/domain/member/model/repository/UserRepository.java`

### If you need the budget context

다음 순서로 읽는다.

1. `docs/plans/2026-03-17-budget-engine-selective-absorption.md`
2. 최근 흡수 커밋 범위 확인

```bash
git show --stat d7e2c6c
git show --stat 19c083e
```

3. 실제 코드
   - `src/main/java/com/ssafy/amagetdone/domain/budget/**`
   - `src/test/java/com/ssafy/amagetdone/domain/budget/**`
   - budget가 건드린 `plan`, `transaction`, `global/docs` 지원 파일

## Do Not Redo These

- `d7e2c6c`의 budget-engine selective absorption 분류 작업
- `2000c1e`의 test social -> fin member 연결 작업
- `EnsureFinMemberLinkedUseCase`의 current-user-aware 계약 전환 자체
- 이미 완료된 `docs/api/` 도메인별 초안 작성
- 이미 추가된 `GET /v1/mypage` 코드/테스트/문서 세트를 다시 새로 설계하는 작업
- scope 확정 후의 기존 non-target Notion 원칙을 다시 뒤집는 작업
- 계좌 외부 미러링 모델을 유지하는 쪽으로 임시 땜질하는 작업
- 제거된 `POST /v1/accounts/me/sync`, `GET /v1/accounts/verification/{accountNo}`, `POST /v1/admin/accounts`를 복구하는 작업
- Notion에 남아 있던 제거 API를 active API처럼 다시 `완료` 상태로 되돌리는 작업

이 셋은 "해야 할 일"이 아니라 "이미 브랜치에 들어온 일"로 취급해야 한다.

## Important Working Assumptions

### Member assumptions

- `EnsureFinMemberLinkedUseCase`는 이제 `execute(userId, email)` 기준이다.
- `createMember -> duplicate면 searchMember` 순서는 유지해야 한다.
- 검색된 `userKey`는 로컬에서 미점유이거나 현재 사용자 소유일 때만 허용한다.
- 다른 로컬 사용자 소유면 `MEMBER_020`로 실패해야 한다.
- 테스트 소셜 로그인은 `test@test.com`으로 실제 금융회원 연동을 수행한다.

### Budget assumptions

- budget 관련 변경은 이미 현재 브랜치에 대규모 반영돼 있다.
- budget 외 파일이 일부 바뀌어도 그것이 예산 기능을 위한 최소 support일 수 있다.
- budget 검증에는 WSL 환경에서 JUnit XML report 우회가 필요할 수 있다.

### API docs assumptions

- 로컬 API 문서의 기준 경로는 `docs/api/`다.
- Full URL은 모두 `https://j14d101.p.ssafy.io/api/...` 기준으로 적는다.
- request/response는 controller, DTO, controller test를 진실원으로 본다.
- Notion API 명세 업데이트 대상은 `account`, `member`, `budget`, `mypage`다.
- `plan`, `transaction`, `global` Notion 관련 후속 판단은 `issues/` 문서를 먼저 보고 결정한다.
- Notion API 명세는 도메인 대표 페이지 안에 inline 데이터베이스가 보이는 기존 양식을 따른다.
- 개별 엔드포인트를 최상위 `API 명세서` 아래에 바로 만들지 않는다.
- `issues/`는 해결된 문서를 쌓아두는 보관함이 아니라 현재 미해결 항목만 남기는 작업 목록으로 본다.
- account/accountverification 노션 명세는 실제 컨트롤러 기준으로 다시 맞췄다.
- `mypage`는 `GET /v1/mypage` 단일 조회를 기준으로 작성하고, `내 정보` 탭과 프로필 수정 진입에서 함께 쓸 수 있는 응답을 우선으로 한다.
- 완료된 `docs/plans` 실행 문서는 계속 누적하지 않는다. test-social-fin-link 건은 문서를 제거했고, canonical history는 `master-status.md`와 commit `2000c1e`로 확인한다.

### Account-link assumptions

- `docs/finapi`의 SSAFY 계정계 API는 고정 외부 인터페이스다.
- 외부 SSAFY 계좌와 우리 DB `account`는 같은 개념이 아니다.
- 우리 DB `account`는 "우리 서비스에 연동 완료된 계좌"만 의미해야 한다.
- 현재 로컬 구현은 `Account.active`를 soft-invalid 최소 상태로 사용한다.
- 연결 전 확인 단계에는 `POST /v1/accounts/confirm` 같은 전용 확인 API를 두는 방향으로 설계가 정리돼 있다.
- 1원 인증 요청 단계에서는 로컬 `account` 선생성을 요구하지 않는다.
- 1원 인증 확인 성공 시점이 로컬 계좌 생성/갱신의 단일 지점이어야 한다.
- member/transaction는 `verified && active` linked account 기준으로 동작해야 한다.
- account 작업은 현재 "진행 중 구현"이 아니라 "일단락된 기준선"으로 보고, reopening 전에는 `56b7be5`, `e1ac6ae`, `544904a`의 차이를 먼저 확인한다.

## Suggested Verification Commands

### API docs-focused

```bash
find docs/api -maxdepth 1 -type f | sort
```

```bash
rg -n "https://j14d101\\.p\\.ssafy\\.io/api" docs/api
```

```bash
sed -n '1,220p' docs/api/account.md
sed -n '1,220p' docs/api/member.md
sed -n '1,260p' docs/api/budget.md
```

### Account-link-focused

```bash
sed -n '1,260p' docs/plans/2026-03-18-account-link-refactor-design.md
sed -n '1,320p' docs/plans/2026-03-18-account-link-refactor.md
```

```bash
rg -n "@PostMapping|@GetMapping" src/main/java/com/ssafy/amagetdone/domain/account src/main/java/com/ssafy/amagetdone/domain/accountverification
```

```bash
./gradlew test --tests 'com.ssafy.amagetdone.domain.account..*' --tests 'com.ssafy.amagetdone.domain.accountverification..*' --tests 'com.ssafy.amagetdone.domain.ErdSchemaAlignmentTest'
```

```bash
./gradlew test --tests 'com.ssafy.amagetdone.domain.member.application.service.MemberAuthServiceTest' --tests 'com.ssafy.amagetdone.domain.member.application.service.MemberTokenServiceTest' --tests 'com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncServiceTest' --tests 'com.ssafy.amagetdone.domain.transaction.application.service.TransactionAnalysisServiceTest'
```

### Member-focused

```bash
./gradlew test --tests 'com.ssafy.amagetdone.domain.member.application.service.MemberAuthServiceTest' --tests 'com.ssafy.amagetdone.domain.member.application.usecase.FindOrCreateSocialUserUseCaseTest' --tests 'com.ssafy.amagetdone.domain.member.application.service.MemberProfileServiceTest' --tests 'com.ssafy.amagetdone.domain.member.application.usecase.EnsureFinMemberLinkedUseCaseTest' --tests 'com.ssafy.amagetdone.domain.member.infra.client.SsafyFinMemberClientTest'
```

### Budget-focused

```bash
./gradlew test --no-daemon --tests 'com.ssafy.amagetdone.domain.budget.application.service.BudgetAllocationServiceTest' --tests 'com.ssafy.amagetdone.domain.budget.application.service.BudgetRolloverServiceTest' --tests 'com.ssafy.amagetdone.domain.budget.application.service.BudgetSpentUpdateServiceTest'
```

```bash
./gradlew test --no-daemon -I /tmp/disable-junit-xml.init.gradle --tests 'com.ssafy.amagetdone.domain.budget.api.controller.BudgetControllerTest'
```

```bash
./gradlew build -x test --no-daemon -I /tmp/disable-junit-xml.init.gradle
```

### Mypage-focused

```bash
./gradlew cleanTest test --no-daemon --rerun-tasks --tests 'com.ssafy.amagetdone.domain.mypage.application.service.MypageQueryServiceTest' --tests 'com.ssafy.amagetdone.domain.mypage.api.controller.MypageControllerTest' --tests 'com.ssafy.amagetdone.global.config.SwaggerConfigIntegrationTest'
```

```bash
sed -n '1,220p' docs/api/mypage.md
```

## Open Questions Before Starting New Work

1. account를 다시 만질 경우, 삭제된 `GET /v1/accounts/bank`, `POST /v1/admin/accounts/products`, `GET /v1/admin/accounts/products`를 되살릴 의도인지 먼저 확인해야 한다.
2. budget 후속 작업이라면 `.sisyphus` 원본 계획이 현재 워크트리에 없다는 점을 감안하고, 현재 브랜치의 실제 코드/커밋을 기준으로 판단해야 한다.
3. API 문서 후속 작업이라면 Notion 수정 대상이 `account`, `member`, `budget`, `mypage`인지 먼저 재확인해야 한다.
4. `GET /v1/accounts/bank`와 관리자용 상품 API는 삭제된 상태이므로, 재도입이 필요하다면 별도 합의가 필요하다.

## Recommended Next Move

- account 작업을 이어받는 경우 먼저 아래 targeted test 묶음을 다시 실행한다.

```bash
./gradlew test --tests 'com.ssafy.amagetdone.domain.account..*' --tests 'com.ssafy.amagetdone.domain.accountverification..*' --tests 'com.ssafy.amagetdone.domain.ErdSchemaAlignmentTest'
```

- account 작업이 현재 일단락된 상태라면, 새 요구사항이 나오기 전까지는 이 브랜치를 추가 구현 브랜치가 아니라 기준선 확인용으로 취급한다.
- account를 다시 열어야 한다면, 삭제된 `/bank`와 `/products` API를 다시 추가하려는지부터 먼저 판단한다.
- 버그를 잡으려는 경우에는 먼저 위 검증 명령으로 현재 상태를 재현하고, 문서상의 "완료"와 실제 HEAD 상태가 아직 일치하는지 확인한다.
- 문서 정리만 목적이라면 이 문서와 `master-status.md`를 기준으로 후속 문서를 이어 쓰면 된다.
- API 문서 후속 작업이라면 `docs/api/mypage.md`와 Notion의 `마이페이지` 페이지를 같은 계약으로 맞추고, `GET /v1/mypage` 테스트 결과까지 함께 확인한다.
- mypage 후속 작업이라면 먼저 위 `Mypage-focused` 검증 명령이 여전히 통과하는지 확인한 뒤 응답 필드를 확장한다.
- Notion 구조를 다시 손댈 일이 있다면 `소비 분석 관리`, `설정 관리`처럼 도메인 페이지 안에 inline 데이터베이스가 유지되는지부터 확인한다.
- 어떤 이슈를 실제로 끝냈다면 `issues/` 문서 자체와 이 문서의 참조도 함께 제거하거나 최신 상태로 바꾼다.
- account 작업을 다시 시작한다면 먼저 현재 기준선의 `confirm -> verification request -> verification check -> linked accounts list` 흐름이 깨지지 않는지 재검증한다.
- 후속 작업 전에는 `master-status.md`와 이 문서의 account-link 섹션이 현재 diff와 맞는지 먼저 확인한다.
