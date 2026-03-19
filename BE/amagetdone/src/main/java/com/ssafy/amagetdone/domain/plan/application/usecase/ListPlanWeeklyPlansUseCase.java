package com.ssafy.amagetdone.domain.plan.application.usecase;

import com.ssafy.amagetdone.domain.plan.api.dto.PlanWeeklyPlansPageResponse;
import com.ssafy.amagetdone.domain.plan.application.service.PlanQueryService;

import org.springframework.stereotype.Service;

@Service
public class ListPlanWeeklyPlansUseCase {

	private final PlanQueryService planQueryService;

	public ListPlanWeeklyPlansUseCase(PlanQueryService planQueryService) {
		this.planQueryService = planQueryService;
	}

	public PlanWeeklyPlansPageResponse execute(Integer userId, Integer planId, Integer page, Integer size) {
		return planQueryService.listPlanWeeklyPlans(userId, planId, page, size);
	}
}
