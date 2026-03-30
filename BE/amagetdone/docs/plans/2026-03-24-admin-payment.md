# Admin Payment Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add unified `/admin` payment endpoints for account transfer, card payment, and card payment cancel while immediately syncing the resulting transaction into local override tables.

**Architecture:** Consolidate admin HTTP entrypoints into a single `AdminController`, split orchestration into `AccountAdminService` and `AdminPaymentService`, and implement payment behavior in dedicated use cases. Reuse existing SSAFY admin clients where possible, extend the card admin client for cancel support, and persist each payment/cancel as append-only override rows instead of triggering full transaction sync.

**Tech Stack:** Spring Boot, JPA, RestClient, JUnit 5, Mockito, Gradle

---

### Task 1: Unify Admin Controller Surface

**Files:**
- Create: `src/test/java/com/ssafy/amagetdone/domain/account/api/controller/AdminControllerTest.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/api/controller/AccountAdminController.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/api/controller/AdminDataInitController.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/api/dto/AccountAdminDto.java`
- Delete or replace via move: `src/test/java/com/ssafy/amagetdone/domain/account/api/controller/AccountAdminControllerTest.java`
- Delete or replace via move: `src/test/java/com/ssafy/amagetdone/domain/account/api/controller/AdminDataInitControllerTest.java`

**Step 1: Write the failing controller tests**

```java
@Test
void executeAccountPaymentReturnsWrappedResponse() {
	AdminPaymentService adminPaymentService = mock(AdminPaymentService.class);
	AccountAdminService accountAdminService = mock(AccountAdminService.class);
	AdminController controller = new AdminController(accountAdminService, adminPaymentService);
	AccountAdminDto.AccountPaymentRequest request =
		new AccountAdminDto.AccountPaymentRequest("0011234567890123", "0010000000000001", 10000L, "테스트 출금", "테스트 입금");
	AccountAdminDto.PaymentResponse response =
		new AccountAdminDto.PaymentResponse("account", "0011234567890123", 77L, "20260324", "092500", 10000L, "COMPLETED");

	when(adminPaymentService.executeAccountPayment(7, request)).thenReturn(response);

	ApiResponse<AccountAdminDto.PaymentResponse> result = controller.executeAccountPayment(new AuthenticatedMember(7), request);

	assertThat(result.status()).isEqualTo("ok");
	assertThat(result.data()).isEqualTo(response);
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.api.controller.AdminControllerTest"`

Expected: FAIL because `AdminController`, new DTOs, or payment methods do not exist yet.

**Step 3: Write minimal controller and DTO changes**

```java
@RestController
@RequestMapping("/admin")
class AdminController {

	private final AccountAdminService accountAdminService;
	private final AdminPaymentService adminPaymentService;

	@PostMapping("/payments/account")
	ApiResponse<AccountAdminDto.PaymentResponse> executeAccountPayment(...) { ... }
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.api.controller.AdminControllerTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/api/controller src/main/java/com/ssafy/amagetdone/domain/account/api/dto src/test/java/com/ssafy/amagetdone/domain/account/api/controller
git commit -m "refactor: unify admin controller endpoints"
```

### Task 2: Split Admin Services Without Behavior Changes

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/application/service/AccountAdminService.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/application/service/AdminPaymentService.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/account/application/service/AccountAdminServiceTest.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/account/application/service/AdminPaymentServiceTest.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/application/service/AccountService.java`
- Modify: `src/test/java/com/ssafy/amagetdone/domain/account/application/service/AccountServiceTest.java`

**Step 1: Write failing service tests**

```java
@Test
void registerProductDelegatesToUseCase() {
	when(registerDemandDepositProductUseCase.execute(request)).thenReturn(response);

	assertThat(accountAdminService.registerProduct(request)).isEqualTo(response);
}

@Test
void executeCardPaymentDelegatesToUseCase() {
	when(executeAdminCardPaymentUseCase.execute(7, request)).thenReturn(response);

	assertThat(adminPaymentService.executeCardPayment(7, request)).isEqualTo(response);
}
```

**Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.application.service.AccountAdminServiceTest" --tests "com.ssafy.amagetdone.domain.account.application.service.AdminPaymentServiceTest"`

Expected: FAIL because split services do not exist.

**Step 3: Write minimal service split**

```java
@Service
@RequiredArgsConstructor
public class AdminPaymentService {
	private final ExecuteAdminAccountPaymentUseCase executeAdminAccountPaymentUseCase;
	...
}
```

**Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.application.service.AccountAdminServiceTest" --tests "com.ssafy.amagetdone.domain.account.application.service.AdminPaymentServiceTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/application/service src/test/java/com/ssafy/amagetdone/domain/account/application/service
git commit -m "refactor: split admin account and payment services"
```

### Task 3: Add Account Payment Use Case With Immediate Override Persistence

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/ExecuteAdminAccountPaymentUseCase.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/ExecuteAdminAccountPaymentUseCaseTest.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/model/repository/AccountRepository.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/exception/AccountErrorCode.java`

**Step 1: Write the failing use case test**

```java
@Test
void executePersistsWithdrawalOverrideAfterSsafyTransfer() {
	when(accountRepository.findActiveLinkedAccountByUserIdAndAccountNo(7, "0011234567890123"))
		.thenReturn(Optional.of(account));
	when(ssafyAdminDemandDepositApiClient.createTransferTransaction(...)).thenReturn(transactionItem);

	AccountAdminDto.PaymentResponse result = useCase.execute(7, request);

	verify(accountTransactionOverrideRepository).save(any(AccountTransactionOverride.class));
	assertThat(result.targetType()).isEqualTo("account");
	assertThat(result.transactionUniqueNo()).isEqualTo(91L);
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.application.usecase.ExecuteAdminAccountPaymentUseCaseTest"`

Expected: FAIL because the use case and request/response mapping do not exist.

**Step 3: Write minimal implementation**

```java
@Transactional
public AccountAdminDto.PaymentResponse execute(Integer userId, AccountAdminDto.AccountPaymentRequest request) {
	Account account = accountRepository.findActiveLinkedAccountByUserIdAndAccountNo(userId, request.accountNo())
		.orElseThrow(() -> new AccountException(AccountErrorCode.ACCOUNT_NOT_FOUND));
	DemandDepositTransactionDto.Item item = ssafyAdminDemandDepositApiClient.createTransferTransaction(...);
	accountTransactionOverrideRepository.save(toOverride(account, item));
	return AccountAdminDto.PaymentResponse.fromAccount(item, account.getAccountNo());
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.application.usecase.ExecuteAdminAccountPaymentUseCaseTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/ExecuteAdminAccountPaymentUseCase.java src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/ExecuteAdminAccountPaymentUseCaseTest.java src/main/java/com/ssafy/amagetdone/domain/account/model/repository/AccountRepository.java
git commit -m "feat: add admin account payment use case"
```

### Task 4: Add Card Payment Use Case With Immediate Override Persistence

**Files:**
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/ExecuteAdminCardPaymentUseCase.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/ExecuteAdminCardPaymentUseCaseTest.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/model/repository/CardRepository.java`

**Step 1: Write the failing use case test**

```java
@Test
void executePersistsCardOverrideAfterPayment() {
	when(cardRepository.findByCardNo("5241111122223333")).thenReturn(Optional.of(card));
	when(ssafyAdminCreditCardApiClient.createCardTransaction(...)).thenReturn(cardItem);

	AccountAdminDto.PaymentResponse result = useCase.execute(7, request);

	verify(cardTransactionOverrideRepository).save(any(CardTransactionOverride.class));
	assertThat(result.targetType()).isEqualTo("card");
	assertThat(result.status()).isEqualTo("BILLED");
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.application.usecase.ExecuteAdminCardPaymentUseCaseTest"`

Expected: FAIL because the use case does not exist.

**Step 3: Write minimal implementation**

```java
@Transactional
public AccountAdminDto.PaymentResponse execute(Integer userId, AccountAdminDto.CardPaymentRequest request) {
	Card card = findOwnedCard(userId, request.cardNo());
	CardTransactionDto.Item item = ssafyAdminCreditCardApiClient.createCardTransaction(
		adminApiKey,
		card.getWithdrawalAccount().getUser().getUserKey(),
		card.getCardNo(),
		card.getCvc(),
		request.merchantId(),
		request.amount()
	);
	cardTransactionOverrideRepository.save(toCardOverride(card, item));
	return AccountAdminDto.PaymentResponse.fromCard(item, card.getCardNo());
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.application.usecase.ExecuteAdminCardPaymentUseCaseTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/ExecuteAdminCardPaymentUseCase.java src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/ExecuteAdminCardPaymentUseCaseTest.java src/main/java/com/ssafy/amagetdone/domain/account/model/repository/CardRepository.java
git commit -m "feat: add admin card payment use case"
```

### Task 5: Extend SSAFY Card Admin Client And Implement Cancel Use Case

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyAdminCreditCardApiClient.java`
- Create: `src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/CancelAdminCardPaymentUseCase.java`
- Create: `src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/CancelAdminCardPaymentUseCaseTest.java`
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/exception/AccountErrorCode.java`

**Step 1: Write the failing cancel use case test**

```java
@Test
void cancelAppendsCancelOverrideInsteadOfUpdatingExistingRow() {
	when(cardRepository.findByCardNo("5241111122223333")).thenReturn(Optional.of(card));
	when(ssafyAdminCreditCardApiClient.cancelCardTransaction(...)).thenReturn(cancelItem);

	AccountAdminDto.PaymentResponse result = useCase.execute(7, request);

	verify(cardTransactionOverrideRepository).save(any(CardTransactionOverride.class));
	verify(cardTransactionOverrideRepository, never()).save(existingApprovalRow);
	assertThat(result.status()).isEqualTo("CANCEL");
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.application.usecase.CancelAdminCardPaymentUseCaseTest"`

Expected: FAIL because cancel client method and use case do not exist.

**Step 3: Write minimal implementation**

```java
public CardTransactionDto.Item cancelCardTransaction(String apiKey, String userKey, String cardNo, String cvc, Long transactionUniqueNo) {
	ApiCancelCardTransactionResponse payload = post(...);
	return new CardTransactionDto.Item(..., "Y", "Y", "CANCEL");
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.application.usecase.CancelAdminCardPaymentUseCaseTest"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/infra/client/SsafyAdminCreditCardApiClient.java src/main/java/com/ssafy/amagetdone/domain/account/application/usecase/CancelAdminCardPaymentUseCase.java src/test/java/com/ssafy/amagetdone/domain/account/application/usecase/CancelAdminCardPaymentUseCaseTest.java src/main/java/com/ssafy/amagetdone/domain/account/exception/AccountErrorCode.java
git commit -m "feat: add admin card payment cancel flow"
```

### Task 6: Remove Obsolete Wiring And Verify The Whole Admin Payment Surface

**Files:**
- Modify: `src/main/java/com/ssafy/amagetdone/domain/account/application/service/AccountService.java`
- Modify: `src/test/java/com/ssafy/amagetdone/domain/account/application/service/AccountServiceTest.java`
- Modify: `src/test/java/com/ssafy/amagetdone/domain/account/api/controller/AdminControllerTest.java`

**Step 1: Write the final failing regression tests**

```java
@Test
void legacyAccountServiceNoLongerOwnsAdminPaymentBehavior() {
	assertThat(AccountService.class.getDeclaredMethods())
		.extracting(Method::getName)
		doesNotContain("initializeCardTransactions", "initializeAccountTransactions");
}
```

**Step 2: Run focused regression suite**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.api.controller.AdminControllerTest" --tests "com.ssafy.amagetdone.domain.account.application.service.AccountAdminServiceTest" --tests "com.ssafy.amagetdone.domain.account.application.service.AdminPaymentServiceTest" --tests "com.ssafy.amagetdone.domain.account.application.usecase.ExecuteAdminAccountPaymentUseCaseTest" --tests "com.ssafy.amagetdone.domain.account.application.usecase.ExecuteAdminCardPaymentUseCaseTest" --tests "com.ssafy.amagetdone.domain.account.application.usecase.CancelAdminCardPaymentUseCaseTest"`

Expected: FAIL until obsolete wiring is removed and all new paths are connected.

**Step 3: Clean up wiring and keep only active responsibilities**

```java
// Keep AccountService focused on end-user account features.
// Admin product/init/payment entrypoints move to dedicated admin services.
```

**Step 4: Run full relevant verification**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.api.controller.AdminControllerTest" --tests "com.ssafy.amagetdone.domain.account.application.service.*" --tests "com.ssafy.amagetdone.domain.account.application.usecase.*"`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/ssafy/amagetdone/domain/account/application/service/AccountService.java src/test/java/com/ssafy/amagetdone/domain/account/application/service/AccountServiceTest.java src/test/java/com/ssafy/amagetdone/domain/account/api/controller/AdminControllerTest.java
git commit -m "refactor: finish admin payment wiring"
```

### Task 7: Final Verification And Documentation Capture

**Files:**
- Create or update: `docs/plans/2026-03-24-admin-payment-design.md`
- Create or update: `docs/plans/2026-03-24-admin-payment.md`

**Step 1: Run final verification**

Run: `.\gradlew.bat test --tests "com.ssafy.amagetdone.domain.account.api.controller.*" --tests "com.ssafy.amagetdone.domain.account.application.service.*" --tests "com.ssafy.amagetdone.domain.account.application.usecase.*"`

Expected: PASS with all new admin payment tests green.

**Step 2: Review final diff**

Run: `git diff --stat HEAD~1..HEAD`

Expected: Admin controller, DTO, service, use case, SSAFY client, and tests only.

**Step 3: Force-add ignored docs if they should be preserved in git**

```bash
git add -f docs/plans/2026-03-24-admin-payment-design.md docs/plans/2026-03-24-admin-payment.md
git commit -m "docs: add admin payment design and implementation plan"
```

**Step 4: Prepare Notion issue summary**

Document:
- 문제 상황
- 원인
- 다양한 접근방법
- 선택한 해결방법
- 결과

**Step 5: Stop and hand off for execution**

No implementation beyond this point in the planning session.
