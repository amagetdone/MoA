package com.ssafy.amagetdone.domain.transaction.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeekCategory;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekCategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.member.model.vo.UserStatus;
import com.ssafy.amagetdone.domain.transaction.infra.client.MerchantInferenceApiClient;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.MerchantInferenceDto;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisMetadataRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionExcludedReason;
import com.ssafy.amagetdone.domain.transaction.support.util.FixedExpenseClassifier;
import com.ssafy.amagetdone.domain.transaction.support.util.TransactionExclusionPolicy;
import com.ssafy.amagetdone.domain.transaction.support.util.WeeklyEssentialAllocator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransactionAnalysisServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private AccountTransactionOverrideRepository accountTransactionOverrideRepository;

	@Mock
	private CardTransactionOverrideRepository cardTransactionOverrideRepository;

	@Mock
	private TransactionAnalysisResultRepository transactionAnalysisResultRepository;

	@Mock
	private TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository;

	@Mock
	private EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository;

	@Mock
	private EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository;

	@Mock
	private MerchantInferenceApiClient merchantInferenceApiClient;

	@Test
	void analyzeTransactionsUsesOnlyCompletedMonthsWindow() {
		TransactionAnalysisService service = serviceWithClock("2026-03-17T10:15:30Z");
		User user = user(1, "user-key-1");
		Category defaultCategory = Category.create(4, "Other");

		given(transactionAnalysisMetadataRepository.findTopByUser_IdAndRequestedMonthsOrderByAnalyzedAtDesc(1, 3))
			.willReturn(Optional.empty());
		given(accountTransactionOverrideRepository.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(
			any(),
			any(),
			any()
		)).willReturn(List.of());
		given(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of());
		given(userRepository.getReferenceById(1)).willReturn(user);
		given(accountRepository.findAllByUser_Id(1)).willReturn(List.of());
		given(categoryRepository.findAll()).willReturn(List.of(defaultCategory));
		given(transactionAnalysisResultRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(transactionAnalysisMetadataRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		TransactionAnalysisMetadata metadata = service.analyzeTransactions(1, 3);

		ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(accountTransactionOverrideRepository)
			.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(
				any(),
				fromCaptor.capture(),
				toCaptor.capture()
			);
		assertEquals(LocalDateTime.of(2025, 12, 1, 0, 0), fromCaptor.getValue());
		assertEquals(LocalDateTime.of(2026, 2, 28, 23, 59, 59, 999_999_999), toCaptor.getValue());
		assertEquals(LocalDateTime.of(2025, 12, 1, 0, 0), metadata.getWindowStartAt());
		assertEquals(LocalDateTime.of(2026, 2, 28, 23, 59, 59, 999_999_999), metadata.getWindowEndAt());
		assertEquals(3, metadata.getRequestedMonths());
		assertEquals(0, metadata.getTransactionCount());
		verifyNoInteractions(merchantInferenceApiClient);
	}

	@Test
	void analyzeTransactionsReusesStoredMetadataWhenSignatureIsUnchanged() {
		TransactionAnalysisService service = serviceWithClock("2026-03-17T10:15:30Z");
		User user = user(1, "user-key-1");
		Account account = account(user, "ACC-1", "111-222-333");
		Category category = Category.create(4, "Other");
		AccountTransactionOverride febExpense = accountTransaction(
			101,
			category,
			account,
			1001L,
			LocalDateTime.of(2026, 2, 10, 9, 0),
			55_000L,
			1_000_000L,
			"Phone bill",
			"",
			2
		);
		TransactionAnalysisMetadata existingMetadata = TransactionAnalysisMetadata.builder()
			.id(88)
			.user(user)
			.requestedMonths(3)
			.windowStartAt(LocalDateTime.of(2025, 12, 1, 0, 0))
			.windowEndAt(LocalDateTime.of(2026, 2, 28, 23, 59, 59, 999_999_999))
			.transactionCount(1)
			.latestTransactionAt(LocalDateTime.of(2026, 2, 10, 9, 0))
			.analyzedAt(LocalDateTime.of(2026, 3, 1, 0, 0))
			.build();

		given(transactionAnalysisMetadataRepository.findTopByUser_IdAndRequestedMonthsOrderByAnalyzedAtDesc(1, 3))
			.willReturn(Optional.of(existingMetadata));
		given(accountTransactionOverrideRepository.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(
			any(),
			any(),
			any()
		)).willReturn(List.of(febExpense));
		given(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of());

		TransactionAnalysisMetadata reused = service.analyzeTransactions(1, 3);

		assertSame(existingMetadata, reused);
		verifyNoInteractions(userRepository, accountRepository, categoryRepository, merchantInferenceApiClient);
		verifyNoInteractions(transactionAnalysisResultRepository, essentialExpenseSnapshotWeekRepository,
			essentialExpenseSnapshotWeekCategoryRepository);
	}

	@Test
	void analyzeTransactionsClassifiesTransactionsAndStoresSnapshots() {
		TransactionAnalysisService service = serviceWithClock("2026-04-13T12:00:00Z");
		User user = user(1, "user-key-1");
		Account mainAccount = account(user, "ACC-1", "111-222-333");
		Account internalTargetAccount = account(user, "ACC-2", "999-888-777");
		Card card = card(mainAccount, 9999L, "1234-5678-9012-3456", "123");
		Merchant netflix = Merchant.builder().id(7).name("Netflix").build();
		Merchant emart = Merchant.builder().id(8).name("Emart").build();
		Category defaultCategory = Category.create(4, "기타소비");
		Category subscriptionCategory = Category.create(2, "구독료");
		Category housingCategory = Category.create(12, "주거/통신");
		Category foodCategory = Category.create(10, "식비");
		AccountTransactionOverride incomeTransaction = accountTransaction(
			101,
			defaultCategory,
			mainAccount,
			1001L,
			LocalDateTime.of(2026, 3, 4, 9, 0),
			2_000_000L,
			4_000_000L,
			"Salary",
			"",
			1
		);
		AccountTransactionOverride internalTransferTransaction = accountTransaction(
			102,
			defaultCategory,
			mainAccount,
			1002L,
			LocalDateTime.of(2026, 3, 5, 11, 0),
			300_000L,
			3_700_000L,
			"Transfer",
			"999-888-777",
			2
		);
		AccountTransactionOverride recurringJanuaryTransaction = accountTransaction(
			103,
			defaultCategory,
			mainAccount,
			1003L,
			LocalDateTime.of(2026, 1, 7, 9, 0),
			55_000L,
			3_645_000L,
			"Phone bill",
			"",
			2
		);
		AccountTransactionOverride recurringFebruaryTransaction = accountTransaction(
			104,
			defaultCategory,
			mainAccount,
			1004L,
			LocalDateTime.of(2026, 2, 7, 9, 0),
			55_000L,
			3_590_000L,
			"Phone bill",
			"",
			2
		);
		AccountTransactionOverride recurringMarchTransaction = accountTransaction(
			105,
			defaultCategory,
			mainAccount,
			1005L,
			LocalDateTime.of(2026, 3, 7, 9, 0),
			55_000L,
			3_535_000L,
			"Phone bill",
			"",
			2
		);
		CardTransactionOverride subscriptionTransaction = cardTransaction(
			106,
			defaultCategory,
			netflix,
			card,
			2001L,
			LocalDateTime.of(2026, 3, 6, 8, 0),
			17_000L
		);
		CardTransactionOverride groceryTransaction = cardTransaction(
			107,
			defaultCategory,
			emart,
			card,
			2002L,
			LocalDateTime.of(2026, 3, 5, 18, 0),
			83_000L
		);

		given(transactionAnalysisMetadataRepository.findTopByUser_IdAndRequestedMonthsOrderByAnalyzedAtDesc(1, 3))
			.willReturn(Optional.empty());
		given(userRepository.getReferenceById(1)).willReturn(user);
		given(accountRepository.findAllByUser_Id(1)).willReturn(List.of(mainAccount, internalTargetAccount));
		given(accountTransactionOverrideRepository.findAllByAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(
			any(),
			any(),
			any()
		)).willReturn(
			List.of(
				recurringMarchTransaction,
				internalTransferTransaction,
				incomeTransaction,
				recurringFebruaryTransaction,
				recurringJanuaryTransaction
			)
		);
		given(cardTransactionOverrideRepository
			.findAllByCard_WithdrawalAccount_User_IdAndTransactionDateTimeBetweenOrderByTransactionDateTimeDesc(any(), any(), any()))
			.willReturn(List.of(subscriptionTransaction, groceryTransaction));
		given(merchantInferenceApiClient.labelMerchants(anyList())).willReturn(
			new MerchantInferenceDto.Response(
				List.of(
					inferenceResult("Phone bill", "Phone bill", "주거/통신", "주거/통신", 0.91, false, 0.5),
					inferenceResult("Netflix", "Netflix", "구독료", "구독료", 0.99, false, 0.5),
					inferenceResult("Emart", "Emart", "식비", "식비", 0.88, false, 0.5)
				)
			)
		);
		given(categoryRepository.findAll()).willReturn(List.of(defaultCategory, subscriptionCategory, housingCategory, foodCategory));
		given(transactionAnalysisResultRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(essentialExpenseSnapshotWeekRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(essentialExpenseSnapshotWeekCategoryRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(transactionAnalysisMetadataRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		TransactionAnalysisMetadata metadata = service.analyzeTransactions(1, 3);

		ArgumentCaptor<List<String>> merchantTextsCaptor = listCaptor();
		verify(merchantInferenceApiClient).labelMerchants(merchantTextsCaptor.capture());
		assertEquals(3, merchantTextsCaptor.getValue().size());
		assertTrue(merchantTextsCaptor.getValue().contains("Phone bill"));
		assertTrue(merchantTextsCaptor.getValue().contains("Netflix"));
		assertTrue(merchantTextsCaptor.getValue().contains("Emart"));
		assertFalse(merchantTextsCaptor.getValue().contains("Salary"));
		assertFalse(merchantTextsCaptor.getValue().contains("Transfer"));

		ArgumentCaptor<List<TransactionAnalysisResult>> analysisCaptor = listCaptor();
		verify(transactionAnalysisResultRepository).saveAll(analysisCaptor.capture());
		Map<Integer, TransactionAnalysisResult> resultByTransactionId = analysisCaptor.getValue().stream()
			.collect(Collectors.toMap(result -> result.getTransaction().getId(), Function.identity()));
		assertEquals(TransactionExcludedReason.INCOME, resultByTransactionId.get(101).getExcludedReason());
		assertFalse(resultByTransactionId.get(101).isAnalysisIncluded());
		assertEquals(TransactionExcludedReason.INTERNAL_TRANSFER, resultByTransactionId.get(102).getExcludedReason());
		assertFalse(resultByTransactionId.get(102).isAnalysisIncluded());
		assertEquals(ExpenseScope.FIXED, resultByTransactionId.get(103).getExpenseScope());
		assertEquals(ExpenseScope.FIXED, resultByTransactionId.get(104).getExpenseScope());
		assertEquals(ExpenseScope.FIXED, resultByTransactionId.get(105).getExpenseScope());
		assertEquals(ExpenseScope.FIXED, resultByTransactionId.get(106).getExpenseScope());
		assertEquals(ExpenseScope.VARIABLE, resultByTransactionId.get(107).getExpenseScope());
		assertEquals(subscriptionCategory, resultByTransactionId.get(106).getCategory());
		assertEquals(housingCategory, resultByTransactionId.get(105).getCategory());
		assertEquals(foodCategory, resultByTransactionId.get(107).getCategory());

		ArgumentCaptor<List<EssentialExpenseSnapshotWeek>> weekSnapshotCaptor = listCaptor();
		verify(essentialExpenseSnapshotWeekRepository).saveAll(weekSnapshotCaptor.capture());
		EssentialExpenseSnapshotWeek marchWeek = weekSnapshotCaptor.getValue().stream()
			.filter(snapshot -> snapshot.getWeekStartDate().equals(LocalDate.of(2026, 3, 2)))
			.findFirst()
			.orElseThrow();
		assertEquals(3, marchWeek.getBaseMonths());
		assertEquals(155_000L, marchWeek.getEssentialAmount());
		assertEquals(72_000L, marchWeek.getFixedEssentialAmount());
		assertEquals(83_000L, marchWeek.getVariableEssentialAmount());

		ArgumentCaptor<List<EssentialExpenseSnapshotWeekCategory>> weekCategoryCaptor = listCaptor();
		verify(essentialExpenseSnapshotWeekCategoryRepository).saveAll(weekCategoryCaptor.capture());
		Map<String, Long> marchCategoryAmounts = weekCategoryCaptor.getValue().stream()
			.filter(item -> item.getSnapshotWeek().getWeekStartDate().equals(LocalDate.of(2026, 3, 2)))
			.collect(Collectors.toMap(
				item -> item.getExpenseScope() + ":" + item.getCategory().getName(),
				EssentialExpenseSnapshotWeekCategory::getAmount
			));
		assertEquals(17_000L, marchCategoryAmounts.get("FIXED:구독료"));
		assertEquals(55_000L, marchCategoryAmounts.get("FIXED:주거/통신"));
		assertEquals(83_000L, marchCategoryAmounts.get("VARIABLE:식비"));

		assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), metadata.getWindowStartAt());
		assertEquals(LocalDateTime.of(2026, 3, 31, 23, 59, 59, 999_999_999), metadata.getWindowEndAt());
		assertEquals(7, metadata.getTransactionCount());
		assertEquals(LocalDateTime.of(2026, 3, 7, 9, 0), metadata.getLatestTransactionAt());
	}

	private TransactionAnalysisService serviceWithClock(String instant) {
		TransactionAnalysisService service = new TransactionAnalysisService(
			userRepository,
			accountRepository,
			categoryRepository,
			accountTransactionOverrideRepository,
			cardTransactionOverrideRepository,
			transactionAnalysisResultRepository,
			transactionAnalysisMetadataRepository,
			essentialExpenseSnapshotWeekRepository,
			essentialExpenseSnapshotWeekCategoryRepository,
			merchantInferenceApiClient,
			new TransactionExclusionPolicy(),
			new FixedExpenseClassifier(),
			new WeeklyEssentialAllocator()
		);
		ReflectionTestUtils.setField(
			service,
			"clock",
			Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Seoul"))
		);
		return service;
	}

	private User user(Integer id, String userKey) {
		return User.builder()
			.id(id)
			.status(UserStatus.ACTIVE)
			.userKey(userKey)
			.nickname("tester")
			.email("tester@example.com")
			.build();
	}

	private Account account(User user, String id, String accountNo) {
		Account account = Account.create(user, accountNo, null, null, null, "001", "test-bank", null);
		ReflectionTestUtils.setField(account, "id", id);
		account.markVerified();
		return account;
	}

	private Card card(Account withdrawalAccount, Long cardUniqueNo, String cardNo, String cvc) {
		return Card.builder()
			.cardUniqueNo(cardUniqueNo)
			.withdrawalAccount(withdrawalAccount)
			.cvc(cvc)
			.cardNo(cardNo)
			.name("test-card")
			.issuerCode("ISS")
			.cardIssuerName("issuer")
			.cardExpiryDate(LocalDateTime.of(2027, 12, 31, 0, 0))
			.cardDescription("basic card")
			.withdrawalDate("15")
			.baselinePerformance(0L)
			.maxBenefitLimit(0L)
			.build();
	}

	private AccountTransactionOverride accountTransaction(
		Integer id,
		Category category,
		Account account,
		Long transactionUniqueNo,
		LocalDateTime transactionDateTime,
		Long transactionBalance,
		Long transactionAfterBalance,
		String transactionSummary,
		String transactionMemo,
		Integer transactionType
	) {
		return AccountTransactionOverride.builder()
			.id(id)
			.category(category)
			.transactionDateTime(transactionDateTime)
			.transactionBalance(transactionBalance)
			.transactionMemo(transactionMemo)
			.transactionUniqueNo(transactionUniqueNo)
			.transactionType(transactionType)
			.transactionTypeName(transactionType == 1 ? "IN" : "OUT")
			.transactionAfterBalance(transactionAfterBalance)
			.transactionSummary(transactionSummary)
			.account(account)
			.build();
	}

	private CardTransactionOverride cardTransaction(
		Integer id,
		Category category,
		Merchant merchant,
		Card card,
		Long transactionUniqueNo,
		LocalDateTime transactionDateTime,
		Long transactionBalance
	) {
		return CardTransactionOverride.builder()
			.id(id)
			.category(category)
			.transactionDateTime(transactionDateTime)
			.transactionBalance(transactionBalance)
			.transactionMemo(merchant.getName())
			.transactionUniqueNo(transactionUniqueNo)
			.merchant(merchant)
			.cardStatus(true)
			.billStatementsYn(true)
			.billStatementsStatus("BILLED")
			.card(card)
			.build();
	}

	private MerchantInferenceDto.Result inferenceResult(
		String merchantText,
		String normalizedMerchantText,
		String rawLabel,
		String finalLabel,
		Double confidence,
		Boolean fallbackApplied,
		Double thresholdUsed
	) {
		return new MerchantInferenceDto.Result(
			merchantText,
			normalizedMerchantText,
			rawLabel,
			finalLabel,
			confidence,
			fallbackApplied,
			thresholdUsed
		);
	}

	@SuppressWarnings("unchecked")
	private <T> ArgumentCaptor<List<T>> listCaptor() {
		return (ArgumentCaptor<List<T>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
	}
}
