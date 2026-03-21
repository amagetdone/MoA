package com.ssafy.amagetdone.domain.plan.application.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ssafy.amagetdone.domain.budget.application.service.WeeklyGoalSavingAccrualService;
import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
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
	private final WeeklyGoalSavingAccrualService weeklyGoalSavingAccrualService;
	private final Clock clock;

	public PlanStatusScheduler(
		PlanRepository planRepository,
		TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase,
		WeeklyGoalSavingAccrualService weeklyGoalSavingAccrualService,
		Clock clock
	) {
		this.planRepository = planRepository;
		this.triggerBudgetAllocationUseCase = triggerBudgetAllocationUseCase;
		this.weeklyGoalSavingAccrualService = weeklyGoalSavingAccrualService;
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
		java.util.List<Plan> activatedPlans = new java.util.ArrayList<>();
		planRepository.findAllByStatusAndStartDateLessThanEqualAndAiPipelineStatusAndDeletedFalse(
				PlanStatus.WAITING,
				today,
				PlanAiPipelineStatus.SUCCEEDED
			)
			.forEach(plan -> {
				if (plan.getStatus() == PlanStatus.WAITING) {
					plan.activate();
					activatedPlans.add(plan);
					if (plan.getUser() != null && plan.getUser().getId() != null) {
						activatedUserIds.add(plan.getUser().getId());
					}
				}
			});
		activatedUserIds.forEach(triggerBudgetAllocationUseCase::execute);
		activatedPlans.forEach(plan -> {
			if (plan.getUser() != null && plan.getUser().getId() != null) {
				weeklyGoalSavingAccrualService.applyImmediateAccrual(
					plan.getUser().getId(), plan.getId());
			}
		});
	}

	@Transactional
	void reallocateActivePlansForWeekStart(LocalDate today) {
		if (today == null || today.getDayOfWeek() != DayOfWeek.MONDAY) {
			return;
		}

		Set<Integer> activeUserIds = new LinkedHashSet<>();
		planRepository.findAllByStatusAndDeletedFalse(PlanStatus.ACTIVE).forEach(plan -> {
			if (plan.getUser() != null && plan.getUser().getId() != null) {
				activeUserIds.add(plan.getUser().getId());
			}
		});
		activeUserIds.forEach(triggerBudgetAllocationUseCase::execute);
	}
}
