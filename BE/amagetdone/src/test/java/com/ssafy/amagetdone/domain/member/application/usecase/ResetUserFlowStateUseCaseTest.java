package com.ssafy.amagetdone.domain.member.application.usecase;

import static org.mockito.Mockito.verify;

import com.ssafy.amagetdone.domain.account.model.repository.PocketRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetRolloverRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekCategoryRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseSnapshotWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisMetadataRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResetUserFlowStateUseCaseTest {

	@Mock
	private PocketRepository pocketRepository;

	@Mock
	private PlanRepository planRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Mock
	private BudgetAdjustmentRepository budgetAdjustmentRepository;

	@Mock
	private BudgetRolloverRepository budgetRolloverRepository;

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private BudgetMonthRepository budgetMonthRepository;

	@Mock
	private EssentialWeekAllocationRepository essentialWeekAllocationRepository;

	@Mock
	private EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository;

	@Mock
	private EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository;

	@Mock
	private EssentialExpenseMonthRepository essentialExpenseMonthRepository;

	@Mock
	private TransactionAnalysisResultRepository transactionAnalysisResultRepository;

	@Mock
	private AccountTransactionOverrideRepository accountTransactionOverrideRepository;

	@Mock
	private CardTransactionOverrideRepository cardTransactionOverrideRepository;

	@Mock
	private TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository;

	@InjectMocks
	private ResetUserFlowStateUseCase resetUserFlowStateUseCase;

	@Test
	void executeClearsBudgetPlanAndAnalysisState() {
		resetUserFlowStateUseCase.execute(7);

		verify(transactionAnalysisResultRepository).deleteAllByUserId(7);
		verify(accountTransactionOverrideRepository).deleteAllByUserId(7);
		verify(cardTransactionOverrideRepository).deleteAllByUserId(7);
		verify(transactionAnalysisMetadataRepository).deleteAllByUserId(7);
		verify(budgetAdjustmentRepository).deleteAllByUserId(7);
		verify(budgetRolloverRepository).deleteAllByUserId(7);
		verify(essentialWeekAllocationRepository).deleteAllByUserId(7);
		verify(essentialExpenseSnapshotWeekCategoryRepository).deleteAllByUserId(7);
		verify(essentialExpenseSnapshotWeekRepository).deleteAllByUserId(7);
		verify(essentialExpenseMonthRepository).deleteAllByUserId(7);
		verify(planWeekAllocationRepository).deleteAllByUserId(7);
		verify(pocketRepository).deleteAllByUserId(7);
		verify(planRepository).deleteAllByUserId(7);
		verify(budgetWeekRepository).deleteAllByUserId(7);
		verify(budgetMonthRepository).deleteAllByUserId(7);
	}
}
