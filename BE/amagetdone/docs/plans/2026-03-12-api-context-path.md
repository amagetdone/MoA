# API Context Path Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Move `/api` to the servlet context path, remove duplicated `/api` prefixes from controllers, and keep the external health contract at `/api/health`.

**Architecture:** Configure Spring Boot with a global servlet context path of `/api`, then refactor controller mappings to remove their hardcoded `/api` prefixes. Keep test requests and deploy health checks targeting the external contract so callers remain unchanged while application code becomes prefix-agnostic.

**Tech Stack:** Spring Boot 3, MockMvc, Gradle, Jenkins, Docker Compose

---

### Task 1: Lock the external route contract with failing tests

**Files:**
- Modify: `src/test/java/com/ssafy/amagetdone/global/controller/GlobalHealthCheckControllerTest.java`
- Read: `src/test/java/com/ssafy/amagetdone/domain/member/api/controller/AuthControllerTest.java`
- Read: `src/test/java/com/ssafy/amagetdone/domain/member/api/controller/MemberProfileControllerTest.java`

**Step 1: Remove the legacy `/health` compatibility test**

Keep only the canonical app health request `GET /api/health`.

**Step 2: Confirm controller tests still exercise external `/api/...` paths**

Auth and member profile tests should continue to call `/api/v1/...` because that is the public contract.

**Step 3: Run the focused health controller test before implementation**

Run:

```bash
./gradlew test --tests com.ssafy.amagetdone.global.controller.GlobalHealthCheckControllerTest
```

Expected: fail after the test is tightened and before the controller/context-path refactor is complete.

### Task 2: Move `/api` into Spring configuration

**Files:**
- Modify: `src/main/resources/application.properties`

**Step 1: Add the servlet context path**

Set:

```properties
server.servlet.context-path=/api
```

**Step 2: Keep all other server settings unchanged**

Only centralize the `/api` prefix. Do not add rewrite logic in application code.

### Task 3: Strip `/api` from controller mappings

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/member/api/controller/AuthController.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/member/api/controller/MemberProfileController.java`
- Modify: `src/main/java/com/ssafy/amagetdone/global/controller/GlobalHealthCheckController.java`

**Step 1: Refactor member API controllers**

Change:
- `@RequestMapping("/api/v1/auth")` -> `@RequestMapping("/v1/auth")`
- `@RequestMapping("/api/v1/members")` -> `@RequestMapping("/v1/members")`

**Step 2: Refactor health controller**

Expose only:

```java
@GetMapping("/health")
```

Because the global servlet context path makes the external route `/api/health`.

**Step 3: Do not keep the old root-level `/health` alias**

With a global servlet context path, the canonical public route is `/api/health` only.

### Task 4: Align deployment and example configuration

**Files:**
- Modify: `.env.jenkins.example`
- Verify: `/mnt/c/ssafy/특화/S14P21D101/FE/deploy/web/deploy.sh`

**Step 1: Keep backend-local and external health checks on `/api/health`**

The Jenkins example health URLs should remain:
- `http://127.0.0.1:8080/api/health`
- `http://127.0.0.1:8082/api/health`
- `https://.../api/health`

**Step 2: Confirm frontend deploy script still checks `/healthz` and `/api/health`**

No FE code change is needed if the script already uses those paths.

### Task 5: Verify the refactor

**Files:**
- Verify runtime target: local backend container

**Step 1: Run focused controller tests**

Run:

```bash
./gradlew test --tests com.ssafy.amagetdone.global.controller.GlobalHealthCheckControllerTest
./gradlew test --tests com.ssafy.amagetdone.domain.member.api.controller.AuthControllerTest
./gradlew test --tests com.ssafy.amagetdone.domain.member.api.controller.MemberProfileControllerTest
```

Expected: pass.

**Step 2: Run the full backend test suite**

Run:

```bash
./gradlew test
```

Expected: pass.

**Step 3: Rebuild and start the backend container**

Run:

```bash
docker compose up -d --build backend
```

Expected: backend restarts successfully.

**Step 4: Verify runtime route behavior**

Run:

```bash
curl -i http://127.0.0.1:8080/api/health
curl -i http://127.0.0.1:8080/health
```

Expected:
- `/api/health` returns `200`
- `/health` does not return the app health payload

### Task 6: Commit and push

**Files:**
- Commit modified source and config files

**Step 1: Review git diff**

Run:

```bash
git diff -- src/main/resources/application.properties src/main/java src/test/java .env.jenkins.example
```

**Step 2: Commit**

Run:

```bash
git add src/main/resources/application.properties src/main/java src/test/java .env.jenkins.example
git commit -m "refactor: move api prefix to servlet context path"
```

**Step 3: Push**

Run:

```bash
git push origin HEAD
```
