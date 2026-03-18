# Mypage Profile Design

## Goal

`내 정보` 탭과 `프로필 수정` 진입에 필요한 사용자 요약 데이터를 한 번에 제공하는 `GET /v1/mypage` API를 추가한다.

## Why This Shape

- 실제 `내 정보` 탭은 사용자 기본 정보와 목표/저축 요약 지표를 한 화면에서 함께 보여준다.
- 현재 백엔드는 `GET /v1/budgets/me/summary`로 목표 수/총 저축액만 제공하고, 사용자 프로필 조회 API는 없다.
- 프런트 번들 기준 `내 정보`와 `프로필 수정`은 `userName`, `userEmail`, `profileImageUrl`, `activeGoalCount`, `achievedGoalCount`, `totalSaved`를 함께 다루는 구조에 가깝다.
- 따라서 `mypage` 도메인 단일 조회로 합치는 편이 프런트 관심사와 문서 구조에 가장 잘 맞는다.

## API Contract

- Method: `GET`
- Path: `/api/v1/mypage`
- Auth: `Authorization: Bearer {accessToken}`
- Response data:
  - `userName: String`
  - `userEmail: String`
  - `profileImageUrl: String | null`
  - `activePlanCount: long`
  - `completedPlanCount: long`
  - `totalSavedAmount: long`

## Server Design

- 새 도메인 패키지 `domain/mypage`를 만든다.
- `MypageController`가 `/v1/mypage`를 노출한다.
- `GetMypageUseCase` -> `MypageQueryService` 흐름으로 읽기 로직을 분리한다.
- `MypageQueryService`는 다음 두 소스를 조합한다.
  - `UserRepository`에서 사용자 기본 정보 조회
  - `PlanRepository`에서 active/completed 목표 수와 총 저축액 집계 조회

## Reuse Rule

- 기존 `BudgetSummaryQueryService`의 집계 규칙과 동일하게 `PlanRepository`를 사용한다.
- 기존 `member` 도메인의 `PATCH /v1/members/me/profile`와 `PATCH /v1/members/me/profile-image`는 그대로 둔다.
- `GET /v1/budgets/me/summary`는 기존 계약을 깨지 않도록 유지한다.

## Testing

- `MypageControllerTest`로 인증된 사용자 기준 응답 계약을 검증한다.
- `MypageQueryServiceTest`로 사용자 정보 + 목표 집계 조합 로직을 검증한다.
- Swagger 노출은 `SwaggerConfigIntegrationTest`에 path 존재 여부를 추가한다.

## Docs

- 로컬 문서에 `docs/api/mypage.md`를 추가한다.
- `docs/api/README.md`의 domain 목록에 `mypage.md`를 추가한다.
- Notion은 기존 `마이페이지` 페이지와 inline DB를 재사용하고, 기존 `내 정보 요약 조회` 페이지 내용을 실제 구현 계약으로 갱신한다.

## Risks

- 현재 워크트리가 account 후속 변경으로 dirty 상태이므로 관련 파일을 건드리지 않는다.
- Notion에는 이미 초안 페이지가 있으므로 새 top-level 페이지를 만들지 않고 기존 구조를 유지해야 한다.
