package com.ssafy.amagetdone.domain.plan.application.usecase;

import java.time.Clock;
import java.time.LocalDate;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateRequest;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateResponse;
import com.ssafy.amagetdone.domain.plan.application.event.PlanCreatedEvent;
import com.ssafy.amagetdone.domain.plan.application.service.PlanCreationBudgetSummaryService;
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
	private final TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase;
	private final PlanCreationBudgetSummaryService planCreationBudgetSummaryService;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final Clock clock;

	public CreatePlanUseCase(
		UserRepository userRepository,
		PlanRepository planRepository,
		PlanPriorityService planPriorityService,
		TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase,
		PlanCreationBudgetSummaryService planCreationBudgetSummaryService,
		ApplicationEventPublisher applicationEventPublisher,
		Clock clock
	) {
		this.userRepository = userRepository;
		this.planRepository = planRepository;
		this.planPriorityService = planPriorityService;
		this.triggerBudgetAllocationUseCase = triggerBudgetAllocationUseCase;
		this.planCreationBudgetSummaryService = planCreationBudgetSummaryService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.clock = clock;
	}

	@Transactional
	public PlanCreateResponse execute(Integer userId, PlanCreateRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_OWNER_NOT_FOUND));

		planPriorityService.shiftPriorities(userId, request.priority());

		Plan plan = Plan.create(
			user,
			request.title(),
			request.description(),
			request.targetAmount(),
			request.startDate(),
			request.endDate(),
			request.priority()
		);
		if (!request.startDate().isAfter(LocalDate.now(clock))) {
			plan.activate();
		}

		Plan savedPlan = planRepository.save(plan);
		triggerBudgetAllocationUseCase.execute(userId);
		PlanCreateResponse response = planCreationBudgetSummaryService.build(savedPlan);

		applicationEventPublisher.publishEvent(new PlanCreatedEvent(savedPlan.getId(), userId));

		return response;
	}
}
