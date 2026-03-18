package com.ssafy.amagetdone.domain.plan.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import org.mockito.ArgumentCaptor;
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

	private CreatePlanUseCase createPlanUseCase;

	private void createUseCaseWithClock(Clock clock) {
		createPlanUseCase = new CreatePlanUseCase(
			userRepository,
			planRepository,
			planPriorityService,
			applicationEventPublisher,
			clock
		);
	}

	@Test
	void createsWaitingPlanAndPublishesCreatedEvent() {
		createUseCaseWithClock(Clock.fixed(Instant.parse("2026-03-10T00:00:00Z"), ZoneId.of("Asia/Seoul")));
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 1);

		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(planRepository.save(any(Plan.class))).willAnswer(invocation -> {
			Plan plan = invocation.getArgument(0);
			ReflectionTestUtils.setField(plan, "id", 10);
			return plan;
		});

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

	@Test
	void createsActivePlanWhenStartDateIsToday() {
		createUseCaseWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 1);
		LocalDate today = LocalDate.of(2026, 3, 19);

		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(planRepository.save(any(Plan.class))).willAnswer(invocation -> {
			Plan plan = invocation.getArgument(0);
			ReflectionTestUtils.setField(plan, "id", 11);
			return plan;
		});

		PlanCreateResponse response = createPlanUseCase.execute(1, new PlanCreateRequest(
			"Immediate plan",
			"Starts now",
			100000L,
			today,
			today.plusWeeks(2),
			1
		));

		assertEquals("ACTIVE", response.status());
		ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
		verify(planRepository).save(planCaptor.capture());
		assertEquals(com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus.ACTIVE, planCaptor.getValue().getStatus());
		verify(applicationEventPublisher).publishEvent(new PlanCreatedEvent(11, 1));
	}

	@Test
	void createsActivePlanUsingKstDateEvenWhenSystemDefaultZoneIsUtc() {
		createUseCaseWithClock(Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul")));
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 1);

		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(planRepository.save(any(Plan.class))).willAnswer(invocation -> {
			Plan plan = invocation.getArgument(0);
			ReflectionTestUtils.setField(plan, "id", 12);
			return plan;
		});

		PlanCreateResponse response = createPlanUseCase.execute(1, new PlanCreateRequest(
			"KST plan",
			"KST should be active",
			100000L,
			LocalDate.of(2026, 3, 19),
			LocalDate.of(2026, 4, 1),
			1
		));

		assertEquals("ACTIVE", response.status());
	}
}
