package com.ssafy.amagetdone.domain.budget.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetMonthStatus;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.vo.UserStatus;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisResult;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionExcludedReason;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BudgetSpentUpdateServiceTest {

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private EssentialWeekAllocationRepository essentialWeekAllocationRepository;

	@Test
	void updateSpentAmountsRoutesEssentialAndFreeSpendingToSeparateBudgetBuckets() {
		BudgetSpentUpdateService service = new BudgetSpentUpdateService(budgetWeekRepository, essentialWeekAllocationRepository);
		User user = user(1, "user-key-1");
		Category housingCategory = Category.create(12, "주거/통신");
		Category foodCategory = Category.create(10, "식비");
		BudgetMonth budgetMonth = BudgetMonth.builder()
			.id(1)
			.user(user)
			.baseDate(LocalDate.of(2026, 3, 1))
			.status(BudgetMonthStatus.OPEN)
			.build();
		BudgetWeek budgetWeek = BudgetWeek.builder()
			.id(101)
			.user(user)
			.weekSequence(1)
			.weekStartDate(LocalDate.of(2026, 3, 2))
			.weekEndDate(LocalDate.of(2026, 3, 8))
			.actualSpentAmount(0L)
			.build();
		EssentialExpenseMonth essentialExpenseMonth = EssentialExpenseMonth.builder()
			.id(201)
			.category(housingCategory)
			.budgetMonth(budgetMonth)
			.expenseScope(ExpenseScope.FIXED.name())
			.amount(55_000L)
			.build();
		EssentialWeekAllocation housingAllocation = EssentialWeekAllocation.builder()
			.id(301)
			.essentialExpenseMonth(essentialExpenseMonth)
			.category(housingCategory)
			.budgetWeek(budgetWeek)
			.expenseScope(ExpenseScope.FIXED)
			.plannedAmount(55_000L)
			.actualSpentAmount(0L)
			.build();

		Account account = account(user, "ACC-1", "111-222-333");
		TransactionAnalysisResult essentialResult = analysisResult(
			accountTransaction(101, housingCategory, account, 1_001L, LocalDateTime.of(2026, 3, 4, 9, 0), 55_000L, "통신비"),
			housingCategory,
			true,
			null,
			ExpenseScope.FIXED
		);
		TransactionAnalysisResult freeSpendingResult = analysisResult(
			accountTransaction(102, foodCategory, account, 1_002L, LocalDateTime.of(2026, 3, 5, 19, 0), 83_000L, "이마트"),
			foodCategory,
			false,
			TransactionExcludedReason.NON_ESSENTIAL,
			null
		);
		TransactionAnalysisResult incomeResult = analysisResult(
			accountTransaction(103, foodCategory, account, 1_003L, LocalDateTime.of(2026, 3, 6, 9, 0), 2_000_000L, "급여"),
			foodCategory,
			false,
			TransactionExcludedReason.INCOME,
			null
		);
		TransactionAnalysisResult internalTransferResult = analysisResult(
			accountTransaction(104, foodCategory, account, 1_004L, LocalDateTime.of(2026, 3, 7, 9, 0), 300_000L, "내계좌이체"),
			foodCategory,
			false,
			TransactionExcludedReason.INTERNAL_TRANSFER,
			null
		);

		given(budgetWeekRepository.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(any(), any(), any()))
			.willReturn(Optional.of(budgetWeek));
		given(essentialWeekAllocationRepository.findByBudgetWeek(budgetWeek)).willReturn(List.of(housingAllocation));

		service.updateSpentAmounts(
			user,
			List.of(essentialResult, freeSpendingResult, incomeResult, internalTransferResult)
		);

		assertEquals(55_000L, housingAllocation.getActualSpentAmount());
		assertEquals(83_000L, budgetWeek.getActualSpentAmount());
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

	private AccountTransactionOverride accountTransaction(
		Integer id,
		Category category,
		Account account,
		Long transactionUniqueNo,
		LocalDateTime transactionDateTime,
		Long transactionBalance,
		String transactionSummary
	) {
		return AccountTransactionOverride.builder()
			.id(id)
			.category(category)
			.account(account)
			.transactionUniqueNo(transactionUniqueNo)
			.transactionDateTime(transactionDateTime)
			.transactionBalance(transactionBalance)
			.transactionAfterBalance(0L)
			.transactionSummary(transactionSummary)
			.transactionMemo(transactionSummary)
			.transactionType(2)
			.transactionTypeName("출금")
			.build();
	}

	private TransactionAnalysisResult analysisResult(
		AccountTransactionOverride transaction,
		Category category,
		boolean analysisIncluded,
		TransactionExcludedReason excludedReason,
		ExpenseScope expenseScope
	) {
		return TransactionAnalysisResult.builder()
			.transaction(transaction)
			.category(category)
			.analysisIncluded(analysisIncluded)
			.excludedReason(excludedReason)
			.expenseScope(expenseScope)
			.analyzedAt(transaction.getTransactionDateTime())
			.build();
	}
}
