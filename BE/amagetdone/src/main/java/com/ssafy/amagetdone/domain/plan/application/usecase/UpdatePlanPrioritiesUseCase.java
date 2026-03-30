package com.ssafy.amagetdone.domain.plan.application.usecase;

import com.ssafy.amagetdone.domain.plan.api.dto.PlanPriorityUpdateRequest;
import com.ssafy.amagetdone.domain.plan.application.service.PlanPriorityService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdatePlanPrioritiesUseCase {

	private final PlanPriorityService planPriorityService;

	public UpdatePlanPrioritiesUseCase(PlanPriorityService planPriorityService) {
		this.planPriorityService = planPriorityService;
	}

	@Transactional
	public void execute(Integer userId, PlanPriorityUpdateRequest request) {
		planPriorityService.updatePriorities(
			userId,
			request.plans().stream()
				.map(item -> new PlanPriorityService.PriorityUpdateCommand(item.planId(), item.priority()))
				.toList()
		);
	}
}
