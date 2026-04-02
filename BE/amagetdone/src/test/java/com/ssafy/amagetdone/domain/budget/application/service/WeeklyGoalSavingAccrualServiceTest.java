package com.ssafy.amagetdone.domain.budget.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.application.service.PlanPriorityService;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WeeklyGoalSavingAccrualServiceTest {

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Mock
	private BudgetAdjustmentRepository budgetAdjustmentRepository;

	@Mock
	private PlanRepository planRepository;

	@Mock
	private PlanPriorityService planPriorityService;

	@Mock
	private TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase;

	private final Clock clock = Clock.fixed(Instant.parse("2026-03-24T00:00:00Z"), ZoneId.of("Asia/Seoul"));

	private WeeklyGoalSavingAccrualService weeklyGoalSavingAccrualService;

	@BeforeEach
	void setUp() {
		weeklyGoalSavingAccrualService = new WeeklyGoalSavingAccrualService(
			budgetWeekRepository,
			planWeekAllocationRepository,
			budgetAdjustmentRepository,
			planRepository,
			planPriorityService,
			triggerBudgetAllocationUseCase,
			clock
		);
	}

	@Test
	void compactsWaitingAndActivePrioritiesWhenPlanBecomesCompleted() {
		User user = user(1);
		BudgetWeek budgetWeek = budgetWeek(user, 10);
		Plan completedPlan = activePlan(user, 101, 2, 900L, 1_000L);
		PlanWeekAllocation allocation = allocation(completedPlan, budgetWeek, 100L);

		given(budgetWeekRepository.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(
			1,
			LocalDate.of(2026, 3, 24),
			LocalDate.of(2026, 3, 24)
		)).willReturn(Optional.of(budgetWeek));
		given(planWeekAllocationRepository.findByBudgetWeekAndActualAmountIsNull(budgetWeek)).willReturn(List.of(allocation));
		given(planWeekAllocationRepository.findByBudgetWeek(budgetWeek)).willReturn(List.of());
		given(budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(budgetWeek)).willReturn(List.of());
		given(budgetAdjustmentRepository.save(any(BudgetAdjustment.class))).willAnswer(invocation -> invocation.getArgument(0));
		weeklyGoalSavingAccrualService.applyWeeklyAccrual(1);

		assertEquals(1_000L, completedPlan.getSavedAmount());
		verify(planPriorityService).compactPrioritiesAfterCompletion(1, 2);
		verify(planRepository).saveAll(List.of(completedPlan));
		verify(planRepository, never()).adjustSavedAmount(any(), any());
	}

	@Test
	void doesNotCompactPrioritiesWhenPlanRemainsActive() {
		User user = user(1);
		BudgetWeek budgetWeek = budgetWeek(user, 10);
		Plan activePlan = activePlan(user, 101, 2, 800L, 1_000L);
		PlanWeekAllocation allocation = allocation(activePlan, budgetWeek, 100L);

		given(budgetWeekRepository.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(
			1,
			LocalDate.of(2026, 3, 24),
			LocalDate.of(2026, 3, 24)
		)).willReturn(Optional.of(budgetWeek));
		given(planWeekAllocationRepository.findByBudgetWeekAndActualAmountIsNull(budgetWeek)).willReturn(List.of(allocation));
		given(planWeekAllocationRepository.findByBudgetWeek(budgetWeek)).willReturn(List.of());
		given(budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(budgetWeek)).willReturn(List.of());
		given(budgetAdjustmentRepository.save(any(BudgetAdjustment.class))).willAnswer(invocation -> invocation.getArgument(0));
		weeklyGoalSavingAccrualService.applyWeeklyAccrual(1);

		assertEquals(900L, activePlan.getSavedAmount());
		verify(planPriorityService, never()).compactPrioritiesAfterCompletion(any(), any());
		verify(planRepository, never()).saveAll(any());
		verify(planRepository, never()).adjustSavedAmount(any(), any());
	}

	@Test
	void preservesOriginalEndDateWhenShortfallExtendsPlan() {
		User user = user(1);
		BudgetWeek budgetWeek = budgetWeek(user, 10);
		ReflectionTestUtils.setField(budgetWeek, "plannedIncomeAmount", 0L);
		Plan activePlan = activePlan(user, 101, 2, 0L, 1_300L);
		PlanWeekAllocation allocation = allocation(activePlan, budgetWeek, 100L);

		given(budgetWeekRepository.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(
			1,
			LocalDate.of(2026, 3, 24),
			LocalDate.of(2026, 3, 24)
		)).willReturn(Optional.of(budgetWeek));
		given(planWeekAllocationRepository.findByBudgetWeekAndActualAmountIsNull(budgetWeek)).willReturn(List.of(allocation));
		given(planWeekAllocationRepository.findByBudgetWeek(budgetWeek)).willReturn(List.of());
		given(budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(budgetWeek)).willReturn(List.of());

		weeklyGoalSavingAccrualService.applyWeeklyAccrual(1);

		assertEquals(LocalDate.of(2026, 5, 31), activePlan.getOriginalEndDate());
		assertEquals(LocalDate.of(2026, 6, 7), activePlan.getEndDate());
		verify(planRepository).saveAll(List.of(activePlan));
		verify(triggerBudgetAllocationUseCase).executeIncludingPlanEnd(1, LocalDate.of(2026, 6, 7));
	}

	private User user(int userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}

	private BudgetWeek budgetWeek(User user, int id) {
		BudgetWeek budgetWeek = BudgetWeek.builder()
			.user(user)
			.weekStartDate(LocalDate.of(2026, 3, 24))
			.weekEndDate(LocalDate.of(2026, 3, 30))
			.plannedIncomeAmount(1_000L)
			.plannedEssentialAmount(0L)
			.rolloverInAmount(0L)
			.manualAdjustmentAmount(0L)
			.build();
		ReflectionTestUtils.setField(budgetWeek, "id", id);
		return budgetWeek;
	}

	private Plan activePlan(User user, int id, int priority, long savedAmount, long targetAmount) {
		Plan plan = Plan.builder()
			.user(user)
			.title("Goal")
			.targetAmount(targetAmount)
			.savedAmount(savedAmount)
			.startDate(LocalDate.of(2026, 3, 1))
			.endDate(LocalDate.of(2026, 5, 31))
			.targetWeeks(13)
			.priority(priority)
			.status(PlanStatus.ACTIVE)
			.build();
		ReflectionTestUtils.setField(plan, "id", id);
		return plan;
	}

	private PlanWeekAllocation allocation(Plan plan, BudgetWeek budgetWeek, long finalAmount) {
		return PlanWeekAllocation.builder()
			.plan(plan)
			.budgetWeek(budgetWeek)
			.finalAmount(finalAmount)
			.plannedAmount(finalAmount)
			.build();
	}
}
