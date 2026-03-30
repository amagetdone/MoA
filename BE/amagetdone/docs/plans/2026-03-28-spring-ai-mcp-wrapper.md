# Spring AI MCP Wrapper Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a Spring AI Streamable HTTP MCP server that exposes read-only budget, asset, transaction, and spending-analysis tools by reusing the current Spring service layer with `userId` inputs.

**Architecture:** Keep MCP isolated under `domain/mcp`, add Spring AI only through BOM-managed WebMVC MCP starter, and route all tool calls through adapter services that delegate to existing usecases/services without changing current REST controllers or auth flow. Default MCP to disabled so existing behavior remains unchanged unless explicitly enabled.

**Tech Stack:** Spring Boot 3.5.11, Spring AI MCP Server WebMVC starter, Java 17, JUnit 5, Mockito, existing Spring MVC/WebFlux stack.

---

### Task 1: Verify dependency compatibility before adding Spring AI

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.properties`
- Test: no test file yet, use Gradle dependency inspection

**Step 1: Write the failing dependency-guard check as a build command**

Run:

```powershell
.\gradlew.bat dependencyInsight --dependency spring-webmvc --configuration runtimeClasspath
```

Expected before changes: no Spring AI artifacts present.

**Step 2: Add Spring AI BOM and MCP starter minimally**

Update `build.gradle` with:

```gradle
ext {
    set('springAiVersion', '1.0.1')
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}"
    }
}

dependencies {
    implementation 'org.springframework.ai:spring-ai-starter-mcp-server-webmvc'
}
```

Add disabled-by-default properties in `src/main/resources/application.properties`:

```properties
mcp.finance.enabled=false
spring.ai.mcp.server.enabled=${mcp.finance.enabled:false}
spring.ai.mcp.server.name=finance-read-mcp
spring.ai.mcp.server.type=SYNC
```

**Step 3: Run dependency inspection to verify no framework drift**

Run:

```powershell
.\gradlew.bat dependencyInsight --dependency spring-webmvc --configuration runtimeClasspath
.\gradlew.bat dependencyInsight --dependency spring-webflux --configuration runtimeClasspath
.\gradlew.bat dependencyInsight --dependency jackson-databind --configuration runtimeClasspath
.\gradlew.bat dependencyInsight --dependency spring-ai-starter-mcp-server-webmvc --configuration runtimeClasspath
```

Expected: Spring Framework stays on Boot-managed 6.2.x line, Spring AI starter resolves cleanly, no duplicate incompatible versions.

**Step 4: Run a narrow baseline test slice**

Run:

```powershell
.\gradlew.bat test --tests "com.ssafy.amagetdone.config.SecurityConfigIntegrationTest"
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.chatbot.infra.client.ChatbotApiClientConfigurationTest"
```

Expected: PASS. This proves existing MVC/security and WebClient-related configuration still boots.

**Step 5: Commit**

```powershell
git add build.gradle src/main/resources/application.properties
git commit -m "build: add spring ai mcp dependencies"
```

### Task 2: Add MCP configuration with default-off isolation

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/mcp/api/config/McpProperties.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/mcp/api/config/McpServerConfig.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/mcp/api/config/McpServerConfigTest.java`

**Step 1: Write the failing config test**

Test sketch:

```java
@SpringBootTest(
    classes = AmagetdoneApplication.class,
    properties = "mcp.finance.enabled=false"
)
class McpServerConfigTest {
    @Autowired ApplicationContext context;

    @Test
    void mcpBeansAreNotRegisteredWhenDisabled() {
        assertThat(context.getBeansOfType(FinanceReadMcpTools.class)).isEmpty();
    }
}
```

**Step 2: Run test to verify it fails for missing classes**

Run:

```powershell
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.mcp.api.config.McpServerConfigTest"
```

Expected: FAIL because MCP config classes do not exist yet.

**Step 3: Add minimal config**

Implement:

- `McpProperties` with `@ConfigurationProperties(prefix = "mcp.finance")`
- `McpServerConfig` with `@EnableConfigurationProperties(McpProperties.class)`
- Gate bean registration with `@ConditionalOnProperty(name = "mcp.finance.enabled", havingValue = "true")`

Minimal snippet:

```java
@ConfigurationProperties(prefix = "mcp.finance")
public record McpProperties(boolean enabled) {}
```

**Step 4: Expand test to enabled path and verify pass**

Add second test/property case to confirm config class loads when enabled.

Run:

```powershell
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.mcp.api.config.McpServerConfigTest"
```

Expected: PASS.

**Step 5: Commit**

```powershell
git add src/main/java/com/ssafy/amagetdone/domain/mcp/api/config src/test/java/com/ssafy/amagetdone/domain/mcp/api/config
git commit -m "feat: add mcp configuration guard"
```

### Task 3: Add budget MCP adapter on top of existing usecases

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/mcp/application/service/BudgetMcpQueryAdapter.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/mcp/application/service/BudgetMcpQueryAdapterTest.java`

**Step 1: Write failing adapter tests**

Test behaviors:

```java
@Test
void getBudgetDashboardDelegatesToExistingUseCase() { ... }

@Test
void getWeeklySavingsDetailDelegatesYearWeekVerbatim() { ... }
```

Mock:

- `GetBudgetIncomeUseCase`
- `GetBudgetDashboardUseCase`
- `GetBudgetHomeUseCase`
- `GetBudgetSummaryUseCase`
- `GetWeeklySavingsDetailUseCase`
- `GetWeeklyFreeSpendingDetailUseCase`
- `GetWeeklyEssentialExpensesUseCase`
- `GetWeeklyEssentialExpenseDetailUseCase`

**Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.mcp.application.service.BudgetMcpQueryAdapterTest"
```

Expected: FAIL because adapter class does not exist.

**Step 3: Add minimal adapter**

Implementation shape:

```java
@Service
@RequiredArgsConstructor
public class BudgetMcpQueryAdapter {
    private final GetBudgetDashboardUseCase getBudgetDashboardUseCase;

    public BudgetDashboardDto.Response getBudgetDashboard(Integer userId) {
        return getBudgetDashboardUseCase.execute(userId);
    }
}
```

Repeat for the remaining budget queries, no extra business logic.

**Step 4: Run tests**

Run:

```powershell
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.mcp.application.service.BudgetMcpQueryAdapterTest"
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.budget.application.usecase.ReadBudgetUseCaseSyncHookTest"
```

Expected: PASS. The second test guards that existing read-sync semantics were not altered.

**Step 5: Commit**

```powershell
git add src/main/java/com/ssafy/amagetdone/domain/mcp/application/service/BudgetMcpQueryAdapter.java src/test/java/com/ssafy/amagetdone/domain/mcp/application/service/BudgetMcpQueryAdapterTest.java
git commit -m "feat: add budget mcp adapter"
```

### Task 4: Add asset MCP adapter with userId-based reads

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/mcp/application/service/AssetMcpQueryAdapter.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/mcp/application/service/AssetMcpQueryAdapterTest.java`

**Step 1: Write failing tests for the three asset queries**

Behaviors:

```java
@Test
void getLinkedAccountsReconcilesAndReturnsBalances() { ... }

@Test
void getAccountDetailValidatesOwnershipAndReadsBalance() { ... }

@Test
void getLinkedCardsUsesExistingRepositoryCondition() { ... }
```

Reuse existing DTOs where possible:

- `AccountDto.AccountSummaryResponse`
- `AccountDto.CardSummaryResponse`

**Step 2: Run test to verify failure**

Run:

```powershell
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.mcp.application.service.AssetMcpQueryAdapterTest"
```

Expected: FAIL because adapter class does not exist.

**Step 3: Implement minimal asset adapter**

Dependencies:

- `ReconcileLinkedAccountsUseCase`
- `ListAccountsUseCase`
- `FindUserAccountUseCase`
- `InquireAccountBalanceUseCase`
- `CardRepository`

Implementation rules:

- `getLinkedAccounts(Integer userId)` should mirror `AccountService.getAccounts`
- `getAccountDetail(Integer userId, String accountNo)` should mirror `AccountService.getAccount`
- `getLinkedCards(Integer userId)` should mirror `AccountService.getCards`

Do not modify `AccountService`.

**Step 4: Run tests and an existing asset service regression**

Run:

```powershell
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.mcp.application.service.AssetMcpQueryAdapterTest"
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.application.service.AccountServiceTest"
```

Expected: PASS.

**Step 5: Commit**

```powershell
git add src/main/java/com/ssafy/amagetdone/domain/mcp/application/service/AssetMcpQueryAdapter.java src/test/java/com/ssafy/amagetdone/domain/mcp/application/service/AssetMcpQueryAdapterTest.java
git commit -m "feat: add asset mcp adapter"
```

### Task 5: Add transaction and spending-analysis MCP adapter

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/mcp/application/service/TransactionMcpQueryAdapter.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/mcp/application/service/TransactionMcpQueryAdapterTest.java`

**Step 1: Write failing adapter tests**

Behaviors:

```java
@Test
void getTransactionListBuildsExistingQueryObject() { ... }

@Test
void getMonthlyExpenseSummaryDelegatesToFacade() { ... }

@Test
void getWeeklySpendingAnalysisDelegatesToFacade() { ... }
```

Dependencies to mock:

- `ListTransactionsUseCase`
- `TransactionAnalysisResponseFacade`

**Step 2: Run failing tests**

Run:

```powershell
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.mcp.application.service.TransactionMcpQueryAdapterTest"
```

Expected: FAIL because adapter class does not exist.

**Step 3: Implement minimal adapter**

Key mapping:

```java
public TransactionListDto.Response getTransactionList(
    Integer userId,
    LocalDate fromDate,
    LocalDate toDate,
    String analysisInclusion,
    Integer page,
    Integer size
) {
    return listTransactionsUseCase.execute(
        userId,
        new TransactionListDto.Query(fromDate, toDate, analysisInclusion, page, size)
    );
}
```

Use facade for:

- monthly expense
- weekly expense graph
- monthly expense graph
- onboarding analysis
- weekly analysis
- monthly analysis

**Step 4: Run tests and one existing transaction regression**

Run:

```powershell
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.mcp.application.service.TransactionMcpQueryAdapterTest"
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionQueryServiceTest"
```

Expected: PASS.

**Step 5: Commit**

```powershell
git add src/main/java/com/ssafy/amagetdone/domain/mcp/application/service/TransactionMcpQueryAdapter.java src/test/java/com/ssafy/amagetdone/domain/mcp/application/service/TransactionMcpQueryAdapterTest.java
git commit -m "feat: add transaction mcp adapter"
```

### Task 6: Expose Spring AI MCP tools and verify disabled/non-impact behavior

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/mcp/application/tool/FinanceReadMcpTools.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/mcp/application/tool/FinanceReadMcpToolsTest.java`
- Modify: `src/test/java/com/ssafy/amagetdone/config/SecurityConfigIntegrationTest.java`

**Step 1: Write failing tool registration tests**

Behaviors:

```java
@SpringBootTest(properties = "mcp.finance.enabled=true")
class FinanceReadMcpToolsTest {
    @Autowired FinanceReadMcpTools tools;

    @Test
    void budgetToolDelegatesToBudgetAdapter() { ... }
}
```

Also add a security/config regression assertion that existing unauthenticated controller behavior is unchanged when MCP is disabled.

**Step 2: Run test to verify failure**

Run:

```powershell
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.mcp.application.tool.FinanceReadMcpToolsTest"
```

Expected: FAIL because tool class does not exist.

**Step 3: Implement tool class**

Tool method shape:

```java
@Tool(name = "get_budget_dashboard", description = "Get current budget dashboard by userId")
public BudgetDashboardDto.Response getBudgetDashboard(int userId) {
    return budgetMcpQueryAdapter.getBudgetDashboard(userId);
}
```

Repeat for all approved read-only tool methods. Keep methods thin and side-effect-free beyond delegated service behavior.

**Step 4: Run focused verification**

Run:

```powershell
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.mcp.application.tool.FinanceReadMcpToolsTest"
.\gradlew.bat test --tests "com.ssafy.amagetdone.config.SecurityConfigIntegrationTest"
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.api.controller.TransactionControllerTest"
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.budget.api.controller.BudgetControllerTest"
```

Expected: PASS.

**Step 5: Commit**

```powershell
git add src/main/java/com/ssafy/amagetdone/domain/mcp/application/tool/FinanceReadMcpTools.java src/test/java/com/ssafy/amagetdone/domain/mcp/application/tool/FinanceReadMcpToolsTest.java src/test/java/com/ssafy/amagetdone/config/SecurityConfigIntegrationTest.java
git commit -m "feat: expose finance read mcp tools"
```

### Task 7: Full verification and integration check

**Files:**
- Modify: none unless verification fails
- Test: existing suite + new MCP tests

**Step 1: Run full targeted verification set**

Run:

```powershell
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.mcp..*"
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.application.service.AccountServiceTest"
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.budget.application.service.BudgetHomeQueryServiceTest"
.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.transaction.application.service.TransactionQueryServiceTest"
.\gradlew.bat test --tests "com.ssafy.amagetdone.config.SecurityConfigIntegrationTest"
```

Expected: PASS.

**Step 2: Run broader suite if the focused slice is clean**

Run:

```powershell
.\gradlew.bat test
```

Expected: PASS. If too slow, capture exact failing module and fix before claiming completion.

**Step 3: Inspect final diff**

Run:

```powershell
git status --short
git diff --stat
```

Expected: only intended MCP-related files changed.

**Step 4: Final commit if verification required follow-up edits**

```powershell
git add .
git commit -m "test: finalize mcp wrapper verification"
```

**Step 5: Prepare handoff**

Summarize:

- dependency compatibility evidence
- default-off safety behavior
- tool list exposed
- tests run with exact commands
