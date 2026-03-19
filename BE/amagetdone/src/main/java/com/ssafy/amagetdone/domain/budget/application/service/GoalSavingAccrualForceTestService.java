package com.ssafy.amagetdone.domain.budget.application.service;

import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.application.usecase.ApplyDailyGoalSavingAccrualUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class GoalSavingAccrualForceTestService {

	private final ApplyDailyGoalSavingAccrualUseCase applyDailyGoalSavingAccrualUseCase;

	public GoalSavingAccrualForceDto.Response force(Integer userId, GoalSavingAccrualForceDto.Request request) {
		return applyDailyGoalSavingAccrualUseCase.force(userId, request);
	}
}
