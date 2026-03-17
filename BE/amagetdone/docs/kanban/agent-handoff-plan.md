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

- 브랜치는 `feat/back/auth`
- 원격보다 앞선 내용은 docs 커밋 1개
- 최근 핵심 코드 커밋은 `d7e2c6c`, `2000c1e`

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
   - `docs/api/account.md`
   - `docs/api/account-verification.md`
   - `docs/api/budget.md`
   - `docs/api/plan.md`
   - `docs/api/transaction.md`
   - `docs/api/global.md`
5. non-target Notion 관련 후속 이슈
   - `issues/2026-03-17-non-target-notion-api-pages.md`

### If you need the member-link context

다음 순서로 읽는다.

1. `docs/plans/2026-03-17-fin-member-relink-design.md`
2. `docs/plans/2026-03-17-fin-member-relink.md`
3. `docs/plans/2026-03-17-test-social-fin-link.md`
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
- scope 확정 후의 Notion 원칙: `account`, `member`, `budget` 외 도메인을 Notion에 추가 반영하는 작업

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
- Notion API 명세 업데이트 대상은 `account`, `member`, `budget`만이다.
- `plan`, `transaction`, `global` Notion 관련 후속 판단은 `issues/` 문서를 먼저 보고 결정한다.
- Notion API 명세는 도메인 대표 페이지 안에 inline 데이터베이스가 보이는 기존 양식을 따른다.
- 개별 엔드포인트를 최상위 `API 명세서` 아래에 바로 만들지 않는다.
- `issues/`는 해결된 문서를 쌓아두는 보관함이 아니라 현재 미해결 항목만 남기는 작업 목록으로 본다.

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

## Open Questions Before Starting New Work

1. 다음 작업이 member 흐름인지 budget 후속 작업인지 먼저 명확히 분기해야 한다.
2. budget 후속 작업이라면 `.sisyphus` 원본 계획이 현재 워크트리에 없다는 점을 감안하고, 현재 브랜치의 실제 코드/커밋을 기준으로 판단해야 한다.
3. verification/account 쪽 선행 리팩터링(`a7221d6`, `2347d86`) 위에서 동작한다는 점을 잊지 말아야 한다.
4. API 문서 후속 작업이라면 Notion 수정 대상이 여전히 `account`, `member`, `budget`만인지 먼저 재확인해야 한다.

## Recommended Next Move

- 새 기능을 시작하기 전이라면 member 또는 budget 중 한 축만 선택해서 관련 테스트를 먼저 재실행한다.
- 버그를 잡으려는 경우에는 먼저 위 검증 명령으로 현재 상태를 재현하고, 문서상의 "완료"와 실제 HEAD 상태가 아직 일치하는지 확인한다.
- 문서 정리만 목적이라면 이 문서와 `master-status.md`를 기준으로 후속 문서를 이어 쓰면 된다.
- API 문서 후속 작업이라면 먼저 `docs/api/` 로컬 문서와 Notion의 `account/member/budget` 페이지가 같은 계약인지 비교하고, non-target 도메인은 `issues/` 처리 원칙을 유지한다.
- Notion 구조를 다시 손댈 일이 있다면 `소비 분석 관리`, `설정 관리`처럼 도메인 페이지 안에 inline 데이터베이스가 유지되는지부터 확인한다.
- 어떤 이슈를 실제로 끝냈다면 `issues/` 문서 자체와 이 문서의 참조도 함께 제거하거나 최신 상태로 바꾼다.
