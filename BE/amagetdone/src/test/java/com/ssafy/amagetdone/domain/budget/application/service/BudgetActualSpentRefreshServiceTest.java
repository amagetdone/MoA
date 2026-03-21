package com.ssafy.amagetdone.domain.budget.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetMonthStatus;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.vo.UserStatus;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisMetadataRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionExcludedReason;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BudgetActualSpentRefreshServiceTest {

	@Mock
	private TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository;

	@Mock
	private TransactionAnalysisResultRepository transactionAnalysisResultRepository;

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private EssentialWeekAllocationRepository essentialWeekAllocationRepository;

	private BudgetActualSpentRefreshService service;

	@BeforeEach
	void setUp() {
		service = new BudgetActualSpentRefreshService(
			transactionAnalysisMetadataRepository,
			transactionAnalysisResultRepository,
			budgetWeekRepository,
			essentialWeekAllocationRepository
		);
	}

	@Test
	void refreshRecomputesTargetWeeksFromLatestAnalysisAndResetsStaleValues() {
		Integer userId = 1;
		LocalDate fromDate = LocalDate.of(2026, 3, 3);
		LocalDate toDate = LocalDate.of(2026, 3, 16);
		LocalDateTime analyzedAt = LocalDateTime.of(2026, 3, 20, 12, 0);
		User user = user(userId);
		Category housing = Category.create(10, "주거비");

		BudgetWeek week1 = budgetWeek(user, 1, LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 9), 900L);
		BudgetWeek week2 = budgetWeek(user, 2, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 16), 700L);
		EssentialWeekAllocation week1Allocation = essentialAllocation(101, week1, housing, ExpenseScope.FIXED, 800L);
		EssentialWeekAllocation week2Allocation = essentialAllocation(102, week2, housing, ExpenseScope.FIXED, 600L);

		given(transactionAnalysisMetadataRepository.findTopByUser_IdOrderByAnalyzedAtDesc(userId))
			.willReturn(Optional.of(metadata(userId, analyzedAt)));
		given(budgetWeekRepository.findAllByUser_IdOrderByWeekStartDateAsc(userId)).willReturn(List.of(week1, week2));
		given(transactionAnalysisResultRepository.findAllByUserIdAndAnalyzedAtAndTransactionDateTimeBetweenOrderByTransactionDateTimeAsc(
			userId,
			analyzedAt,
			fromDate.atStartOfDay(),
			toDate.atTime(LocalTime.MAX)
		)).willReturn(List.of(
			analysisResult(1, housing, ExpenseScope.FIXED, 50_000L, LocalDateTime.of(2026, 3, 4, 9, 0), true, null),
			analysisResult(2, housing, ExpenseScope.FIXED, 20_000L, LocalDateTime.of(2026, 3, 10, 9, 0), false, TransactionExcludedReason.NON_ESSENTIAL)
		));
		given(essentialWeekAllocationRepository.findByBudgetWeek(week1)).willReturn(List.of(week1Allocation));
		given(essentialWeekAllocationRepository.findByBudgetWeek(week2)).willReturn(List.of(week2Allocation));

		service.refresh(userId, fromDate, toDate);

		assertEquals(0L, week1.getActualSpentAmount());
		assertEquals(20_000L, week2.getActualSpentAmount());
		assertEquals(50_000L, week1Allocation.getActualSpentAmount());
		assertEquals(0L, week2Allocation.getActualSpentAmount());
	}

	@Test
	void refreshIgnoresNonPositiveAndNonExpenseTransactionsWhileKeepingLatestAnalysisOnly() {
		Integer userId = 1;
		LocalDate fromDate = LocalDate.of(2026, 3, 3);
		LocalDate toDate = LocalDate.of(2026, 3, 9);
		LocalDateTime analyzedAt = LocalDateTime.of(2026, 3, 20, 12, 0);
		User user = user(userId);
		Category food = Category.create(20, "식비");
		Category transport = Category.create(30, "교통비");

		BudgetWeek week = budgetWeek(user, 1, fromDate, toDate, 11_000L);
		EssentialWeekAllocation foodAllocation = essentialAllocation(201, week, food, ExpenseScope.VARIABLE, 9_000L);
		EssentialWeekAllocation transportAllocation = essentialAllocation(202, week, transport, ExpenseScope.VARIABLE, 4_000L);

		given(transactionAnalysisMetadataRepository.findTopByUser_IdOrderByAnalyzedAtDesc(userId))
			.willReturn(Optional.of(metadata(userId, analyzedAt)));
		given(budgetWeekRepository.findAllByUser_IdOrderByWeekStartDateAsc(userId)).willReturn(List.of(week));
		given(transactionAnalysisResultRepository.findAllByUserIdAndAnalyzedAtAndTransactionDateTimeBetweenOrderByTransactionDateTimeAsc(
			userId,
			analyzedAt,
			fromDate.atStartOfDay(),
			toDate.atTime(LocalTime.MAX)
		)).willReturn(List.of(
			analysisResult(3, food, ExpenseScope.VARIABLE, 0L, LocalDateTime.of(2026, 3, 4, 9, 0), true, null),
			analysisResult(4, food, ExpenseScope.VARIABLE, 12_500L, LocalDateTime.of(2026, 3, 5, 9, 0), true, null),
			analysisResult(5, transport, ExpenseScope.VARIABLE, 3_000L, LocalDateTime.of(2026, 3, 6, 9, 0), false, TransactionExcludedReason.INCOME),
			analysisResult(6, transport, ExpenseScope.VARIABLE, -1_000L, LocalDateTime.of(2026, 3, 7, 9, 0), false, TransactionExcludedReason.NON_ESSENTIAL),
			analysisResult(7, transport, ExpenseScope.VARIABLE, 9_000L, LocalDateTime.of(2026, 3, 8, 9, 0), false, TransactionExcludedReason.NON_ESSENTIAL)
		));
		given(essentialWeekAllocationRepository.findByBudgetWeek(week)).willReturn(List.of(foodAllocation, transportAllocation));

		service.refresh(userId, fromDate, toDate);

		assertEquals(12_500L, foodAllocation.getActualSpentAmount());
		assertEquals(0L, transportAllocation.getActualSpentAmount());
		assertEquals(9_000L, week.getActualSpentAmount());
		verify(transactionAnalysisResultRepository).findAllByUserIdAndAnalyzedAtAndTransactionDateTimeBetweenOrderByTransactionDateTimeAsc(
			userId,
			analyzedAt,
			fromDate.atStartOfDay(),
			toDate.atTime(LocalTime.MAX)
		);
	}

	private TransactionAnalysisMetadata metadata(Integer userId, LocalDateTime analyzedAt) {
		return TransactionAnalysisMetadata.builder()
			.user(user(userId))
			.requestedMonths(3)
			.windowStartAt(analyzedAt.minusMonths(3))
			.windowEndAt(analyzedAt)
			.transactionCount(3)
			.latestTransactionAt(analyzedAt)
			.analyzedAt(analyzedAt)
			.build();
	}

	private TransactionAnalysisResult analysisResult(
		int transactionId,
		Category category,
		ExpenseScope expenseScope,
		long amount,
		LocalDateTime transactionDateTime,
		boolean analysisIncluded,
		TransactionExcludedReason excludedReason
	) {
		return TransactionAnalysisResult.builder()
			.transaction(TransactionOverride.builder()
				.id(transactionId)
				.category(category)
				.transactionDateTime(transactionDateTime)
				.transactionBalance(amount)
				.transactionMemo("memo")
				.build())
			.category(category)
			.analysisIncluded(analysisIncluded)
			.excludedReason(excludedReason)
			.expenseScope(expenseScope)
			.analyzedAt(LocalDateTime.of(2026, 3, 20, 12, 0))
			.build();
	}

	private BudgetWeek budgetWeek(User user, int weekSequence, LocalDate startDate, LocalDate endDate, long staleActualSpentAmount) {
		return BudgetWeek.builder()
			.user(user)
			.weekSequence(weekSequence)
			.weekStartDate(startDate)
			.weekEndDate(endDate)
			.actualSpentAmount(staleActualSpentAmount)
			.plannedIncomeAmount(1_000_000L)
			.plannedEssentialAmount(200_000L)
			.finalFreeAmount(300_000L)
			.build();
	}

	private EssentialWeekAllocation essentialAllocation(
		int id,
		BudgetWeek budgetWeek,
		Category category,
		ExpenseScope expenseScope,
		long staleActualSpentAmount
	) {
		return EssentialWeekAllocation.builder()
			.id(id)
			.budgetWeek(budgetWeek)
			.category(category)
			.expenseScope(expenseScope)
			.actualSpentAmount(staleActualSpentAmount)
			.plannedAmount(100_000L)
			.build();
	}

	private User user(int userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
		return user;
	}
}
