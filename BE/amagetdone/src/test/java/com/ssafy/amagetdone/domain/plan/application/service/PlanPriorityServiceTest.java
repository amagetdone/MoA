package com.ssafy.amagetdone.domain.plan.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import java.util.List;

import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;

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
		given(planRepository.findAllByUserIdAndPriorityGreaterThanEqualOrderByPriorityAsc(1, 2))
			.willReturn(List.of(second, third));

		planPriorityService.shiftPriorities(1, 2);

		assertEquals(3, ReflectionTestUtils.getField(second, "priority"));
		assertEquals(4, ReflectionTestUtils.getField(third, "priority"));
	}

	private Plan planWithPriority(int priority) {
		Plan plan = instantiatePlan();
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
