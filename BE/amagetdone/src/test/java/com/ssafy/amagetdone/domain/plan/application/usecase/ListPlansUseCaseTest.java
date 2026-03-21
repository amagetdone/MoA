package com.ssafy.amagetdone.domain.plan.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanListItemResponse;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
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

		given(planRepository.findAllByUserIdAndDeletedFalseAndStatusIn(
			1,
			List.of(PlanStatus.WAITING, PlanStatus.ACTIVE, PlanStatus.PAUSED)
		))
			.willReturn(List.of(activePlan, waitingPlan));

		List<PlanListItemResponse> response = listPlansUseCase.execute(1);

		assertEquals(2, response.size());
		assertEquals(1L, response.get(0).planId());
		assertEquals("iPad purchase", response.get(0).title());
		assertEquals(14L, response.get(0).dday());
		assertEquals(0, response.get(0).achievementRate().compareTo(new java.math.BigDecimal("32")));
		assertEquals(2L, response.get(1).planId());
		assertEquals(72L, response.get(1).dday());
		assertEquals(0, response.get(1).achievementRate().compareTo(new java.math.BigDecimal("33.3")));
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
			.build();
		ReflectionTestUtils.setField(plan, "id", id);
		return plan;
	}
}
