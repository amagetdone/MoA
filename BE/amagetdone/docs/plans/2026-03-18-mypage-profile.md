# Mypage Profile Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add `GET /v1/mypage` so the app can fetch profile summary and plan counters in one response.

**Architecture:** Add a small read-only `mypage` domain that composes `UserRepository` and `PlanRepository` into a single response. Keep existing `member` update APIs and existing `budget` summary API unchanged.

**Tech Stack:** Spring Boot, Spring MVC, Spring Security, JPA, MockMvc, JUnit 5, Notion MCP

---

### Task 1: Add the failing query-service test

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/mypage/application/service/MypageQueryServiceTest.java`
- Modify: none
- Test: `src/test/java/com/ssafy/amagetdone/domain/mypage/application/service/MypageQueryServiceTest.java`

**Step 1: Write the failing test**

- Assert that user profile fields and plan summary metrics are combined into one response.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests 'com.ssafy.amagetdone.domain.mypage.application.service.MypageQueryServiceTest'`

**Step 3: Write minimal implementation**

- Add DTO, query service, and use case classes needed for the test.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests 'com.ssafy.amagetdone.domain.mypage.application.service.MypageQueryServiceTest'`

### Task 2: Add the failing controller test

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/mypage/api/controller/MypageControllerTest.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/mypage/api/controller/MypageController.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/mypage/api/dto/MypageDto.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/mypage/application/usecase/GetMypageUseCase.java`
- Test: `src/test/java/com/ssafy/amagetdone/domain/mypage/api/controller/MypageControllerTest.java`

**Step 1: Write the failing test**

- Assert `GET /api/v1/mypage` returns `userName`, `userEmail`, `profileImageUrl`, `activePlanCount`, `completedPlanCount`, `totalSavedAmount`.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests 'com.ssafy.amagetdone.domain.mypage.api.controller.MypageControllerTest'`

**Step 3: Write minimal implementation**

- Wire controller to use case and return `ApiResponse.ok(...)`.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests 'com.ssafy.amagetdone.domain.mypage.api.controller.MypageControllerTest'`

### Task 3: Expose Swagger docs

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/global/docs/controller/MypageDocsController.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/mypage/api/controller/MypageController.java`
- Modify: `src/test/java/com/ssafy/amagetdone/global/config/SwaggerConfigIntegrationTest.java`

**Step 1: Add failing Swagger assertion**

- Assert `/v1/mypage` exists with summary text.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests 'com.ssafy.amagetdone.global.config.SwaggerConfigIntegrationTest'`

**Step 3: Implement docs interface**

- Add swagger annotations through `MypageDocsController`.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests 'com.ssafy.amagetdone.global.config.SwaggerConfigIntegrationTest'`

### Task 4: Update local docs and Notion

**Files:**
- Create: `docs/api/mypage.md`
- Modify: `docs/api/README.md`
- Modify: existing Notion page `내 정보 요약 조회`

**Step 1: Write the docs**

- Use the same field order as other local API docs.

**Step 2: Update Notion**

- Reuse the existing `마이페이지` page and inline DB entry.

### Task 5: Update closeout docs and verify

**Files:**
- Modify: `docs/kanban/master-status.md`
- Modify: `docs/kanban/agent-handoff-plan.md`
- Modify if needed: `issues/2026-03-17-non-target-notion-api-pages.md`

**Step 1: Refresh canonical status**

- Reflect the dirty working tree and new `mypage` workstream.

**Step 2: Run focused verification**

Run:

```bash
./gradlew test --tests 'com.ssafy.amagetdone.domain.mypage..*' --tests 'com.ssafy.amagetdone.global.config.SwaggerConfigIntegrationTest'
```

```bash
git status --short --branch
```
