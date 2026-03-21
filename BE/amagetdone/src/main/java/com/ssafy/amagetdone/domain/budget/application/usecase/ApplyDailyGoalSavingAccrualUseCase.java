package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.application.service.DailyGoalSavingAccrualService;
import com.ssafy.amagetdone.domain.budget.application.service.WeeklyGoalSavingAccrualService;

import org.springframework.stereotype.Component;

@Component
public class ApplyDailyGoalSavingAccrualUseCase {

	private final WeeklyGoalSavingAccrualService weeklyGoalSavingAccrualService;
	private final DailyGoalSavingAccrualService dailyGoalSavingAccrualService;

	public ApplyDailyGoalSavingAccrualUseCase(
		WeeklyGoalSavingAccrualService weeklyGoalSavingAccrualService,
		DailyGoalSavingAccrualService dailyGoalSavingAccrualService
	) {
		this.weeklyGoalSavingAccrualService = weeklyGoalSavingAccrualService;
		this.dailyGoalSavingAccrualService = dailyGoalSavingAccrualService;
	}

	public GoalSavingAccrualForceDto.Response execute(Integer userId) {
		weeklyGoalSavingAccrualService.applyWeeklyAccrual(userId);
		return null;
	}

	public GoalSavingAccrualForceDto.Response backfill(Integer userId, GoalSavingAccrualForceDto.BackfillRequest request) {
		return dailyGoalSavingAccrualService.backfillAccrual(userId, request);
	}
}
