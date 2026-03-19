package com.ssafy.amagetdone.domain.plan.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;

import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
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
class PlanPriorityServiceTest {

	@Mock
	private PlanRepository planRepository;

	@InjectMocks
	private PlanPriorityService planPriorityService;

	@Test
	void shiftsExistingPlansBackWhenPriorityConflicts() {
		Plan second = planWithPriority(2);
		Plan third = planWithPriority(3);
		given(planRepository.findAllByUserIdAndDeletedFalseAndStatusInAndPriorityGreaterThanEqualOrderByPriorityAsc(
			1,
			List.of(PlanStatus.WAITING, PlanStatus.ACTIVE, PlanStatus.PAUSED),
			2
		))
			.willReturn(List.of(second, third));

		planPriorityService.shiftPriorities(1, 2);

		assertEquals(3, ReflectionTestUtils.getField(second, "priority"));
		assertEquals(4, ReflectionTestUtils.getField(third, "priority"));
	}

	@Test
	void compactsRemainingPrioritiesAfterDeletion() {
		Plan third = planWithPriority(3);
		Plan fourth = planWithPriority(4);
		given(planRepository.findAllByUserIdAndDeletedFalseAndStatusInAndPriorityGreaterThanOrderByPriorityAsc(
			1,
			List.of(PlanStatus.WAITING, PlanStatus.ACTIVE, PlanStatus.PAUSED),
			2
		))
			.willReturn(List.of(third, fourth));

		planPriorityService.compactPrioritiesAfterDeletion(1, 2);

		assertEquals(2, ReflectionTestUtils.getField(third, "priority"));
		assertEquals(3, ReflectionTestUtils.getField(fourth, "priority"));
	}

	@Test
	void updatesAllPlanPrioritiesWhenRequestContainsFullUserPlanSet() {
		Plan first = planWithIdAndPriority(101, 1);
		Plan second = planWithIdAndPriority(102, 2);
		Plan third = planWithIdAndPriority(103, 3);
		given(planRepository.findAllByUser_IdAndStatusAndDeletedFalse(1, PlanStatus.ACTIVE))
			.willReturn(new ArrayList<>(List.of(first, second, third)));

		planPriorityService.updatePriorities(
			1,
			List.of(
				new PlanPriorityService.PriorityUpdateCommand(103, 1),
				new PlanPriorityService.PriorityUpdateCommand(101, 2),
				new PlanPriorityService.PriorityUpdateCommand(102, 3)
			)
		);

		assertEquals(2, ReflectionTestUtils.getField(first, "priority"));
		assertEquals(3, ReflectionTestUtils.getField(second, "priority"));
		assertEquals(1, ReflectionTestUtils.getField(third, "priority"));
	}

	@Test
	void throwsWhenRequestDoesNotContainFullUserPlanSet() {
		Plan first = planWithIdAndPriority(101, 1);
		Plan second = planWithIdAndPriority(102, 2);
		given(planRepository.findAllByUser_IdAndStatusAndDeletedFalse(1, PlanStatus.ACTIVE))
			.willReturn(List.of(first, second));

		PlanException exception = assertThrows(
			PlanException.class,
			() -> planPriorityService.updatePriorities(
				1,
				List.of(new PlanPriorityService.PriorityUpdateCommand(101, 1))
			)
		);

		assertEquals(PlanErrorCode.INVALID_PRIORITY_UPDATE_REQUEST, exception.getErrorCode());
	}

	@Test
	void throwsWhenPrioritiesAreNotContiguous() {
		Plan first = planWithIdAndPriority(101, 1);
		Plan second = planWithIdAndPriority(102, 2);
		given(planRepository.findAllByUser_IdAndStatusAndDeletedFalse(1, PlanStatus.ACTIVE))
			.willReturn(List.of(first, second));

		PlanException exception = assertThrows(
			PlanException.class,
			() -> planPriorityService.updatePriorities(
				1,
				List.of(
					new PlanPriorityService.PriorityUpdateCommand(101, 1),
					new PlanPriorityService.PriorityUpdateCommand(102, 3)
				)
			)
		);

		assertEquals(PlanErrorCode.INVALID_PRIORITY_UPDATE_REQUEST, exception.getErrorCode());
	}

	@Test
	void ignoresInactivePlansWhenUpdatingPriorities() {
		Plan activeFirst = planWithIdAndPriority(101, 1);
		Plan activeSecond = planWithIdAndPriority(102, 2);
		Plan waitingPlan = planWithIdAndPriority(999, 9);
		given(planRepository.findAllByUser_IdAndStatusAndDeletedFalse(1, PlanStatus.ACTIVE))
			.willReturn(List.of(activeFirst, activeSecond));

		planPriorityService.updatePriorities(
			1,
			List.of(
				new PlanPriorityService.PriorityUpdateCommand(102, 1),
				new PlanPriorityService.PriorityUpdateCommand(101, 2)
			)
		);

		assertEquals(2, ReflectionTestUtils.getField(activeFirst, "priority"));
		assertEquals(1, ReflectionTestUtils.getField(activeSecond, "priority"));
		assertEquals(9, ReflectionTestUtils.getField(waitingPlan, "priority"));
	}

	private Plan planWithPriority(int priority) {
		Plan plan = instantiatePlan();
		ReflectionTestUtils.setField(plan, "priority", priority);
		return plan;
	}

	private Plan planWithIdAndPriority(int id, int priority) {
		Plan plan = instantiatePlan();
		ReflectionTestUtils.setField(plan, "id", id);
		ReflectionTestUtils.setField(plan, "priority", priority);
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
