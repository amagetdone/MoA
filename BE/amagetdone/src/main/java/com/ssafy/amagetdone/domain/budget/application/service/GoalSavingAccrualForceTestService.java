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

	public GoalSavingAccrualForceDto.Response backfill(Integer userId) {
		return applyDailyGoalSavingAccrualUseCase.backfill(userId, null);
	}

	public GoalSavingAccrualForceDto.Response apply(Integer userId, GoalSavingAccrualForceDto.ApplyRequest request) {
		return applyGoalSavingAccrualForceUseCase.apply(userId, request);
	}
}
