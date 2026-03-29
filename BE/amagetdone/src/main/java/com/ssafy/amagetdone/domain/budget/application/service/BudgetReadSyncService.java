package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.ApplyDailyGoalSavingAccrualUseCase;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncService;
import com.ssafy.amagetdone.domain.transaction.application.usecase.RunTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.SyncLatestTransactionsUseCase;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetReadSyncService {

	private static final int READ_SYNC_MONTHS = 3;

	private final TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase;
	private final SyncLatestTransactionsUseCase syncLatestTransactionsUseCase;
	private final RunTransactionAnalysisUseCase runTransactionAnalysisUseCase;
	private final BudgetActualSpentRefreshService budgetActualSpentRefreshService;
	private final BudgetRolloverResyncService budgetRolloverResyncService;
	private final ApplyDailyGoalSavingAccrualUseCase applyDailyGoalSavingAccrualUseCase;
	private final BudgetWeekRepository budgetWeekRepository;
	private final PlanRepository planRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final Clock clock;
	private final EntityManager entityManager;

	public void sync(Integer userId) {
		LocalDate today = LocalDate.now(clock);
		ensureCurrentWeekAllocation(userId, today);

		Long preservedFinalFreeAmount = preserveCurrentWeekFreeAmount(userId, today);

		TransactionSyncService.SyncResult syncResult = syncLatestTransactionsUseCase.execute(userId, READ_SYNC_MONTHS, false);
		if (!hasNewTransactions(syncResult)) {
			applyDailyGoalSavingAccrualUseCase.execute(userId);
			flushAndClear();
			return;
		}

		runTransactionAnalysisUseCase.execute(userId, READ_SYNC_MONTHS);
		runTransactionAnalysisUseCase.execute(userId, YearMonth.from(today));
		triggerBudgetAllocationUseCase.execute(userId);
		applyDailyGoalSavingAccrualUseCase.backfill(userId, null);
		budgetActualSpentRefreshService.refresh(userId, today);
		budgetRolloverResyncService.resync(userId);
		applyDailyGoalSavingAccrualUseCase.execute(userId);

		restoreCurrentWeekFreeAmount(userId, today, preservedFinalFreeAmount);
		flushAndClear();
	}

	private Long preserveCurrentWeekFreeAmount(Integer userId, LocalDate today) {
		return budgetWeekRepository
			.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(userId, today, today)
			.map(BudgetWeek::getFinalFreeAmount)
			.orElse(null);
	}

	private void restoreCurrentWeekFreeAmount(Integer userId, LocalDate today, Long preservedAmount) {
		if (preservedAmount == null) {
			return;
		}
		budgetWeekRepository
			.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(userId, today, today)
			.ifPresent(currentWeek -> currentWeek.setFinalFreeAmount(preservedAmount));
	}

	private void ensureCurrentWeekAllocation(Integer userId, LocalDate today) {
		BudgetWeek currentWeek = budgetWeekRepository
			.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(userId, today, today)
			.orElse(null);
		if (currentWeek == null) {
			triggerBudgetAllocationUseCase.execute(userId);
			applyDailyGoalSavingAccrualUseCase.backfill(userId, null);
			return;
		}

		Set<Integer> currentAllocatedPlanIds = planWeekAllocationRepository.findByBudgetWeek(currentWeek).stream()
			.map(allocation -> allocation.getPlan())
			.filter(java.util.Objects::nonNull)
			.map(Plan::getId)
			.filter(java.util.Objects::nonNull)
			.collect(Collectors.toSet());
		boolean missingActiveAllocation = planRepository.findAllByUser_IdAndStatusAndDeletedFalse(userId, PlanStatus.ACTIVE).stream()
			.filter(Plan::isAutoAllocateEnabled)
			.map(Plan::getId)
			.filter(java.util.Objects::nonNull)
			.anyMatch(planId -> !currentAllocatedPlanIds.contains(planId));
		if (missingActiveAllocation) {
			triggerBudgetAllocationUseCase.execute(userId);
			applyDailyGoalSavingAccrualUseCase.backfill(userId, null);
		}
	}

	private boolean hasNewTransactions(TransactionSyncService.SyncResult syncResult) {
		return syncResult != null
			&& (syncResult.insertedAccountCount() > 0 || syncResult.insertedCardCount() > 0);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
