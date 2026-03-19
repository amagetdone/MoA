package com.ssafy.amagetdone.domain.budget.application.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.application.usecase.ApplyDailyGoalSavingAccrualUseCase;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DailyGoalSavingAccrualSchedulerTest {

	@Mock
	private PlanRepository planRepository;

	@Mock
	private ApplyDailyGoalSavingAccrualUseCase applyDailyGoalSavingAccrualUseCase;

	@Test
	void appliesDailyAccrualForActivePlanOwners() {
		DailyGoalSavingAccrualScheduler scheduler = new DailyGoalSavingAccrualScheduler(
			planRepository,
			applyDailyGoalSavingAccrualUseCase,
			Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul"))
		);
		given(planRepository.findAllByStatus(PlanStatus.ACTIVE)).willReturn(List.of(
			activePlan(1, 10),
			activePlan(1, 11),
			activePlan(2, 12)
		));

		scheduler.applyDailyGoalSavingAccrual(LocalDate.of(2026, 3, 19));

		verify(applyDailyGoalSavingAccrualUseCase).execute(1);
		verify(applyDailyGoalSavingAccrualUseCase).execute(2);
	}

	@Test
	void doesNothingWhenThereAreNoActivePlans() {
		DailyGoalSavingAccrualScheduler scheduler = new DailyGoalSavingAccrualScheduler(
			planRepository,
			applyDailyGoalSavingAccrualUseCase,
			Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul"))
		);
		given(planRepository.findAllByStatus(PlanStatus.ACTIVE)).willReturn(List.of());

		scheduler.applyDailyGoalSavingAccrual(LocalDate.of(2026, 3, 19));

		verify(applyDailyGoalSavingAccrualUseCase, never()).execute(org.mockito.ArgumentMatchers.anyInt());
	}

	private Plan activePlan(Integer userId, Integer planId) {
		Plan plan = Plan.create(user(userId), "Trip", "desc", 300_000L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 30), 1);
		ReflectionTestUtils.setField(plan, "id", planId);
		plan.activate();
		return plan;
	}

	private User user(Integer userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}
}
