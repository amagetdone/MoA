package com.ssafy.amagetdone.domain.plan.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.time.ZoneId;

import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;
import com.ssafy.amagetdone.domain.budget.application.service.WeeklyGoalSavingAccrualService;
import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlanStatusSchedulerTest {

	@Mock
	private PlanRepository planRepository;

	@Mock
	private TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase;

	@Mock
	private WeeklyGoalSavingAccrualService weeklyGoalSavingAccrualService;

	private PlanStatusScheduler planStatusScheduler;

	private void createSchedulerWithClock(Clock clock) {
		planStatusScheduler = new PlanStatusScheduler(planRepository, triggerBudgetAllocationUseCase, weeklyGoalSavingAccrualService, clock);
	}

	@Test
	void activatesWaitingPlansWhoseStartDateHasArrived() {
		createSchedulerWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		LocalDate today = LocalDate.of(2026, 3, 18);
		Plan waitingPlan = planWithStatuses(1, PlanStatus.WAITING, PlanAiPipelineStatus.SUCCEEDED);
		Plan sameUserWaitingPlan = planWithStatuses(1, PlanStatus.WAITING, PlanAiPipelineStatus.SUCCEEDED);
		Plan pendingAiPlan = planWithStatuses(2, PlanStatus.WAITING, PlanAiPipelineStatus.PENDING);

		given(planRepository.findAllByStatusAndStartDateLessThanEqualAndAiPipelineStatusAndDeletedFalse(
			PlanStatus.WAITING,
			today,
			PlanAiPipelineStatus.SUCCEEDED
		)).willReturn(List.of(waitingPlan, sameUserWaitingPlan));

		planStatusScheduler.activateStartedPlans(today);

		assertEquals(PlanStatus.ACTIVE, waitingPlan.getStatus());
		assertEquals(PlanStatus.ACTIVE, sameUserWaitingPlan.getStatus());
		assertEquals(PlanStatus.WAITING, pendingAiPlan.getStatus());
		verify(triggerBudgetAllocationUseCase).execute(1);
	}

	@Test
	void activatesWaitingPlansOnApplicationReady() {
		createSchedulerWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		LocalDate today = LocalDate.of(2026, 3, 19);
		Plan waitingPlan = planWithStatuses(3, PlanStatus.WAITING, PlanAiPipelineStatus.SUCCEEDED);

		given(planRepository.findAllByStatusAndStartDateLessThanEqualAndAiPipelineStatusAndDeletedFalse(
			PlanStatus.WAITING,
			today,
			PlanAiPipelineStatus.SUCCEEDED
		)).willReturn(List.of(waitingPlan));

		planStatusScheduler.activateStartedPlansOnStartup();

		assertEquals(PlanStatus.ACTIVE, waitingPlan.getStatus());
		verify(triggerBudgetAllocationUseCase).execute(3);
	}

	@Test
	void retriggersAllocationForActivePlanOwnersAtWeekStart() {
		createSchedulerWithClock(Clock.fixed(Instant.parse("2026-03-22T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		Plan firstUserPlan = planWithStatuses(1, PlanStatus.ACTIVE, PlanAiPipelineStatus.SUCCEEDED);
		Plan sameUserPlan = planWithStatuses(1, PlanStatus.ACTIVE, PlanAiPipelineStatus.SUCCEEDED);
		Plan secondUserPlan = planWithStatuses(2, PlanStatus.ACTIVE, PlanAiPipelineStatus.SUCCEEDED);

		given(planRepository.findAllByStatusAndDeletedFalse(PlanStatus.ACTIVE))
			.willReturn(List.of(firstUserPlan, sameUserPlan, secondUserPlan));

		planStatusScheduler.reallocateActivePlansForWeekStart(LocalDate.of(2026, 3, 23));

		verify(triggerBudgetAllocationUseCase).execute(1);
		verify(triggerBudgetAllocationUseCase).execute(2);
	}

	@Test
	void doesNotTriggerAllocationWhenNothingWasActivated() {
		createSchedulerWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));

		given(planRepository.findAllByStatusAndStartDateLessThanEqualAndAiPipelineStatusAndDeletedFalse(
			PlanStatus.WAITING,
			LocalDate.of(2026, 3, 19),
			PlanAiPipelineStatus.SUCCEEDED
		)).willReturn(List.of());

		planStatusScheduler.activateStartedPlans(LocalDate.of(2026, 3, 19));

		verify(triggerBudgetAllocationUseCase, never()).execute(org.mockito.ArgumentMatchers.anyInt());
	}

	private Plan planWithStatuses(Integer userId, PlanStatus status, PlanAiPipelineStatus aiPipelineStatus) {
		Plan plan = instantiatePlan();
		ReflectionTestUtils.setField(plan, "status", status);
		ReflectionTestUtils.setField(plan, "aiPipelineStatus", aiPipelineStatus);
		ReflectionTestUtils.setField(plan, "user", user(userId));
		return plan;
	}

	private User user(Integer userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}

	private Plan instantiatePlan() {
		try {
			var constructor = Plan.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
