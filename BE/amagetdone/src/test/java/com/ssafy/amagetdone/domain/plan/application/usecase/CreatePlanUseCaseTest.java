package com.ssafy.amagetdone.domain.plan.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateRequest;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateResponse;
import com.ssafy.amagetdone.domain.plan.application.event.PlanCreatedEvent;
import com.ssafy.amagetdone.domain.plan.application.service.PlanCreationBudgetSummaryService;
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
	private TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase;

	@Mock
	private PlanCreationBudgetSummaryService planCreationBudgetSummaryService;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	private CreatePlanUseCase createPlanUseCase;
	private Clock clock;

	private void createUseCase() {
		if (clock == null) {
			clock = Clock.fixed(Instant.parse("2026-03-19T00:00:00Z"), ZoneId.of("Asia/Seoul"));
		}
		createPlanUseCase = new CreatePlanUseCase(
			userRepository,
			planRepository,
			planPriorityService,
			triggerBudgetAllocationUseCase,
			planCreationBudgetSummaryService,
			applicationEventPublisher,
			clock
		);
	}

	@Test
	void createsWaitingPlanAndPublishesCreatedEvent() {
		createUseCase();
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 1);

		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(planRepository.save(any(Plan.class))).willAnswer(invocation -> {
			Plan plan = invocation.getArgument(0);
			ReflectionTestUtils.setField(plan, "id", 10);
			return plan;
		});
		given(planCreationBudgetSummaryService.build(any(Plan.class)))
			.willReturn(summaryResponse(10, "Europe trip fund", 150_000L, 22, "WAITING"));

		PlanCreateResponse response = createPlanUseCase.execute(1, new PlanCreateRequest(
			"Europe trip fund",
			"Summer travel savings",
			3000000L,
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 9, 1),
			1
		));

		assertEquals(10, response.planId());
		assertEquals("Europe trip fund", response.planTitle());
		assertEquals(150_000L, response.weeklyMission().weeklySavingsAmount());
		verify(planPriorityService).shiftPriorities(1, 1);
		ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
		verify(planRepository).save(planCaptor.capture());
		assertTrue(planCaptor.getValue().getTargetWeeks() > 0);
		verify(planWeekAllocationRepository, never()).saveAll(any());
		verify(triggerBudgetAllocationUseCase).execute(1);
		verify(planCreationBudgetSummaryService).build(planCaptor.getValue());
		verify(applicationEventPublisher).publishEvent(new PlanCreatedEvent(10, 1));
	}

	@Test
	void createsActivePlanWhenStartDateIsToday() {
		createUseCase();
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 1);
		LocalDate today = LocalDate.of(2026, 3, 19);

		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(planRepository.save(any(Plan.class))).willAnswer(invocation -> {
			Plan plan = invocation.getArgument(0);
			ReflectionTestUtils.setField(plan, "id", 11);
			return plan;
		});
		given(planCreationBudgetSummaryService.build(any(Plan.class)))
			.willReturn(summaryResponse(11, "Immediate plan", 100_000L, 3, "ACTIVE"));

		PlanCreateResponse response = createPlanUseCase.execute(1, new PlanCreateRequest(
			"Immediate plan",
			"Starts now",
			100000L,
			today,
			today.plusWeeks(2),
			1
		));

		assertEquals(11, response.planId());
		assertEquals(100_000L, response.weeklyMission().weeklySavingsAmount());
		assertEquals("ACTIVE", response.status());
		ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
		verify(planRepository).save(planCaptor.capture());
		assertEquals(com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus.ACTIVE, planCaptor.getValue().getStatus());
		verify(triggerBudgetAllocationUseCase).execute(1);
		verify(applicationEventPublisher).publishEvent(new PlanCreatedEvent(11, 1));
	}

	@Test
	void createsActivePlanUsingKstDateEvenWhenSystemDefaultZoneIsUtc() {
		clock = Clock.fixed(Instant.parse("2026-03-18T15:30:00Z"), ZoneId.of("Asia/Seoul"));
		createUseCase();
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 1);

		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(planRepository.save(any(Plan.class))).willAnswer(invocation -> {
			Plan plan = invocation.getArgument(0);
			ReflectionTestUtils.setField(plan, "id", 12);
			return plan;
		});
		given(planCreationBudgetSummaryService.build(any(Plan.class)))
			.willReturn(summaryResponse(12, "KST plan", 100_000L, 2, "ACTIVE"));

		PlanCreateResponse response = createPlanUseCase.execute(1, new PlanCreateRequest(
			"KST plan",
			"KST should be active",
			100000L,
			LocalDate.of(2026, 3, 19),
			LocalDate.of(2026, 4, 1),
			1
		));

		assertEquals("ACTIVE", response.status());
		assertEquals(12, response.planId());
		assertEquals(100_000L, response.weeklyMission().weeklySavingsAmount());
		verify(triggerBudgetAllocationUseCase).execute(1);
	}

	@Test
	void keepsPlanWaitingWhenStartDateIsInFuture() {
		createUseCase();
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 1);

		given(userRepository.findById(1)).willReturn(Optional.of(user));
		given(planRepository.save(any(Plan.class))).willAnswer(invocation -> {
			Plan plan = invocation.getArgument(0);
			ReflectionTestUtils.setField(plan, "id", 13);
			return plan;
		});
		given(planCreationBudgetSummaryService.build(any(Plan.class))).willReturn(summaryResponse(13, "Future plan", 100_000L, 2, "WAITING"));

		PlanCreateResponse response = createPlanUseCase.execute(1, new PlanCreateRequest(
			"Future plan",
			"Starts later",
			100000L,
			LocalDate.of(2026, 3, 26),
			LocalDate.of(2026, 4, 8),
			1
		));

		assertEquals("WAITING", response.status());
		ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
		verify(planRepository).save(planCaptor.capture());
		assertEquals(com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus.WAITING, planCaptor.getValue().getStatus());
		verify(triggerBudgetAllocationUseCase).execute(1);
	}

	private PlanCreateResponse summaryResponse(
		int planId,
		String planTitle,
		long weeklySavingsAmount,
		int weeksToGoal,
		String status
	) {
		return new PlanCreateResponse(
			planId,
			planTitle,
			new PlanCreateResponse.WeeklyMission(weeklySavingsAmount, weeksToGoal),
			new PlanCreateResponse.WeeklyBudget(
				1_500_000L,
				300_000L,
				new PlanCreateResponse.BudgetBucket(
					700_000L,
					java.util.List.of(
						new PlanCreateResponse.BudgetItem("주거비", 350_000L),
						new PlanCreateResponse.BudgetItem("통신비", 200_000L),
						new PlanCreateResponse.BudgetItem("구독비", 150_000L)
					)
				),
				new PlanCreateResponse.BudgetBucket(
					300_000L,
					java.util.List.of(
						new PlanCreateResponse.BudgetItem("식비", 180_000L),
						new PlanCreateResponse.BudgetItem("여가비", 70_000L),
						new PlanCreateResponse.BudgetItem("생활비", 50_000L)
					)
				),
				200_000L
			),
			status
		);
	}
}
