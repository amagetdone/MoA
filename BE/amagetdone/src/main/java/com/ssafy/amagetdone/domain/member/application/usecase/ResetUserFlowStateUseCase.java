package com.ssafy.amagetdone.domain.member.application.usecase;

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

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ResetUserFlowStateUseCase {

	private final PocketRepository pocketRepository;
	private final PlanRepository planRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetAdjustmentRepository budgetAdjustmentRepository;
	private final BudgetRolloverRepository budgetRolloverRepository;
	private final BudgetWeekRepository budgetWeekRepository;
	private final BudgetMonthRepository budgetMonthRepository;
	private final EssentialWeekAllocationRepository essentialWeekAllocationRepository;
	private final EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository;
	private final EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository;
	private final EssentialExpenseMonthRepository essentialExpenseMonthRepository;
	private final TransactionAnalysisResultRepository transactionAnalysisResultRepository;
	private final AccountTransactionOverrideRepository accountTransactionOverrideRepository;
	private final CardTransactionOverrideRepository cardTransactionOverrideRepository;
	private final TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository;

	public ResetUserFlowStateUseCase(
		PocketRepository pocketRepository,
		PlanRepository planRepository,
		PlanWeekAllocationRepository planWeekAllocationRepository,
		BudgetAdjustmentRepository budgetAdjustmentRepository,
		BudgetRolloverRepository budgetRolloverRepository,
		BudgetWeekRepository budgetWeekRepository,
		BudgetMonthRepository budgetMonthRepository,
		EssentialWeekAllocationRepository essentialWeekAllocationRepository,
		EssentialExpenseSnapshotWeekCategoryRepository essentialExpenseSnapshotWeekCategoryRepository,
		EssentialExpenseSnapshotWeekRepository essentialExpenseSnapshotWeekRepository,
		EssentialExpenseMonthRepository essentialExpenseMonthRepository,
		TransactionAnalysisResultRepository transactionAnalysisResultRepository,
		AccountTransactionOverrideRepository accountTransactionOverrideRepository,
		CardTransactionOverrideRepository cardTransactionOverrideRepository,
		TransactionAnalysisMetadataRepository transactionAnalysisMetadataRepository
	) {
		this.pocketRepository = pocketRepository;
		this.planRepository = planRepository;
		this.planWeekAllocationRepository = planWeekAllocationRepository;
		this.budgetAdjustmentRepository = budgetAdjustmentRepository;
		this.budgetRolloverRepository = budgetRolloverRepository;
		this.budgetWeekRepository = budgetWeekRepository;
		this.budgetMonthRepository = budgetMonthRepository;
		this.essentialWeekAllocationRepository = essentialWeekAllocationRepository;
		this.essentialExpenseSnapshotWeekCategoryRepository = essentialExpenseSnapshotWeekCategoryRepository;
		this.essentialExpenseSnapshotWeekRepository = essentialExpenseSnapshotWeekRepository;
		this.essentialExpenseMonthRepository = essentialExpenseMonthRepository;
		this.transactionAnalysisResultRepository = transactionAnalysisResultRepository;
		this.accountTransactionOverrideRepository = accountTransactionOverrideRepository;
		this.cardTransactionOverrideRepository = cardTransactionOverrideRepository;
		this.transactionAnalysisMetadataRepository = transactionAnalysisMetadataRepository;
	}

	@Transactional
	public void execute(Integer userId) {
		transactionAnalysisResultRepository.deleteAllByUserId(userId);
		accountTransactionOverrideRepository.deleteAllByUserId(userId);
		cardTransactionOverrideRepository.deleteAllByUserId(userId);
		transactionAnalysisMetadataRepository.deleteAllByUserId(userId);
		budgetAdjustmentRepository.deleteAllByUserId(userId);
		budgetRolloverRepository.deleteAllByUserId(userId);
		essentialWeekAllocationRepository.deleteAllByUserId(userId);
		essentialExpenseSnapshotWeekCategoryRepository.deleteAllByUserId(userId);
		essentialExpenseSnapshotWeekRepository.deleteAllByUserId(userId);
		essentialExpenseMonthRepository.deleteAllByUserId(userId);
		planWeekAllocationRepository.deleteAllByUserId(userId);
		pocketRepository.deleteAllByUserId(userId);
		planRepository.deleteAllByUserId(userId);
		budgetWeekRepository.deleteAllByUserId(userId);
		budgetMonthRepository.deleteAllByUserId(userId);
	}
}
