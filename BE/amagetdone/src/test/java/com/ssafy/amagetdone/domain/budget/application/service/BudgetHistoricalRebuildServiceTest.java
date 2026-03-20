package com.ssafy.amagetdone.domain.budget.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetRolloverRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BudgetHistoricalRebuildServiceTest {

	@Mock
	private PlanRepository planRepository;

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private BudgetMonthRepository budgetMonthRepository;

	@Mock
	private BudgetAdjustmentRepository budgetAdjustmentRepository;

	@Mock
	private BudgetRolloverRepository budgetRolloverRepository;

	@Mock
	private EssentialWeekAllocationRepository essentialWeekAllocationRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Mock
	private BudgetAllocationTriggerService budgetAllocationTriggerService;

	@Mock
	private BudgetActualSpentRefreshService budgetActualSpentRefreshService;

	@Mock
	private BudgetRolloverResyncService budgetRolloverResyncService;

	@Mock
	private DailyGoalSavingAccrualService dailyGoalSavingAccrualService;

	@Test
	void rebuildResetsHistoricalBudgetStateThenReplaysAutoAccrual() {
		User user = user(1);
		Plan targetPlan = activePlan(user, 11, 120_000L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));
		BudgetWeek staleWeek = budgetWeek(user, 101, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 7));
		BudgetMonth staleMonth = budgetMonth(user, 201, LocalDate.of(2026, 3, 1));
		PlanWeekAllocation stalePlanAllocation = PlanWeekAllocation.builder()
			.plan(targetPlan)
			.budgetWeek(staleWeek)
			.plannedAmount(70_000L)
			.finalAmount(50_000L)
			.build();
		EssentialWeekAllocation staleEssentialAllocation = EssentialWeekAllocation.builder()
			.budgetWeek(staleWeek)
			.plannedAmount(30_000L)
			.actualSpentAmount(10_000L)
			.build();
		BudgetAdjustment staleAutoAdd = BudgetAdjustment.builder()
			.budgetWeek(staleWeek)
			.plan(targetPlan)
			.adjustmentType(BudgetAdjustmentType.PLAN_SAVED_AUTO_ADD)
			.adjustmentAmount(30_000L)
			.reason("Automatic daily goal saving accrual applied")
			.build();
		BudgetAdjustment staleManualAdd = BudgetAdjustment.builder()
			.budgetWeek(staleWeek)
			.plan(targetPlan)
			.adjustmentType(BudgetAdjustmentType.PLAN_SAVED_ADD)
			.adjustmentAmount(15_000L)
			.reason("manual")
			.build();
		BudgetWeek rebuiltWeek = budgetWeek(user, 102, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 7));
		BudgetAllocationService.AllocationResult rebuiltAllocation = new BudgetAllocationService.AllocationResult(
			List.of(staleMonth),
			List.of(rebuiltWeek),
			List.of(),
			List.of(),
			List.of()
		);
		GoalSavingAccrualForceDto.Response rebuiltResponse = new GoalSavingAccrualForceDto.Response(
			GoalSavingAccrualForceDto.OperationType.BACKFILL,
			null,
			GoalSavingAccrualForceDto.TargetType.ALL,
			null,
			45_000L,
			LocalDate.of(2026, 3, 20),
			LocalDate.of(2026, 3, 19),
			LocalDate.of(2026, 3, 1),
			LocalDate.of(2026, 3, 19),
			"UP_TO_2026-03-19",
			List.of()
		);

		given(planRepository.findByIdAndUserId(11, 1)).willReturn(Optional.of(targetPlan));
		given(budgetWeekRepository.findAll()).willReturn(List.of(staleWeek));
		given(budgetMonthRepository.findAll()).willReturn(List.of(staleMonth));
		given(budgetAdjustmentRepository.findAllByBudgetWeekIn(List.of(staleWeek)))
			.willReturn(List.of(staleAutoAdd, staleManualAdd));
		given(essentialWeekAllocationRepository.findAllByBudgetWeekIn(List.of(staleWeek)))
			.willReturn(List.of(staleEssentialAllocation));
		given(planWeekAllocationRepository.findAllByBudgetWeekIn(List.of(staleWeek)))
			.willReturn(List.of(stalePlanAllocation));
		given(budgetAllocationTriggerService.triggerHistorical(1, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 20)))
			.willReturn(rebuiltAllocation);
		given(dailyGoalSavingAccrualService.backfillAccrual(1, new GoalSavingAccrualForceDto.BackfillRequest(null)))
			.willReturn(rebuiltResponse);

		BudgetHistoricalRebuildService service = new BudgetHistoricalRebuildService(
			planRepository,
			budgetWeekRepository,
			budgetMonthRepository,
			budgetAdjustmentRepository,
			budgetRolloverRepository,
			essentialWeekAllocationRepository,
			planWeekAllocationRepository,
			budgetAllocationTriggerService,
			budgetActualSpentRefreshService,
			budgetRolloverResyncService,
			dailyGoalSavingAccrualService,
			Clock.fixed(Instant.parse("2026-03-20T00:00:00Z"), ZoneId.of("Asia/Seoul"))
		);

		GoalSavingAccrualForceDto.Response response = service.rebuild(1, 11);

		assertThat(response).isSameAs(rebuiltResponse);
		assertThat(targetPlan.getSavedAmount()).isEqualTo(75_000L);
		verify(planRepository).saveAll(argThat(plans -> {
			java.util.List<Plan> savedPlans = new java.util.ArrayList<>();
			plans.forEach(savedPlans::add);
			return savedPlans.size() == 1 && savedPlans.contains(targetPlan);
		}));
		verify(budgetAdjustmentRepository).deleteAll(List.of(staleAutoAdd, staleManualAdd));
		verify(essentialWeekAllocationRepository).deleteAll(List.of(staleEssentialAllocation));
		verify(planWeekAllocationRepository).deleteAll(List.of(stalePlanAllocation));
		verify(budgetWeekRepository).deleteAll(List.of(staleWeek));
		verify(budgetAllocationTriggerService).triggerHistorical(1, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 20));
		verify(budgetActualSpentRefreshService).refresh(1, List.of(rebuiltWeek));
		verify(budgetRolloverResyncService).resync(1);
		verify(dailyGoalSavingAccrualService).backfillAccrual(1, new GoalSavingAccrualForceDto.BackfillRequest(null));

		InOrder inOrder = inOrder(
			budgetAdjustmentRepository,
			budgetRolloverRepository,
			essentialWeekAllocationRepository,
			planWeekAllocationRepository,
			budgetWeekRepository,
			budgetAllocationTriggerService,
			budgetActualSpentRefreshService,
			budgetRolloverResyncService,
			dailyGoalSavingAccrualService
		);
		inOrder.verify(budgetAdjustmentRepository).deleteAll(List.of(staleAutoAdd, staleManualAdd));
		inOrder.verify(budgetRolloverRepository).deleteAllBySourceBudgetWeekIn(List.of(staleWeek));
		inOrder.verify(budgetRolloverRepository).deleteAllByTargetBudgetWeekIn(List.of(staleWeek));
		inOrder.verify(essentialWeekAllocationRepository).deleteAll(List.of(staleEssentialAllocation));
		inOrder.verify(planWeekAllocationRepository).deleteAll(List.of(stalePlanAllocation));
		inOrder.verify(budgetWeekRepository).deleteAll(List.of(staleWeek));
		inOrder.verify(budgetAllocationTriggerService).triggerHistorical(1, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 20));
		inOrder.verify(budgetActualSpentRefreshService).refresh(1, List.of(rebuiltWeek));
		inOrder.verify(budgetRolloverResyncService).resync(1);
		inOrder.verify(dailyGoalSavingAccrualService).backfillAccrual(1, new GoalSavingAccrualForceDto.BackfillRequest(null));
	}

	@Test
	void rebuildThrowsWhenPlanDoesNotBelongToUser() {
		given(planRepository.findByIdAndUserId(99, 1)).willReturn(Optional.empty());

		BudgetHistoricalRebuildService service = new BudgetHistoricalRebuildService(
			planRepository,
			budgetWeekRepository,
			budgetMonthRepository,
			budgetAdjustmentRepository,
			budgetRolloverRepository,
			essentialWeekAllocationRepository,
			planWeekAllocationRepository,
			budgetAllocationTriggerService,
			budgetActualSpentRefreshService,
			budgetRolloverResyncService,
			dailyGoalSavingAccrualService,
			Clock.fixed(Instant.parse("2026-03-20T00:00:00Z"), ZoneId.of("Asia/Seoul"))
		);

		assertThatThrownBy(() -> service.rebuild(1, 99))
			.isInstanceOf(PlanException.class);
	}

	private User user(int userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}

	private Plan activePlan(User user, int planId, long savedAmount, LocalDate startDate, LocalDate endDate) {
		Plan plan = Plan.builder()
			.user(user)
			.title("Travel Fund")
			.description("Travel Fund")
			.targetAmount(1_000_000L)
			.savedAmount(savedAmount)
			.startDate(startDate)
			.endDate(endDate)
			.targetWeeks(18)
			.priority(1)
			.status(PlanStatus.ACTIVE)
			.build();
		ReflectionTestUtils.setField(plan, "id", planId);
		return plan;
	}

	private BudgetWeek budgetWeek(User user, int budgetWeekId, LocalDate startDate, LocalDate endDate) {
		BudgetWeek budgetWeek = BudgetWeek.builder()
			.user(user)
			.weekSequence(1)
			.weekStartDate(startDate)
			.weekEndDate(endDate)
			.periodDays((int)(endDate.toEpochDay() - startDate.toEpochDay() + 1L))
			.build();
		ReflectionTestUtils.setField(budgetWeek, "id", budgetWeekId);
		return budgetWeek;
	}

	private BudgetMonth budgetMonth(User user, int budgetMonthId, LocalDate baseDate) {
		BudgetMonth budgetMonth = BudgetMonth.builder()
			.user(user)
			.baseDate(baseDate)
			.totalIncomeAmount(2_000_000L)
			.totalFixedEssentialAmount(500_000L)
			.totalVariableEssentialAmount(300_000L)
			.build();
		ReflectionTestUtils.setField(budgetMonth, "id", budgetMonthId);
		return budgetMonth;
	}
}
