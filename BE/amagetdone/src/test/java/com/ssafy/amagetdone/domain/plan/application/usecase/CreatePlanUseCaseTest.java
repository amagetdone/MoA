package com.ssafy.amagetdone.domain.plan.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateRequest;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateResponse;
import com.ssafy.amagetdone.domain.plan.application.event.PlanCreatedEvent;
import com.ssafy.amagetdone.domain.plan.application.service.PlanPriorityService;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CreatePlanUseCaseTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PlanRepository planRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Mock
	private PlanPriorityService planPriorityService;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	@InjectMocks
	private CreatePlanUseCase createPlanUseCase;

	@Test
	void createsWaitingPlanAndPublishesCreatedEvent() {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 1);
		Plan savedPlan = instantiatePlan();
		ReflectionTestUtils.setField(savedPlan, "id", 10);
		ReflectionTestUtils.setField(savedPlan, "title", "Europe trip fund");
		ReflectionTestUtils.setField(savedPlan, "description", "Summer travel savings");
		ReflectionTestUtils.setField(savedPlan, "targetAmount", 3000000L);
		ReflectionTestUtils.setField(savedPlan, "startDate", LocalDate.of(2026, 4, 1));
		ReflectionTestUtils.setField(savedPlan, "endDate", LocalDate.of(2026, 9, 1));
		ReflectionTestUtils.setField(savedPlan, "priority", 1);
		ReflectionTestUtils.setField(savedPlan, "status", com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus.WAITING);

		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(planRepository.save(any(Plan.class))).willReturn(savedPlan);

		PlanCreateResponse response = createPlanUseCase.execute(1, new PlanCreateRequest(
			"Europe trip fund",
			"Summer travel savings",
			3000000L,
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 9, 1),
			1
		));

		assertEquals(10, response.planId());
		assertEquals("WAITING", response.status());
		verify(planPriorityService).shiftPriorities(1, 1);
		verify(planWeekAllocationRepository, never()).saveAll(any());
		verify(applicationEventPublisher).publishEvent(new PlanCreatedEvent(10, 1));
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
