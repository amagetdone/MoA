package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.application.service.GoalSavingAccrualApplyTestService;

import org.springframework.stereotype.Component;

@Component
public class ApplyGoalSavingAccrualForceUseCase {

	private final GoalSavingAccrualApplyTestService goalSavingAccrualApplyTestService;

	public ApplyGoalSavingAccrualForceUseCase(GoalSavingAccrualApplyTestService goalSavingAccrualApplyTestService) {
		this.goalSavingAccrualApplyTestService = goalSavingAccrualApplyTestService;
	}

	public GoalSavingAccrualForceDto.Response apply(Integer userId, GoalSavingAccrualForceDto.ApplyRequest request) {
		return goalSavingAccrualApplyTestService.apply(userId, request);
	}
}
