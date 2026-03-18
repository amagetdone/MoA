package com.ssafy.amagetdone.domain.plan.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlanStatusSchedulerTest {

	@Mock
	private PlanRepository planRepository;

	@InjectMocks
	private PlanStatusScheduler planStatusScheduler;

	@Test
	void activatesWaitingPlansWhoseStartDateHasArrived() {
		LocalDate today = LocalDate.of(2026, 3, 18);
		Plan waitingPlan = planWithStatuses(PlanStatus.WAITING, PlanAiPipelineStatus.SUCCEEDED);
		Plan pendingAiPlan = planWithStatuses(PlanStatus.WAITING, PlanAiPipelineStatus.PENDING);

		given(planRepository.findAllByStatusAndStartDateLessThanEqualAndAiPipelineStatus(
			PlanStatus.WAITING,
			today,
			PlanAiPipelineStatus.SUCCEEDED
		)).willReturn(List.of(waitingPlan));

		planStatusScheduler.activateStartedPlans(today);

		assertEquals(PlanStatus.ACTIVE, waitingPlan.getStatus());
		assertEquals(PlanStatus.WAITING, pendingAiPlan.getStatus());
	}

	@Test
	void activatesWaitingPlansOnApplicationReady() {
		LocalDate today = LocalDate.now();
		Plan waitingPlan = planWithStatuses(PlanStatus.WAITING, PlanAiPipelineStatus.SUCCEEDED);

		given(planRepository.findAllByStatusAndStartDateLessThanEqualAndAiPipelineStatus(
			PlanStatus.WAITING,
			today,
			PlanAiPipelineStatus.SUCCEEDED
		)).willReturn(List.of(waitingPlan));

		planStatusScheduler.activateStartedPlansOnStartup();

		assertEquals(PlanStatus.ACTIVE, waitingPlan.getStatus());
	}

	private Plan planWithStatuses(PlanStatus status, PlanAiPipelineStatus aiPipelineStatus) {
		Plan plan = instantiatePlan();
		ReflectionTestUtils.setField(plan, "status", status);
		ReflectionTestUtils.setField(plan, "aiPipelineStatus", aiPipelineStatus);
		return plan;
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
