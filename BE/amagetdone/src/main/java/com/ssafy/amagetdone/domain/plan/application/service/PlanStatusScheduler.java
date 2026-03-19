package com.ssafy.amagetdone.domain.plan.application.service;

import java.time.Clock;
import java.time.LocalDate;

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
	private final Clock clock;

	public PlanStatusScheduler(PlanRepository planRepository, Clock clock) {
		this.planRepository = planRepository;
		this.clock = clock;
	}

	@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
	@Transactional
	public void activateStartedPlans() {
		activateStartedPlans(LocalDate.now(clock));
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void activateStartedPlansOnStartup() {
		activateStartedPlans(LocalDate.now(clock));
	}

	@Transactional
	void activateStartedPlans(LocalDate today) {
		planRepository.findAllByStatusAndStartDateLessThanEqualAndAiPipelineStatus(
				PlanStatus.WAITING,
				today,
				PlanAiPipelineStatus.SUCCEEDED
			)
			.forEach(plan -> {
				if (plan.getStatus() == PlanStatus.WAITING) {
					plan.activate();
				}
			});
	}
}
