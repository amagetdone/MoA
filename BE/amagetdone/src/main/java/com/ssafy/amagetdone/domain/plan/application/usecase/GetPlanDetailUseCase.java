package com.ssafy.amagetdone.domain.plan.application.usecase;

import com.ssafy.amagetdone.domain.plan.api.dto.PlanDetailResponse;
import com.ssafy.amagetdone.domain.plan.application.service.PlanQueryService;

import org.springframework.stereotype.Service;

@Service
public class GetPlanDetailUseCase {

	private final PlanQueryService planQueryService;

	public GetPlanDetailUseCase(PlanQueryService planQueryService) {
		this.planQueryService = planQueryService;
	}

	public PlanDetailResponse execute(Integer userId, Integer planId) {
		return planQueryService.getPlanDetail(userId, planId);
	}
}
