package com.ssafy.amagetdone.domain.plan.application.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlanStatusScheduler {

	private final PlanRepository planRepository;
	private final TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase;
	private final Clock clock;

	public PlanStatusScheduler(
		PlanRepository planRepository,
		TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase,
		Clock clock
	) {
		this.planRepository = planRepository;
		this.triggerBudgetAllocationUseCase = triggerBudgetAllocationUseCase;
		this.clock = clock;
	}

	@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
	@Transactional
	public void activateStartedPlans() {
		activateStartedPlans(LocalDate.now(clock));
	}

	@Scheduled(cron = "0 10 0 * * MON", zone = "Asia/Seoul")
	@Transactional
	public void reallocateActivePlansForWeekStart() {
		reallocateActivePlansForWeekStart(LocalDate.now(clock));
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void activateStartedPlansOnStartup() {
		activateStartedPlans(LocalDate.now(clock));
	}

	@Transactional
	void activateStartedPlans(LocalDate today) {
		Set<Integer> activatedUserIds = new LinkedHashSet<>();
		planRepository.findAllByStatusAndStartDateLessThanEqualAndAiPipelineStatus(
				PlanStatus.WAITING,
				today,
				PlanAiPipelineStatus.SUCCEEDED
			)
			.forEach(plan -> {
				if (plan.getStatus() == PlanStatus.WAITING) {
					plan.activate();
					if (plan.getUser() != null && plan.getUser().getId() != null) {
						activatedUserIds.add(plan.getUser().getId());
					}
				}
			});
		activatedUserIds.forEach(triggerBudgetAllocationUseCase::execute);
	}

	@Transactional
	void reallocateActivePlansForWeekStart(LocalDate today) {
		if (today == null || today.getDayOfWeek() != DayOfWeek.MONDAY) {
			return;
		}

		Set<Integer> activeUserIds = new LinkedHashSet<>();
		planRepository.findAllByStatus(PlanStatus.ACTIVE).forEach(plan -> {
			if (plan.getUser() != null && plan.getUser().getId() != null) {
				activeUserIds.add(plan.getUser().getId());
			}
		});
		activeUserIds.forEach(triggerBudgetAllocationUseCase::execute);
	}
}
