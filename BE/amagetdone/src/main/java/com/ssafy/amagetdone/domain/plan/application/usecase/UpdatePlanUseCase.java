package com.ssafy.amagetdone.domain.plan.application.usecase;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.amagetdone.domain.plan.api.dto.PlanUpdateRequest;
import com.ssafy.amagetdone.domain.plan.application.event.PlanCreatedEvent;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;

@Service
public class UpdatePlanUseCase {

	private final PlanRepository planRepository;
	private final ApplicationEventPublisher applicationEventPublisher;

	public UpdatePlanUseCase(PlanRepository planRepository, ApplicationEventPublisher applicationEventPublisher) {
		this.planRepository = planRepository;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Transactional
	public void execute(Integer userId, Integer planId, PlanUpdateRequest request) {
		Plan plan = planRepository.findByIdAndUserIdAndDeletedFalse(planId, userId)
			.orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));

		if (plan.isAiProcessing()) {
			throw new PlanException(PlanErrorCode.PLAN_UPDATE_NOT_ALLOWED);
		}

		plan.updateMetadata(request.title(), request.description());
		plan.resetAiPipelineForRegeneration();
		applicationEventPublisher.publishEvent(new PlanCreatedEvent(planId, userId));
	}
}
