package com.ssafy.amagetdone.domain.plan.application.usecase;

import com.ssafy.amagetdone.domain.plan.application.service.PlanPriorityService;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeletePlanUseCase {

	private final PlanRepository planRepository;
	private final PlanPriorityService planPriorityService;

	public DeletePlanUseCase(PlanRepository planRepository, PlanPriorityService planPriorityService) {
		this.planRepository = planRepository;
		this.planPriorityService = planPriorityService;
	}

	@Transactional
	public void execute(Integer userId, Integer planId) {
		Plan plan = planRepository.findByIdAndUserIdAndDeletedFalse(planId, userId)
			.orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));

		Integer deletedPriority = plan.getPriority();
		plan.cancelAndDelete();
		if (deletedPriority != null) {
			planPriorityService.compactPrioritiesAfterDeletion(userId, deletedPriority);
		}
	}
}
