# Health Check Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** `BE/amagetdone` 프로젝트에 `GET /health` 헬스체크 엔드포인트를 추가해 `Hello World!`를 반환한다.

**Architecture:** 웹 계층에 전역 컨트롤러 하나를 추가하고, `MockMvc` 기반 슬라이스 테스트로 엔드포인트 계약을 고정한다. 구현은 테스트를 먼저 작성해 실패를 확인한 뒤 최소 코드만 추가하는 방식으로 진행한다.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring Web, Spring Boot Test, MockMvc

---

### Task 1: Health Check 테스트 추가

**Files:**
- Create: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/global/controller/GlobalHealthCheckControllerTest.java`
- Test: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/global/controller/GlobalHealthCheckControllerTest.java`

**Step 1: Write the failing test**

```java
@WebMvcTest(GlobalHealthCheckController.class)
class GlobalHealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsHelloWorld() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello World!"));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.ssafy.amagetdone.global.controller.GlobalHealthCheckControllerTest`
Expected: FAIL because `GlobalHealthCheckController` does not exist yet

### Task 2: 최소 컨트롤러 구현

**Files:**
- Create: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/global/controller/GlobalHealthCheckController.java`
- Test: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/global/controller/GlobalHealthCheckControllerTest.java`

**Step 3: Write minimal implementation**

```java
@RestController
public class GlobalHealthCheckController {

    @GetMapping("/health")
    public String health() {
        return "Hello World!";
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.ssafy.amagetdone.global.controller.GlobalHealthCheckControllerTest`
Expected: PASS

### Task 3: 회귀 확인

**Files:**
- Modify: `BE/amagetdone/src/main/java/com/ssafy/amagetdone/global/controller/GlobalHealthCheckController.java`
- Test: `BE/amagetdone/src/test/java/com/ssafy/amagetdone/global/controller/GlobalHealthCheckControllerTest.java`

**Step 5: Run broader verification**

Run: `./gradlew test`
Expected: PASS
