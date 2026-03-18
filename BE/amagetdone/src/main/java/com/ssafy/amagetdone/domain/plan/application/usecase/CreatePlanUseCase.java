package com.ssafy.amagetdone.domain.plan.application.usecase;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateRequest;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateResponse;
import com.ssafy.amagetdone.domain.plan.application.event.PlanCreatedEvent;
import com.ssafy.amagetdone.domain.plan.application.service.PlanPriorityService;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePlanUseCase {

	private final UserRepository userRepository;
	private final PlanRepository planRepository;
	private final PlanPriorityService planPriorityService;
	private final ApplicationEventPublisher applicationEventPublisher;

	public CreatePlanUseCase(
		UserRepository userRepository,
		PlanRepository planRepository,
		PlanPriorityService planPriorityService,
		ApplicationEventPublisher applicationEventPublisher
	) {
		this.userRepository = userRepository;
		this.planRepository = planRepository;
		this.planPriorityService = planPriorityService;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Transactional
	public PlanCreateResponse execute(Integer userId, PlanCreateRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_OWNER_NOT_FOUND));

		planPriorityService.shiftPriorities(userId, request.priority());

		Plan savedPlan = planRepository.save(Plan.create(
			user,
			request.title(),
			request.description(),
			request.targetAmount(),
			request.startDate(),
			request.endDate(),
			request.priority()
		));

		applicationEventPublisher.publishEvent(new PlanCreatedEvent(savedPlan.getId(), userId));

		return new PlanCreateResponse(
			savedPlan.getId(),
			savedPlan.getTitle(),
			savedPlan.getDescription(),
			savedPlan.getTargetAmount(),
			savedPlan.getStartDate(),
			savedPlan.getEndDate(),
			savedPlan.getPriority(),
			savedPlan.getStatus().name()
		);
	}
}
