package com.ssafy.amagetdone.domain.budget.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GoalSavingAccrualApplyTestServiceTest {

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
	void applyAddsOneDaySliceOnly() {
		User user = user(1);
		Plan plan = activePlan(user, 21, "Travel Fund", 0L, 1);
		BudgetWeek budgetWeek = budgetWeek(user, 201, 3, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22), 7);
		allocationsByWeekId.put(201, List.of(allocation(plan, budgetWeek, 70_000L)));
		budgetWeeks.add(budgetWeek);

		GoalSavingAccrualApplyTestService service = serviceAt("2026-03-19T01:00:00Z");
		GoalSavingAccrualForceDto.Response response = service.apply(
			1,
			new GoalSavingAccrualForceDto.ApplyRequest(null, GoalSavingAccrualForceDto.Scope.DAY)
		);

		assertThat(response.operationType()).isEqualTo(GoalSavingAccrualForceDto.OperationType.APPLY);
		assertThat(response.appliedScope()).isEqualTo(GoalSavingAccrualForceDto.Scope.DAY);
		assertThat(response.appliedAmount()).isEqualTo(10_000L);
		assertThat(plan.getSavedAmount()).isEqualTo(10_000L);
		assertThat(response.plans()).singleElement().satisfies(detail -> assertThat(detail.accruedAmount()).isEqualTo(10_000L));
		assertThat(savedAdjustments).singleElement().satisfies(adjustment -> {
			assertThat(adjustment.getAdjustmentType()).isEqualTo(BudgetAdjustmentType.PLAN_SAVED_ADD);
		});
	}

	@Test
	void applyWeekAddsCurrentWeekFinalAmountImmediately() {
		User user = user(1);
		Plan plan = activePlan(user, 22, "Camera Fund", 5_000L, 1);
		BudgetWeek budgetWeek = budgetWeek(user, 202, 3, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22), 7);
		allocationsByWeekId.put(202, List.of(allocation(plan, budgetWeek, 38_095L)));
		budgetWeeks.add(budgetWeek);

		GoalSavingAccrualApplyTestService service = serviceAt("2026-03-19T01:00:00Z");
		GoalSavingAccrualForceDto.Response response = service.apply(
			1,
			new GoalSavingAccrualForceDto.ApplyRequest(null, GoalSavingAccrualForceDto.Scope.WEEK)
		);

		assertThat(response.appliedAmount()).isEqualTo(38_095L);
		assertThat(plan.getSavedAmount()).isEqualTo(43_095L);
		assertThat(response.periodStartDate()).isEqualTo(LocalDate.of(2026, 3, 16));
		assertThat(response.periodEndDate()).isEqualTo(LocalDate.of(2026, 3, 22));
		assertThat(response.plans()).singleElement().satisfies(detail -> {
			assertThat(detail.finalAmount()).isEqualTo(38_095L);
			assertThat(detail.accruedAmount()).isEqualTo(38_095L);
		});
	}

	@Test
	void applyMonthAddsCurrentMonthTotalIncludingPartialMonthOverlap() {
		User user = user(1);
		Plan plan = activePlan(user, 23, "Month Fund", 0L, 1);
		BudgetWeek marchStart = budgetWeek(user, 203, 1, LocalDate.of(2026, 2, 24), LocalDate.of(2026, 3, 2), 7);
		BudgetWeek middle = budgetWeek(user, 204, 2, LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 9), 7);
		allocationsByWeekId.put(203, List.of(allocation(plan, marchStart, 70_000L)));
		allocationsByWeekId.put(204, List.of(allocation(plan, middle, 35_000L)));
		budgetWeeks.add(marchStart);
		budgetWeeks.add(middle);

		GoalSavingAccrualApplyTestService service = serviceAt("2026-03-19T01:00:00Z");
		GoalSavingAccrualForceDto.Response response = service.apply(
			1,
			new GoalSavingAccrualForceDto.ApplyRequest(null, GoalSavingAccrualForceDto.Scope.MONTH)
		);

		assertThat(response.appliedAmount()).isEqualTo(55_000L);
		assertThat(plan.getSavedAmount()).isEqualTo(55_000L);
		assertThat(response.periodLabel()).isEqualTo("2026-03");
		assertThat(response.plans()).singleElement().satisfies(detail -> {
			assertThat(detail.finalAmount()).isEqualTo(55_000L);
			assertThat(detail.accruedAmount()).isEqualTo(55_000L);
		});
	}

	@Test
	void applyWithPlanIdTargetsOnlyThatPlan() {
		User user = user(1);
		Plan travel = activePlan(user, 24, "Travel", 0L, 1);
		Plan camera = activePlan(user, 25, "Camera", 0L, 2);
		BudgetWeek budgetWeek = budgetWeek(user, 205, 3, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22), 7);
		allocationsByWeekId.put(205, List.of(allocation(travel, budgetWeek, 20_000L), allocation(camera, budgetWeek, 30_000L)));
		budgetWeeks.add(budgetWeek);
		given(planRepository.findByIdAndUserId(24, 1)).willReturn(Optional.of(travel));

		GoalSavingAccrualApplyTestService service = serviceAt("2026-03-19T01:00:00Z");
		GoalSavingAccrualForceDto.Response response = service.apply(
			1,
			new GoalSavingAccrualForceDto.ApplyRequest(24, GoalSavingAccrualForceDto.Scope.WEEK)
		);

		assertThat(response.targetType()).isEqualTo(GoalSavingAccrualForceDto.TargetType.SINGLE);
		assertThat(response.planId()).isEqualTo(24);
		assertThat(response.plans()).hasSize(1);
		assertThat(travel.getSavedAmount()).isEqualTo(20_000L);
		assertThat(camera.getSavedAmount()).isEqualTo(0L);
	}

	@Test
	void applyWithoutPlanIdTargetsAllActivePlans() {
		User user = user(1);
		Plan travel = activePlan(user, 26, "Travel", 0L, 1);
		Plan camera = activePlan(user, 27, "Camera", 0L, 2);
		BudgetWeek budgetWeek = budgetWeek(user, 206, 3, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22), 7);
		allocationsByWeekId.put(206, List.of(allocation(travel, budgetWeek, 20_000L), allocation(camera, budgetWeek, 30_000L)));
		budgetWeeks.add(budgetWeek);

		GoalSavingAccrualApplyTestService service = serviceAt("2026-03-19T01:00:00Z");
		GoalSavingAccrualForceDto.Response response = service.apply(
			1,
			new GoalSavingAccrualForceDto.ApplyRequest(null, GoalSavingAccrualForceDto.Scope.WEEK)
		);

		assertThat(response.targetType()).isEqualTo(GoalSavingAccrualForceDto.TargetType.ALL);
		assertThat(response.plans()).hasSize(2);
		assertThat(response.appliedAmount()).isEqualTo(50_000L);
	}

	@Test
	void applyIsNoOpWhenSamePlanScopeAndPeriodAreRepeated() {
		User user = user(1);
		Plan plan = activePlan(user, 28, "Repeat", 0L, 1);
		BudgetWeek budgetWeek = budgetWeek(user, 207, 3, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22), 7);
		allocationsByWeekId.put(207, List.of(allocation(plan, budgetWeek, 42_000L)));
		budgetWeeks.add(budgetWeek);
		given(planRepository.findByIdAndUserId(28, 1)).willReturn(Optional.of(plan));

		GoalSavingAccrualApplyTestService service = serviceAt("2026-03-19T01:00:00Z");
		GoalSavingAccrualForceDto.Response first = service.apply(
			1,
			new GoalSavingAccrualForceDto.ApplyRequest(28, GoalSavingAccrualForceDto.Scope.WEEK)
		);
		GoalSavingAccrualForceDto.Response second = service.apply(
			1,
			new GoalSavingAccrualForceDto.ApplyRequest(28, GoalSavingAccrualForceDto.Scope.WEEK)
		);

		assertThat(first.appliedAmount()).isEqualTo(42_000L);
		assertThat(second.appliedAmount()).isEqualTo(0L);
		assertThat(plan.getSavedAmount()).isEqualTo(42_000L);
	}

	@Test
	void applyThrowsWhenPlanDoesNotBelongToUser() {
		given(planRepository.findByIdAndUserId(99, 1)).willReturn(Optional.empty());
		GoalSavingAccrualApplyTestService service = serviceAt("2026-03-19T01:00:00Z");

		assertThatThrownBy(() -> service.apply(
			1,
			new GoalSavingAccrualForceDto.ApplyRequest(99, GoalSavingAccrualForceDto.Scope.WEEK)
		)).isInstanceOf(PlanException.class);
	}

	private GoalSavingAccrualApplyTestService serviceAt(String instant) {
		return new GoalSavingAccrualApplyTestService(
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

	private Plan activePlan(User user, int planId, String title, long savedAmount, int priority) {
		Plan plan = Plan.builder()
			.user(user)
			.title(title)
			.description(title)
			.targetAmount(1_000_000L)
			.savedAmount(savedAmount)
			.startDate(LocalDate.of(2026, 3, 1))
			.endDate(LocalDate.of(2026, 5, 31))
			.targetWeeks(13)
			.priority(priority)
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
			.status(BudgetWeekStatus.ACTIVE)
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
