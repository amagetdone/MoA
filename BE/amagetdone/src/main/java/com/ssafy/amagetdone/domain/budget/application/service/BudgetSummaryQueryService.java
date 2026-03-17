package com.ssafy.amagetdone.domain.budget.application.service;

import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetSummaryDto;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetSummaryQueryService {

	private final PlanRepository planRepository;

	public BudgetSummaryDto.Response getSummary(Integer userId) {
		long activePlanCount = planRepository.countByUser_IdAndStatus(userId, PlanStatus.ACTIVE);
		long completedPlanCount = planRepository.countByUser_IdAndStatus(userId, PlanStatus.COMPLETED);
		long totalSavedAmount = Optional.ofNullable(planRepository.sumSavedAmountByUserId(userId)).orElse(0L);

		return new BudgetSummaryDto.Response(activePlanCount, completedPlanCount, totalSavedAmount);
	}
}
