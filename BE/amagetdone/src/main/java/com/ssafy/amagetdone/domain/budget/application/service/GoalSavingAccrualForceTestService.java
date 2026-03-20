package com.ssafy.amagetdone.domain.budget.application.service;

import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.application.usecase.ApplyGoalSavingAccrualForceUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.ApplyDailyGoalSavingAccrualUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class GoalSavingAccrualForceTestService {

	private final ApplyDailyGoalSavingAccrualUseCase applyDailyGoalSavingAccrualUseCase;
	private final ApplyGoalSavingAccrualForceUseCase applyGoalSavingAccrualForceUseCase;
	private final BudgetHistoricalRebuildService budgetHistoricalRebuildService;

	public GoalSavingAccrualForceDto.Response backfill(Integer userId, GoalSavingAccrualForceDto.BackfillRequest request) {
		if (request != null && request.planId() != null) {
			return budgetHistoricalRebuildService.rebuild(userId, request.planId());
		}
		return applyDailyGoalSavingAccrualUseCase.backfill(userId, request);
	}

	public GoalSavingAccrualForceDto.Response apply(Integer userId, GoalSavingAccrualForceDto.ApplyRequest request) {
		return applyGoalSavingAccrualForceUseCase.apply(userId, request);
	}
}
