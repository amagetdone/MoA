package com.ssafy.amagetdone.domain.plan.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.application.service.PlanPriorityService;
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
class DeletePlanUseCaseTest {

	@Mock
	private PlanRepository planRepository;

	@Mock
	private PlanPriorityService planPriorityService;

	@InjectMocks
	private DeletePlanUseCase deletePlanUseCase;

	@Test
	void deletesPlanSoftlyCancelsItAndCompactsRemainingPriorities() {
		Plan deletedPlan = plan(10, 1, 2, PlanStatus.ACTIVE);

		given(planRepository.findByIdAndUserIdAndDeletedFalse(10, 1)).willReturn(Optional.of(deletedPlan));

		deletePlanUseCase.execute(1, 10);

		assertTrue(deletedPlan.isDeleted());
		assertEquals(PlanStatus.CANCELLED, deletedPlan.getStatus());
		verify(planPriorityService).compactPrioritiesAfterDeletion(1, 2);
	}

	@Test
	void keepsExistingPlanAllocationsAndBudgetAmountsUntouched() {
		Plan deletedPlan = plan(10, 1, 1, PlanStatus.ACTIVE);

		given(planRepository.findByIdAndUserIdAndDeletedFalse(10, 1)).willReturn(Optional.of(deletedPlan));

		deletePlanUseCase.execute(1, 10);

		assertTrue(deletedPlan.isDeleted());
		assertEquals(PlanStatus.CANCELLED, deletedPlan.getStatus());
		verify(planPriorityService).compactPrioritiesAfterDeletion(1, 1);
	}

	private Plan plan(int id, int userId, int priority, PlanStatus status) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);

		Plan plan = instantiatePlan();
		ReflectionTestUtils.setField(plan, "id", id);
		ReflectionTestUtils.setField(plan, "user", user);
		ReflectionTestUtils.setField(plan, "priority", priority);
		ReflectionTestUtils.setField(plan, "status", status);
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
