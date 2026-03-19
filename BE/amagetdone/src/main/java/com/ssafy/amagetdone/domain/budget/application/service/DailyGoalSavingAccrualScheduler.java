package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ssafy.amagetdone.domain.budget.application.usecase.ApplyDailyGoalSavingAccrualUseCase;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DailyGoalSavingAccrualScheduler {

	private final PlanRepository planRepository;
	private final ApplyDailyGoalSavingAccrualUseCase applyDailyGoalSavingAccrualUseCase;
	private final Clock clock;

	public DailyGoalSavingAccrualScheduler(
		PlanRepository planRepository,
		ApplyDailyGoalSavingAccrualUseCase applyDailyGoalSavingAccrualUseCase,
		Clock clock
	) {
		this.planRepository = planRepository;
		this.applyDailyGoalSavingAccrualUseCase = applyDailyGoalSavingAccrualUseCase;
		this.clock = clock;
	}

	@Scheduled(cron = "0 15 0 * * *", zone = "Asia/Seoul")
	@Transactional
	public void applyDailyGoalSavingAccrual() {
		applyDailyGoalSavingAccrual(LocalDate.now(clock));
	}

	@Transactional
	void applyDailyGoalSavingAccrual(LocalDate today) {
		if (today == null) {
			return;
		}

		Set<Integer> activeUserIds = new LinkedHashSet<>();
		planRepository.findAllByStatus(PlanStatus.ACTIVE).forEach(plan -> {
			if (plan.getUser() != null && plan.getUser().getId() != null) {
				activeUserIds.add(plan.getUser().getId());
			}
		});
		activeUserIds.forEach(applyDailyGoalSavingAccrualUseCase::execute);
	}
}
