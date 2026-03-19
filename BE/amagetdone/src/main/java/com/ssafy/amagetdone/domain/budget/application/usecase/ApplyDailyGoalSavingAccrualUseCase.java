package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.application.service.DailyGoalSavingAccrualService;

import org.springframework.stereotype.Component;

@Component
public class ApplyDailyGoalSavingAccrualUseCase {

	private final DailyGoalSavingAccrualService dailyGoalSavingAccrualService;

	public ApplyDailyGoalSavingAccrualUseCase(DailyGoalSavingAccrualService dailyGoalSavingAccrualService) {
		this.dailyGoalSavingAccrualService = dailyGoalSavingAccrualService;
	}

	public GoalSavingAccrualForceDto.Response execute(Integer userId) {
		return dailyGoalSavingAccrualService.applyDailyAccrual(userId);
	}

	public GoalSavingAccrualForceDto.Response backfill(Integer userId, GoalSavingAccrualForceDto.BackfillRequest request) {
		return dailyGoalSavingAccrualService.backfillAccrual(userId, request);
	}
}
