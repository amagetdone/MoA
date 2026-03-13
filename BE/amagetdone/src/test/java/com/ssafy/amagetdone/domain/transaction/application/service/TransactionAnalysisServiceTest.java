package com.ssafy.amagetdone.domain.transaction.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.asset.model.entity.Account;
import com.ssafy.amagetdone.domain.asset.model.entity.Card;
import com.ssafy.amagetdone.domain.asset.model.entity.Category;
import com.ssafy.amagetdone.domain.asset.model.entity.Merchant;
import com.ssafy.amagetdone.domain.asset.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.asset.model.repository.CategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseSnapshotWeekCategory;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekCategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.member.model.vo.UserStatus;
import com.ssafy.amagetdone.domain.transaction.exception.TransactionException;
import com.ssafy.amagetdone.domain.transaction.infra.client.MerchantInferenceApiClient;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.MerchantInferenceDto;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisJob;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisJobRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionAnalysisJobStatus;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionAnalysisStep;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionExcludedReason;
import com.ssafy.amagetdone.domain.transaction.support.util.FixedExpenseClassifier;
import com.ssafy.amagetdone.domain.transaction.support.util.TransactionExclusionPolicy;
import com.ssafy.amagetdone.domain.transaction.support.util.WeeklyEssentialAllocator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
	private TransactionAnalysisJobRepository transactionAnalysisJobRepository;

	@Mock
	private TransactionAnalysisResultRepository transactionAnalysisResultRepository;

	@Mock
	private EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository;

	@Mock
	private EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository;

	@Mock
	private MerchantInferenceApiClient merchantInferenceApiClient;

	@Mock
	private TransactionSyncService transactionSyncService;

	@Test
	void createJobRejectsDuplicateRunningAnalysis() {
		TransactionAnalysisService service = new TransactionAnalysisService(
			userRepository,
			accountRepository,
			categoryRepository,
			accountTransactionOverrideRepository,
			cardTransactionOverrideRepository,
			transactionAnalysisJobRepository,
			transactionAnalysisResultRepository,
			essentialExpenseSnapshotWeekRepository,
			essentialExpenseSnapshotWeekCategoryRepository,
			merchantInferenceApiClient,
			transactionSyncService,
			new TransactionExclusionPolicy(),
			new FixedExpenseClassifier(),
			new WeeklyEssentialAllocator()
		);

		given(transactionAnalysisJobRepository.existsByUser_IdAndStatus(1, TransactionAnalysisJobStatus.RUNNING)).willReturn(true);

		assertThrows(TransactionException.class, () -> service.createJob(1, 3));
	}

	@Test
	void runAnalysisExcludesIncomeAndInternalTransfersAndBuildsWeeklySnapshots() {
		TransactionAnalysisService service = new TransactionAnalysisService(
			userRepository,
			accountRepository,
			categoryRepository,
			accountTransactionOverrideRepository,
			cardTransactionOverrideRepository,
			transactionAnalysisJobRepository,
			transactionAnalysisResultRepository,
			essentialExpenseSnapshotWeekRepository,
			essentialExpenseSnapshotWeekCategoryRepository,
			merchantInferenceApiClient,
			transactionSyncService,
			new TransactionExclusionPolicy(),
			new FixedExpenseClassifier(),
			new WeeklyEssentialAllocator()
		);
		User user = user(1, "user-key-1");
		Account mainAccount = account(user, "ACC-1", "111-222-333");
		Account internalTargetAccount = account(user, "ACC-2", "999-888-777");
		Card card = card(mainAccount, 9999L, "1234-5678-9012-3456", "123");
		Merchant netflix = Merchant.builder().id(7).name("넷플릭스").build();
		Merchant emart = Merchant.builder().id(8).name("이마트").build();
		Category defaultCategory = Category.create(4, "기타서비스");
		Category subscriptionCategory = Category.create(2, "구독료");
		Category housingCategory = Category.create(12, "주거/통신");
		Category foodCategory = Category.create(10, "식비");
		TransactionAnalysisJob job = TransactionAnalysisJob.builder()
			.id("job-1")
			.user(user)
			.requestedMonths(3)
			.status(TransactionAnalysisJobStatus.PENDING)
			.currentStep(TransactionAnalysisStep.SYNCING)
			.requestedAt(LocalDateTime.of(2026, 3, 13, 12, 0))
			.build();
		AccountTransactionOverride incomeTransaction = accountTransaction(
			101,
			defaultCategory,
			mainAccount,
			1001L,
			LocalDateTime.of(2026, 3, 4, 9, 0),
			2000000L,
			4000000L,
			"급여",
			"",
			1
		);
		AccountTransactionOverride internalTransferTransaction = accountTransaction(
			102,
			defaultCategory,
			mainAccount,
			1002L,
			LocalDateTime.of(2026, 3, 5, 11, 0),
			300000L,
			3700000L,
			"내계좌이체",
			"999-888-777",
			2
		);
		AccountTransactionOverride recurringJanuaryTransaction = accountTransaction(
			103,
			defaultCategory,
			mainAccount,
			1003L,
			LocalDateTime.of(2026, 1, 7, 9, 0),
			55000L,
			3645000L,
			"통신비",
			"",
			2
		);
		AccountTransactionOverride recurringFebruaryTransaction = accountTransaction(
			104,
			defaultCategory,
			mainAccount,
			1004L,
			LocalDateTime.of(2026, 2, 7, 9, 0),
			55000L,
			3590000L,
			"통신비",
			"",
			2
		);
		AccountTransactionOverride recurringMarchTransaction = accountTransaction(
			105,
			defaultCategory,
			mainAccount,
			1005L,
			LocalDateTime.of(2026, 3, 7, 9, 0),
			55000L,
			3535000L,
			"통신비",
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
			17000L
		);
		CardTransactionOverride groceryTransaction = cardTransaction(
			107,
			defaultCategory,
			emart,
			card,
			2002L,
			LocalDateTime.of(2026, 3, 5, 18, 0),
			83000L
		);

		given(transactionAnalysisJobRepository.findById("job-1")).willReturn(Optional.of(job));
		given(transactionAnalysisJobRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(transactionSyncService.syncLatest(1, 3, false)).willReturn(new TransactionSyncService.SyncResult(0, 0));
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
					inferenceResult("통신비", "통신비", "주거/통신", "주거/통신", 0.91, false, 0.5),
					inferenceResult("넷플릭스", "넷플릭스", "구독료", "구독료", 0.99, false, 0.5),
					inferenceResult("이마트", "이마트", "식비", "식비", 0.88, false, 0.5)
				)
			)
		);
		given(categoryRepository.findAll()).willReturn(List.of(defaultCategory, subscriptionCategory, housingCategory, foodCategory));
		given(transactionAnalysisResultRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(essentialExpenseSnapshotWeekRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(essentialExpenseSnapshotWeekCategoryRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		TransactionAnalysisJob completedJob = service.runAnalysis("job-1", false);

		verify(transactionSyncService).syncLatest(1, 3, false);
		ArgumentCaptor<List<String>> merchantTextsCaptor = listCaptor();
		verify(merchantInferenceApiClient).labelMerchants(merchantTextsCaptor.capture());
		assertEquals(3, merchantTextsCaptor.getValue().size());
		assertTrue(merchantTextsCaptor.getValue().contains("통신비"));
		assertTrue(merchantTextsCaptor.getValue().contains("넷플릭스"));
		assertTrue(merchantTextsCaptor.getValue().contains("이마트"));
		assertFalse(merchantTextsCaptor.getValue().contains("급여"));
		assertFalse(merchantTextsCaptor.getValue().contains("내계좌이체"));

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
		assertEquals(155000L, marchWeek.getEssentialAmount());
		assertEquals(72000L, marchWeek.getFixedEssentialAmount());
		assertEquals(83000L, marchWeek.getVariableEssentialAmount());

		ArgumentCaptor<List<EssentialExpenseSnapshotWeekCategory>> weekCategoryCaptor = listCaptor();
		verify(essentialExpenseSnapshotWeekCategoryRepository).saveAll(weekCategoryCaptor.capture());
		Map<String, Long> marchCategoryAmounts = weekCategoryCaptor.getValue().stream()
			.filter(item -> item.getSnapshotWeek().getWeekStartDate().equals(LocalDate.of(2026, 3, 2)))
			.collect(Collectors.toMap(
				item -> item.getExpenseScope() + ":" + item.getCategory().getName(),
				EssentialExpenseSnapshotWeekCategory::getAmount
			));
		assertEquals(17000L, marchCategoryAmounts.get("FIXED:구독료"));
		assertEquals(55000L, marchCategoryAmounts.get("FIXED:주거/통신"));
		assertEquals(83000L, marchCategoryAmounts.get("VARIABLE:식비"));

		assertEquals(TransactionAnalysisJobStatus.SUCCEEDED, completedJob.getStatus());
		assertEquals(TransactionAnalysisStep.COMPLETED, completedJob.getCurrentStep());
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
		return Account.builder()
			.id(id)
			.user(user)
			.accountNo(accountNo)
			.bankCode("001")
			.bankName("테스트은행")
			.verified(true)
			.build();
	}

	private Card card(Account withdrawalAccount, Long cardUniqueNo, String cardNo, String cvc) {
		return Card.builder()
			.cardUniqueNo(cardUniqueNo)
			.withdrawalAccount(withdrawalAccount)
			.cvc(cvc)
			.cardNo(cardNo)
			.name("테스트카드")
			.issuerCode("ISS")
			.cardIssuerName("테스트카드사")
			.cardExpiryDate(LocalDateTime.of(2027, 12, 31, 0, 0))
			.cardDescription("기본 카드")
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
			.transactionTypeName(transactionType == 1 ? "입금" : "출금")
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
