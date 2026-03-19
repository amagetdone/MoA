package com.ssafy.amagetdone.domain.plan.application.usecase;

import com.ssafy.amagetdone.domain.budget.application.usecase.SyncBudgetStateOnReadUseCase;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanDetailResponse;
import com.ssafy.amagetdone.domain.plan.application.service.PlanQueryService;

import org.springframework.stereotype.Service;

@Service
public class GetPlanDetailUseCase {

	private final SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase;
	private final PlanQueryService planQueryService;

	public GetPlanDetailUseCase(
		SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase,
		PlanQueryService planQueryService
	) {
		this.syncBudgetStateOnReadUseCase = syncBudgetStateOnReadUseCase;
		this.planQueryService = planQueryService;
	}

	public PlanDetailResponse execute(Integer userId, Integer planId) {
		syncBudgetStateOnReadUseCase.execute(userId);
		return planQueryService.getPlanDetail(userId, planId);
	}
}
