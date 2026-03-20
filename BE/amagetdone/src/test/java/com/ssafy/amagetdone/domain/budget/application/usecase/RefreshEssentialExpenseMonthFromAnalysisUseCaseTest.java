package com.ssafy.amagetdone.domain.budget.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.application.service.EssentialExpenseMonthRefreshService;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetMonthStatus;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.budget.model.vo.SourceProvider;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisMetadataRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefreshEssentialExpenseMonthFromAnalysisUseCaseTest {

	@Mock
	private TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository;

	@Mock
	private TransactionAnalysisResultRepository transactionAnalysisResultRepository;

	@Mock
	private BudgetMonthRepository budgetMonthRepository;

	@Mock
	private EssentialExpenseMonthRepository essentialExpenseMonthRepository;

	@Mock
	private EssentialWeekAllocationRepository essentialWeekAllocationRepository;

	private RefreshEssentialExpenseMonthFromAnalysisUseCase useCase;

	@BeforeEach
	void setUp() {
		EssentialExpenseMonthRefreshService refreshService = new EssentialExpenseMonthRefreshService(
			transactionAnalysisMetadataRepository,
			transactionAnalysisResultRepository,
			budgetMonthRepository,
			essentialExpenseMonthRepository,
			essentialWeekAllocationRepository
		);
		useCase = new RefreshEssentialExpenseMonthFromAnalysisUseCase(refreshService);
	}

	@Test
	void refreshesAnalyzerRowsFromLatestAnalysisAndExcludesOnlyMonthsWithoutAnyTransactionsFromDivisor() {
		Integer userId = 1;
		LocalDate baseMonth = LocalDate.of(2026, 3, 1);
		LocalDateTime analyzedAt = LocalDateTime.of(2026, 3, 10, 12, 0);
		Category housing = Category.create(10, "주거비");
		Category food = Category.create(20, "식비");
		BudgetMonth marchBudgetMonth = budgetMonth(100, userId, baseMonth);

		given(transactionAnalysisMetadataRepository.findTopByUser_IdOrderByAnalyzedAtDesc(userId))
			.willReturn(Optional.of(metadata(userId, analyzedAt, LocalDateTime.of(2025, 12, 1, 0, 0), LocalDateTime.of(2026, 2, 28, 23, 59))));
		given(transactionAnalysisResultRepository.findAllByUserIdAndAnalyzedAtAndTransactionDateTimeBetween(
			userId,
			analyzedAt,
			LocalDateTime.of(2025, 12, 1, 0, 0),
			LocalDateTime.of(2026, 2, 28, 23, 59)
		)).willReturn(List.of(
			analysisResult(1, housing, ExpenseScope.FIXED, 300_000L, LocalDateTime.of(2025, 12, 5, 9, 0), true),
			analysisResult(2, housing, ExpenseScope.FIXED, 300_000L, LocalDateTime.of(2026, 2, 5, 9, 0), true),
			analysisResult(3, food, ExpenseScope.VARIABLE, 40_000L, LocalDateTime.of(2025, 12, 10, 9, 0), true),
			analysisResult(4, null, null, 11_000L, LocalDateTime.of(2026, 2, 15, 9, 0), false)
		));
		given(essentialExpenseMonthRepository.findAllByBudgetMonth_User_IdAndBudgetMonth_BaseDate(userId, baseMonth))
			.willReturn(List.of());
		given(budgetMonthRepository.findByUser_IdAndBaseDate(userId, baseMonth))
			.willReturn(Optional.of(marchBudgetMonth));
		given(essentialExpenseMonthRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		useCase.execute(userId, baseMonth);

		ArgumentCaptor<List<EssentialExpenseMonth>> saveCaptor = ArgumentCaptor.forClass(List.class);
		verify(essentialExpenseMonthRepository).saveAll(saveCaptor.capture());
		List<EssentialExpenseMonth> savedRows = saveCaptor.getValue();

		assertEquals(2, savedRows.size());
		EssentialExpenseMonth savedHousing = findByKey(savedRows, 10, ExpenseScope.FIXED);
		EssentialExpenseMonth savedFood = findByKey(savedRows, 20, ExpenseScope.VARIABLE);
		assertEquals(300_000L, savedHousing.getAmount());
		assertEquals(20_000L, savedFood.getAmount());
		verify(essentialExpenseMonthRepository, never()).deleteAll(any());
	}

	@Test
	void roundsAverageAmountToNearestHundredWon() {
		Integer userId = 1;
		LocalDate baseMonth = LocalDate.of(2026, 3, 1);
		LocalDateTime analyzedAt = LocalDateTime.of(2026, 3, 10, 12, 0);
		Category food = Category.create(20, "식비");

		given(transactionAnalysisMetadataRepository.findTopByUser_IdOrderByAnalyzedAtDesc(userId))
			.willReturn(Optional.of(metadata(userId, analyzedAt, LocalDateTime.of(2025, 12, 1, 0, 0), LocalDateTime.of(2026, 2, 28, 23, 59))));
		given(transactionAnalysisResultRepository.findAllByUserIdAndAnalyzedAtAndTransactionDateTimeBetween(
			userId,
			analyzedAt,
			LocalDateTime.of(2025, 12, 1, 0, 0),
			LocalDateTime.of(2026, 2, 28, 23, 59)
		)).willReturn(List.of(
			analysisResult(1, food, ExpenseScope.VARIABLE, 10_150L, LocalDateTime.of(2025, 12, 10, 9, 0), true),
			analysisResult(2, null, null, 8_000L, LocalDateTime.of(2026, 2, 12, 9, 0), false)
		));
		given(essentialExpenseMonthRepository.findAllByBudgetMonth_User_IdAndBudgetMonth_BaseDate(userId, baseMonth))
			.willReturn(List.of());
		given(budgetMonthRepository.findByUser_IdAndBaseDate(userId, baseMonth))
			.willReturn(Optional.of(budgetMonth(100, userId, baseMonth)));
		given(essentialExpenseMonthRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		useCase.execute(userId, baseMonth);

		ArgumentCaptor<List<EssentialExpenseMonth>> saveCaptor = ArgumentCaptor.forClass(List.class);
		verify(essentialExpenseMonthRepository).saveAll(saveCaptor.capture());
		assertEquals(5_100L, saveCaptor.getValue().get(0).getAmount());
	}

	@Test
	void preservesUserInputRowsAndSkipsAnalyzerWriteForConflictingKey() {
		Integer userId = 1;
		LocalDate baseMonth = LocalDate.of(2026, 3, 1);
		LocalDateTime analyzedAt = LocalDateTime.of(2026, 3, 10, 12, 0);
		Category housing = Category.create(10, "주거비");
		Category food = Category.create(20, "식비");
		BudgetMonth marchBudgetMonth = budgetMonth(100, userId, baseMonth);
		EssentialExpenseMonth userInputHousing = essentialExpenseMonth(
			900,
			marchBudgetMonth,
			housing,
			ExpenseScope.FIXED,
			450_000L,
			SourceProvider.USER_INPUT
		);
		EssentialExpenseMonth analyzerHousing = essentialExpenseMonth(
			901,
			marchBudgetMonth,
			housing,
			ExpenseScope.FIXED,
			300_000L,
			SourceProvider.ANALYZER
		);

		given(transactionAnalysisMetadataRepository.findTopByUser_IdOrderByAnalyzedAtDesc(userId))
			.willReturn(Optional.of(metadata(userId, analyzedAt, LocalDateTime.of(2025, 12, 1, 0, 0), LocalDateTime.of(2026, 2, 28, 23, 59))));
		given(transactionAnalysisResultRepository.findAllByUserIdAndAnalyzedAtAndTransactionDateTimeBetween(
			userId,
			analyzedAt,
			LocalDateTime.of(2025, 12, 1, 0, 0),
			LocalDateTime.of(2026, 2, 28, 23, 59)
		)).willReturn(List.of(
			analysisResult(1, housing, ExpenseScope.FIXED, 300_000L, LocalDateTime.of(2025, 12, 5, 9, 0), true),
			analysisResult(2, housing, ExpenseScope.FIXED, 300_000L, LocalDateTime.of(2026, 2, 5, 9, 0), true),
			analysisResult(3, food, ExpenseScope.VARIABLE, 40_000L, LocalDateTime.of(2026, 2, 10, 9, 0), true)
		));
		given(essentialExpenseMonthRepository.findAllByBudgetMonth_User_IdAndBudgetMonth_BaseDate(userId, baseMonth))
			.willReturn(List.of(userInputHousing, analyzerHousing));
		given(budgetMonthRepository.findByUser_IdAndBaseDate(userId, baseMonth))
			.willReturn(Optional.of(marchBudgetMonth));
		given(essentialExpenseMonthRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		useCase.execute(userId, baseMonth);

		ArgumentCaptor<List<EssentialExpenseMonth>> saveCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<List<EssentialExpenseMonth>> deleteCaptor = ArgumentCaptor.forClass(List.class);
		verify(essentialExpenseMonthRepository).saveAll(saveCaptor.capture());
		verify(essentialExpenseMonthRepository).deleteAll(deleteCaptor.capture());
		assertEquals(1, saveCaptor.getValue().size());
		assertEquals(food.getId(), saveCaptor.getValue().get(0).getCategory().getId());
		assertEquals(1, deleteCaptor.getValue().size());
		assertEquals(analyzerHousing.getId(), deleteCaptor.getValue().get(0).getId());
	}

	@Test
	void deletesStaleAnalyzerRowsMissingFromLatestAggregate() {
		Integer userId = 1;
		LocalDate baseMonth = LocalDate.of(2026, 3, 1);
		LocalDateTime analyzedAt = LocalDateTime.of(2026, 3, 10, 12, 0);
		Category food = Category.create(20, "식비");
		Category transport = Category.create(30, "교통비");
		BudgetMonth marchBudgetMonth = budgetMonth(100, userId, baseMonth);
		EssentialExpenseMonth staleTransport = essentialExpenseMonth(
			902,
			marchBudgetMonth,
			transport,
			ExpenseScope.VARIABLE,
			30_000L,
			SourceProvider.ANALYZER
		);

		given(transactionAnalysisMetadataRepository.findTopByUser_IdOrderByAnalyzedAtDesc(userId))
			.willReturn(Optional.of(metadata(userId, analyzedAt, LocalDateTime.of(2025, 12, 1, 0, 0), LocalDateTime.of(2026, 2, 28, 23, 59))));
		given(transactionAnalysisResultRepository.findAllByUserIdAndAnalyzedAtAndTransactionDateTimeBetween(
			userId,
			analyzedAt,
			LocalDateTime.of(2025, 12, 1, 0, 0),
			LocalDateTime.of(2026, 2, 28, 23, 59)
		)).willReturn(List.of(
			analysisResult(1, food, ExpenseScope.VARIABLE, 24_000L, LocalDateTime.of(2025, 12, 10, 9, 0), true),
			analysisResult(2, null, null, 8_000L, LocalDateTime.of(2026, 2, 12, 9, 0), false)
		));
		given(essentialExpenseMonthRepository.findAllByBudgetMonth_User_IdAndBudgetMonth_BaseDate(userId, baseMonth))
			.willReturn(List.of(staleTransport));
		given(budgetMonthRepository.findByUser_IdAndBaseDate(userId, baseMonth))
			.willReturn(Optional.of(marchBudgetMonth));
		given(essentialExpenseMonthRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		useCase.execute(userId, baseMonth);

		ArgumentCaptor<List<EssentialExpenseMonth>> deleteCaptor = ArgumentCaptor.forClass(List.class);
		verify(essentialExpenseMonthRepository).deleteAll(deleteCaptor.capture());
		assertEquals(1, deleteCaptor.getValue().size());
		assertEquals(staleTransport.getId(), deleteCaptor.getValue().get(0).getId());
	}

	@Test
	void deletesReferencingWeeklyAllocationsBeforeDeletingStaleAnalyzerRows() {
		Integer userId = 1;
		LocalDate baseMonth = LocalDate.of(2026, 3, 1);
		LocalDateTime analyzedAt = LocalDateTime.of(2026, 3, 10, 12, 0);
		Category food = Category.create(20, "식비");
		Category transport = Category.create(30, "교통비");
		BudgetMonth marchBudgetMonth = budgetMonth(100, userId, baseMonth);
		EssentialExpenseMonth staleTransport = essentialExpenseMonth(
			902,
			marchBudgetMonth,
			transport,
			ExpenseScope.VARIABLE,
			30_000L,
			SourceProvider.ANALYZER
		);
		EssentialWeekAllocation weeklyAllocation = essentialWeekAllocation(501, staleTransport, transport, userId);

		given(transactionAnalysisMetadataRepository.findTopByUser_IdOrderByAnalyzedAtDesc(userId))
			.willReturn(Optional.of(metadata(userId, analyzedAt, LocalDateTime.of(2025, 12, 1, 0, 0), LocalDateTime.of(2026, 2, 28, 23, 59))));
		given(transactionAnalysisResultRepository.findAllByUserIdAndAnalyzedAtAndTransactionDateTimeBetween(
			userId,
			analyzedAt,
			LocalDateTime.of(2025, 12, 1, 0, 0),
			LocalDateTime.of(2026, 2, 28, 23, 59)
		)).willReturn(List.of(
			analysisResult(1, food, ExpenseScope.VARIABLE, 24_000L, LocalDateTime.of(2025, 12, 10, 9, 0), true)
		));
		given(essentialExpenseMonthRepository.findAllByBudgetMonth_User_IdAndBudgetMonth_BaseDate(userId, baseMonth))
			.willReturn(List.of(staleTransport));
		given(essentialWeekAllocationRepository.findAllByEssentialExpenseMonthIn(List.of(staleTransport)))
			.willReturn(List.of(weeklyAllocation));
		given(budgetMonthRepository.findByUser_IdAndBaseDate(userId, baseMonth))
			.willReturn(Optional.of(marchBudgetMonth));
		given(essentialExpenseMonthRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		useCase.execute(userId, baseMonth);

		InOrder inOrder = org.mockito.Mockito.inOrder(essentialWeekAllocationRepository, essentialExpenseMonthRepository);
		inOrder.verify(essentialWeekAllocationRepository).deleteAll(List.of(weeklyAllocation));
		inOrder.verify(essentialExpenseMonthRepository).deleteAll(List.of(staleTransport));
	}

	private TransactionAnalysisMetadata metadata(
		Integer userId,
		LocalDateTime analyzedAt,
		LocalDateTime windowStartAt,
		LocalDateTime windowEndAt
	) {
		return TransactionAnalysisMetadata.builder()
			.user(user(userId))
			.requestedMonths(6)
			.windowStartAt(windowStartAt)
			.windowEndAt(windowEndAt)
			.transactionCount(4)
			.latestTransactionAt(windowEndAt)
			.analyzedAt(analyzedAt)
			.build();
	}

	private TransactionAnalysisResult analysisResult(
		int transactionId,
		Category category,
		ExpenseScope expenseScope,
		long amount,
		LocalDateTime transactionDateTime,
		boolean analysisIncluded
	) {
		return TransactionAnalysisResult.builder()
			.transaction(TransactionOverride.builder()
				.id(transactionId)
				.category(category != null ? category : Category.create(999, "기타"))
				.transactionDateTime(transactionDateTime)
				.transactionBalance(amount)
				.transactionMemo("memo")
				.build())
			.category(category)
			.analysisIncluded(analysisIncluded)
			.expenseScope(expenseScope)
			.analyzedAt(LocalDateTime.of(2026, 3, 10, 12, 0))
			.build();
	}

	private BudgetMonth budgetMonth(int id, int userId, LocalDate baseDate) {
		BudgetMonth budgetMonth = BudgetMonth.builder()
			.user(user(userId))
			.baseDate(baseDate)
			.status(BudgetMonthStatus.OPEN)
			.build();
		ReflectionTestUtils.setField(budgetMonth, "id", id);
		return budgetMonth;
	}

	private EssentialExpenseMonth essentialExpenseMonth(
		int id,
		BudgetMonth budgetMonth,
		Category category,
		ExpenseScope expenseScope,
		long amount,
		SourceProvider sourceProvider
	) {
		return EssentialExpenseMonth.builder()
			.id(id)
			.budgetMonth(budgetMonth)
			.category(category)
			.expenseScope(expenseScope.name())
			.amount(amount)
			.sourceProvider(sourceProvider)
			.build();
	}

	private EssentialWeekAllocation essentialWeekAllocation(
		int id,
		EssentialExpenseMonth essentialExpenseMonth,
		Category category,
		int userId
	) {
		return EssentialWeekAllocation.builder()
			.id(id)
			.essentialExpenseMonth(essentialExpenseMonth)
			.category(category)
			.budgetWeek(BudgetWeek.builder()
				.id(301)
				.user(user(userId))
				.weekSequence(1)
				.weekStartDate(LocalDate.of(2026, 3, 2))
				.weekEndDate(LocalDate.of(2026, 3, 8))
				.periodDays(7)
				.status(BudgetWeekStatus.ACTIVE)
				.build())
			.expenseScope(ExpenseScope.VARIABLE)
			.plannedAmount(30_000L)
			.actualSpentAmount(0L)
			.build();
	}

	private User user(int id) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private EssentialExpenseMonth findByKey(
		List<EssentialExpenseMonth> essentials,
		int categoryId,
		ExpenseScope expenseScope
	) {
		return essentials.stream()
			.filter(essential -> essential.getCategory().getId() == categoryId)
			.filter(essential -> expenseScope.name().equals(essential.getExpenseScope()))
			.findFirst()
			.orElseThrow();
	}
}
