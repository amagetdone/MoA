package com.ssafy.amagetdone.domain.plan.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanListItemResponse;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListPlansUseCaseTest {

	@Mock
	private PlanRepository planRepository;

	@InjectMocks
	private ListPlansUseCase listPlansUseCase;

	@Test
	void returnsListedPlansOrderedByPriorityWithSummaryFields() {
		Plan waitingPlan = instantiatePlan(
			1,
			"iPad purchase",
			320000L,
			1000000L,
			1,
			3,
			LocalDate.now().plusDays(14),
			PlanStatus.WAITING
		);
		Plan activePlan = instantiatePlan(
			2,
			"Travel fund",
			500000L,
			1500000L,
			2,
			5,
			LocalDate.now().plusDays(72),
			PlanStatus.ACTIVE
		);
		Plan completedPlan = instantiatePlan(
			3,
			"Completed fund",
			1_000_000L,
			1_000_000L,
			3,
			4,
			LocalDate.now().minusDays(1),
			PlanStatus.COMPLETED
		);

		given(planRepository.findAllByUserIdAndDeletedFalseAndStatusIn(
			1,
			List.of(PlanStatus.WAITING, PlanStatus.ACTIVE, PlanStatus.COMPLETED)
		))
			.willReturn(List.of(completedPlan, activePlan, waitingPlan));

		List<PlanListItemResponse> response = listPlansUseCase.execute(1);

		assertEquals(3, response.size());
		assertEquals(1L, response.get(0).planId());
		assertEquals("iPad purchase", response.get(0).title());
		assertEquals("WAITING", response.get(0).status());
		assertEquals(14L, response.get(0).dday());
		assertEquals(0, response.get(0).achievementRate().compareTo(new java.math.BigDecimal("32")));
		assertEquals("PENDING", response.get(0).aiPipelineStatus());
		assertEquals(2L, response.get(1).planId());
		assertEquals("ACTIVE", response.get(1).status());
		assertEquals(72L, response.get(1).dday());
		assertEquals(0, response.get(1).achievementRate().compareTo(new java.math.BigDecimal("33.3")));
		assertEquals("FAILED", response.get(1).aiPipelineStatus());
		assertEquals(3L, response.get(2).planId());
		assertEquals("COMPLETED", response.get(2).status());
		assertEquals("FAILED", response.get(2).aiPipelineStatus());
	}

	@Test
	void doesNotRoundIncompleteAchievementRateUpToHundredPercent() {
		Plan nearlyCompletedPlan = instantiatePlan(
			1,
			"Nearly completed plan",
			899_990L,
			900_000L,
			1,
			3,
			LocalDate.now().plusDays(10),
			PlanStatus.WAITING
		);

		given(planRepository.findAllByUserIdAndDeletedFalseAndStatusIn(
			1,
			List.of(PlanStatus.WAITING, PlanStatus.ACTIVE, PlanStatus.COMPLETED)
		))
			.willReturn(List.of(nearlyCompletedPlan));

		List<PlanListItemResponse> response = listPlansUseCase.execute(1);

		assertEquals(0, response.get(0).achievementRate().compareTo(new java.math.BigDecimal("99.9")));
	}

	private Plan instantiatePlan(
		int id,
		String title,
		long savedAmount,
		long targetAmount,
		int priority,
		int targetWeeks,
		LocalDate endDate,
		PlanStatus status
	) {
		Plan plan = Plan.builder()
			.user(User.createPendingSocialUser())
			.title(title)
			.savedAmount(savedAmount)
			.targetAmount(targetAmount)
			.priority(priority)
			.targetWeeks(targetWeeks)
			.endDate(endDate)
			.status(status)
			.aiPipelineStatus(status == PlanStatus.WAITING ? PlanAiPipelineStatus.PENDING : PlanAiPipelineStatus.FAILED)
			.build();
		ReflectionTestUtils.setField(plan, "id", id);
		return plan;
	}
}
