package com.ssafy.amagetdone.domain.budget.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.AssertionErrors;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DailyGoalSavingAccrualServiceTest {

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Mock
	private BudgetAdjustmentRepository budgetAdjustmentRepository;

	@Mock
	private PlanRepository planRepository;

	private final List<BudgetWeek> budgetWeeks = new ArrayList<>();
	private final List<BudgetAdjustment> savedAdjustments = new ArrayList<>();
	private final Map<Integer, List<PlanWeekAllocation>> allocationsByWeekId = new HashMap<>();

	@BeforeEach
	void setUp() {
		Mockito.lenient().when(budgetWeekRepository.findAll()).thenReturn(budgetWeeks);
		Mockito.lenient().when(planWeekAllocationRepository.findAllByBudgetWeekIn(any())).thenAnswer(invocation -> {
			List<BudgetWeek> weeks = invocation.getArgument(0);
			return weeks.stream()
				.filter(week -> week.getId() != null)
				.flatMap(week -> allocationsByWeekId.getOrDefault(week.getId(), List.of()).stream())
				.toList();
		});
		Mockito.lenient().when(budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(any(BudgetWeek.class)))
			.thenAnswer(invocation -> savedAdjustments.stream()
				.filter(adjustment -> adjustment.getBudgetWeek() == invocation.getArgument(0))
				.toList());
		Mockito.lenient().when(budgetAdjustmentRepository.save(any(BudgetAdjustment.class))).thenAnswer(invocation -> {
			BudgetAdjustment adjustment = invocation.getArgument(0);
			savedAdjustments.add(adjustment);
			return adjustment;
		});
	}

	@Test
	void backfillAccrualAccumulatesFromPlanStartThroughCutoffForAFullWeek() {
		User user = user(1);
		Plan plan = activePlan(user, 11, "Travel Fund", 0L);
		BudgetWeek budgetWeek = budgetWeek(user, 101, 3, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8), 7);
		PlanWeekAllocation allocation = allocation(plan, budgetWeek, 70_000L);
		budgetWeeks.add(budgetWeek);
		allocationsByWeekId.put(101, List.of(allocation));

		DailyGoalSavingAccrualService march5Service = serviceAt("2026-03-05T01:00:00Z");
		GoalSavingAccrualForceDto.Response firstResponse = march5Service.backfillAccrual(
			1,
			new GoalSavingAccrualForceDto.BackfillRequest(null)
		);

		assertThat(plan.getSavedAmount()).isEqualTo(30_000L);
		assertThat(firstResponse.operationType()).isEqualTo(GoalSavingAccrualForceDto.OperationType.BACKFILL);
		assertThat(firstResponse.appliedScope()).isNull();
		assertThat(firstResponse.targetType()).isEqualTo(GoalSavingAccrualForceDto.TargetType.ALL);
		assertThat(firstResponse.cutoffDate()).isEqualTo(LocalDate.of(2026, 3, 4));
		assertThat(firstResponse.appliedAmount()).isEqualTo(30_000L);
		assertThat(firstResponse.periodStartDate()).isEqualTo(LocalDate.of(2026, 3, 2));
		assertThat(firstResponse.periodEndDate()).isEqualTo(LocalDate.of(2026, 3, 4));
		assertThat(firstResponse.plans()).singleElement().satisfies(detail -> {
			assertThat(detail.priority()).isEqualTo(1);
			assertThat(detail.plannedAmount()).isEqualTo(70_000L);
			assertThat(detail.finalAmount()).isEqualTo(70_000L);
			assertThat(detail.accruedAmount()).isEqualTo(30_000L);
			assertThat(detail.savedAmount()).isEqualTo(30_000L);
			assertThat(detail.remainingScheduledAmount()).isEqualTo(40_000L);
		});

		DailyGoalSavingAccrualService march9Service = serviceAt("2026-03-09T01:00:00Z");
		GoalSavingAccrualForceDto.Response secondResponse = march9Service.backfillAccrual(
			1,
			new GoalSavingAccrualForceDto.BackfillRequest(null)
		);

		assertThat(plan.getSavedAmount()).isEqualTo(70_000L);
		assertThat(secondResponse.appliedAmount()).isEqualTo(40_000L);
		assertThat(secondResponse.plans()).singleElement().satisfies(detail -> {
			assertThat(detail.accruedAmount()).isEqualTo(40_000L);
			assertThat(detail.savedAmount()).isEqualTo(70_000L);
			assertThat(detail.remainingScheduledAmount()).isEqualTo(0L);
		});
	}

	@Test
	void backfillAccrualProratesPartialWeekByElapsedDays() {
		User user = user(1);
		Plan plan = activePlan(user, 12, "Camera Fund", 0L);
		BudgetWeek budgetWeek = budgetWeek(user, 102, 4, LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 8), 4);
		PlanWeekAllocation allocation = allocation(plan, budgetWeek, 40_000L);
		budgetWeeks.add(budgetWeek);
		allocationsByWeekId.put(102, List.of(allocation));

		DailyGoalSavingAccrualService service = serviceAt("2026-03-08T01:00:00Z");
		GoalSavingAccrualForceDto.Response response = service.backfillAccrual(
			1,
			new GoalSavingAccrualForceDto.BackfillRequest(null)
		);

		assertThat(plan.getSavedAmount()).isEqualTo(30_000L);
		assertThat(response.plans()).singleElement().satisfies(detail -> {
			assertThat(detail.finalAmount()).isEqualTo(40_000L);
			assertThat(detail.accruedAmount()).isEqualTo(30_000L);
			assertThat(detail.remainingScheduledAmount()).isEqualTo(10_000L);
		});
	}

	@Test
	void applyDailyAccrualIsIdempotentForSameCutoffDate() {
		User user = user(1);
		Plan plan = activePlan(user, 13, "Laptop Fund", 0L);
		BudgetWeek budgetWeek = budgetWeek(user, 103, 5, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8), 7);
		PlanWeekAllocation allocation = allocation(plan, budgetWeek, 70_000L);
		budgetWeeks.add(budgetWeek);
		allocationsByWeekId.put(103, List.of(allocation));

		DailyGoalSavingAccrualService service = serviceAt("2026-03-06T01:00:00Z");

		GoalSavingAccrualForceDto.Response firstResponse = service.applyDailyAccrual(1);
		GoalSavingAccrualForceDto.Response secondResponse = service.applyDailyAccrual(1);

		assertThat(plan.getSavedAmount()).isEqualTo(40_000L);
		assertThat(firstResponse.appliedAmount()).isEqualTo(40_000L);
		assertThat(secondResponse.appliedAmount()).isEqualTo(0L);
		assertThat(savedAdjustments).hasSize(1);
		assertThat(savedAdjustments.get(0).getAdjustmentType()).isEqualTo(BudgetAdjustmentType.PLAN_SAVED_AUTO_ADD);
	}

	@Test
	void backfillAccrualSubtractsWhenWeeklyFinalAmountWasReducedAfterAutoAccrual() {
		User user = user(1);
		Plan plan = activePlan(user, 14, "Trip Fund", 0L);
		BudgetWeek budgetWeek = budgetWeek(user, 104, 6, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8), 7);
		PlanWeekAllocation allocation = allocation(plan, budgetWeek, 70_000L);
		budgetWeeks.add(budgetWeek);
		allocationsByWeekId.put(104, List.of(allocation));

		DailyGoalSavingAccrualService initialService = serviceAt("2026-03-09T01:00:00Z");
		initialService.applyDailyAccrual(1);
		assertThat(plan.getSavedAmount()).isEqualTo(70_000L);

		allocation.setFinalAmount(40_000L);

		DailyGoalSavingAccrualService adjustedService = serviceAt("2026-03-09T01:00:00Z");
		GoalSavingAccrualForceDto.Response response = adjustedService.backfillAccrual(
			1,
			new GoalSavingAccrualForceDto.BackfillRequest(plan.getId())
		);

		assertThat(plan.getSavedAmount()).isEqualTo(40_000L);
		assertThat(response.operationType()).isEqualTo(GoalSavingAccrualForceDto.OperationType.BACKFILL);
		assertThat(response.targetType()).isEqualTo(GoalSavingAccrualForceDto.TargetType.SINGLE);
		assertThat(response.planId()).isEqualTo(plan.getId());
		assertThat(response.appliedAmount()).isEqualTo(-30_000L);
		assertThat(savedAdjustments).extracting(BudgetAdjustment::getAdjustmentType)
			.containsExactly(
				BudgetAdjustmentType.PLAN_SAVED_AUTO_ADD,
				BudgetAdjustmentType.PLAN_SAVED_AUTO_SUBTRACT
			);
	}

	@Test
	void backfillAccrualWithPlanIdTargetsOnlyThatPlan() {
		User user = user(1);
		Plan travelPlan = activePlan(user, 15, "Travel Fund", 0L);
		Plan cameraPlan = activePlan(user, 16, "Camera Fund", 0L);
		BudgetWeek budgetWeek = budgetWeek(user, 105, 7, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22), 7);
		budgetWeeks.add(budgetWeek);
		allocationsByWeekId.put(105, List.of(
			allocation(travelPlan, budgetWeek, 70_000L),
			allocation(cameraPlan, budgetWeek, 35_000L)
		));

		DailyGoalSavingAccrualService service = serviceAt("2026-03-19T01:00:00Z");
		GoalSavingAccrualForceDto.Response response = service.backfillAccrual(
			1,
			new GoalSavingAccrualForceDto.BackfillRequest(15)
		);

		assertThat(response.targetType()).isEqualTo(GoalSavingAccrualForceDto.TargetType.SINGLE);
		assertThat(response.planId()).isEqualTo(15);
		assertThat(response.plans()).hasSize(1);
		assertThat(travelPlan.getSavedAmount()).isEqualTo(30_000L);
		assertThat(cameraPlan.getSavedAmount()).isEqualTo(0L);
	}

	@Test
	void backfillAccrualThrowsWhenPlanDoesNotBelongToUser() {
		User owner = user(2);
		Plan foreignPlan = activePlan(owner, 99, "Foreign Plan", 0L);
		given(planRepository.findByIdAndUserId(99, 1)).willReturn(java.util.Optional.empty());

		DailyGoalSavingAccrualService service = serviceAt("2026-03-19T01:00:00Z");

		assertThat(org.junit.jupiter.api.Assertions.assertThrows(
			PlanException.class,
			() -> service.backfillAccrual(1, new GoalSavingAccrualForceDto.BackfillRequest(foreignPlan.getId()))
		).getErrorCode().name()).isEqualTo("PLAN_NOT_FOUND");
	}

	private DailyGoalSavingAccrualService serviceAt(String instant) {
		return new DailyGoalSavingAccrualService(
			budgetWeekRepository,
			planWeekAllocationRepository,
			budgetAdjustmentRepository,
			planRepository,
			Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Seoul"))
		);
	}

	private User user(int userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}

	private Plan activePlan(User user, int planId, String title, long savedAmount) {
		Plan plan = Plan.builder()
			.user(user)
			.title(title)
			.description(title)
			.targetAmount(1_000_000L)
			.savedAmount(savedAmount)
			.startDate(LocalDate.of(2026, 3, 1))
			.endDate(LocalDate.of(2026, 5, 31))
			.targetWeeks(13)
			.priority(1)
			.status(PlanStatus.ACTIVE)
			.build();
		ReflectionTestUtils.setField(plan, "id", planId);
		return plan;
	}

	private BudgetWeek budgetWeek(
		User user,
		int budgetWeekId,
		int weekSequence,
		LocalDate startDate,
		LocalDate endDate,
		int periodDays
	) {
		BudgetWeek budgetWeek = BudgetWeek.builder()
			.user(user)
			.weekSequence(weekSequence)
			.weekStartDate(startDate)
			.weekEndDate(endDate)
			.periodDays(periodDays)
			.partial(periodDays != 7)
			.build();
		ReflectionTestUtils.setField(budgetWeek, "id", budgetWeekId);
		return budgetWeek;
	}

	private PlanWeekAllocation allocation(Plan plan, BudgetWeek budgetWeek, long finalAmount) {
		return PlanWeekAllocation.builder()
			.plan(plan)
			.budgetWeek(budgetWeek)
			.plannedAmount(finalAmount)
			.finalAmount(finalAmount)
			.build();
	}
}
