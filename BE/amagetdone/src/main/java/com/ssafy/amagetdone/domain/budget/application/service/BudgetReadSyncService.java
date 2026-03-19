package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncService;
import com.ssafy.amagetdone.domain.transaction.application.usecase.RunTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.SyncLatestTransactionsUseCase;

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
	private final BudgetWeekRepository budgetWeekRepository;
	private final PlanRepository planRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final Clock clock;

	public void sync(Integer userId) {
		LocalDate today = LocalDate.now(clock);
		ensureCurrentWeekAllocation(userId, today);

		TransactionSyncService.SyncResult syncResult = syncLatestTransactionsUseCase.execute(userId, READ_SYNC_MONTHS, false);
		if (!hasNewTransactions(syncResult)) {
			return;
		}

		runTransactionAnalysisUseCase.execute(userId, READ_SYNC_MONTHS);
		triggerBudgetAllocationUseCase.execute(userId);
		budgetActualSpentRefreshService.refresh(userId, today);
		budgetRolloverResyncService.resync(userId);
	}

	private void ensureCurrentWeekAllocation(Integer userId, LocalDate today) {
		User userReference = userReference(userId);
		BudgetWeek currentWeek = budgetWeekRepository
			.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(userReference, today, today)
			.orElse(null);
		if (currentWeek == null) {
			triggerBudgetAllocationUseCase.execute(userId);
			return;
		}

		Set<Integer> currentAllocatedPlanIds = planWeekAllocationRepository.findByBudgetWeek(currentWeek).stream()
			.map(allocation -> allocation.getPlan())
			.filter(java.util.Objects::nonNull)
			.map(Plan::getId)
			.filter(java.util.Objects::nonNull)
			.collect(Collectors.toSet());
		boolean missingActiveAllocation = planRepository.findAllByUser_IdAndStatusAndDeletedFalse(userId, PlanStatus.ACTIVE).stream()
			.map(Plan::getId)
			.filter(java.util.Objects::nonNull)
			.anyMatch(planId -> !currentAllocatedPlanIds.contains(planId));
		if (missingActiveAllocation) {
			triggerBudgetAllocationUseCase.execute(userId);
		}
	}

	private boolean hasNewTransactions(TransactionSyncService.SyncResult syncResult) {
		return syncResult != null
			&& (syncResult.insertedAccountCount() > 0 || syncResult.insertedCardCount() > 0);
	}

	private User userReference(Integer userId) {
		User user = User.createPendingSocialUser();
		user.setId(userId);
		return user;
	}
}

