package com.ssafy.amagetdone.domain.budget.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BudgetCurrentWeekSavingsReapplyTestServiceTest {

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Mock
	private BudgetAdjustmentRepository budgetAdjustmentRepository;

	@Mock
	private PlanRepository planRepository;

	@Mock
	private WeeklyGoalSavingAccrualService weeklyGoalSavingAccrualService;

	@Mock
	private BudgetAllocationTriggerService budgetAllocationTriggerService;

	@Mock
	private WeeklySavingsDetailQueryService weeklySavingsDetailQueryService;

	@Test
	void reapplyCurrentWeekSavingsRebuildsCurrentWeekBeforeApplyingWeeklyAccrual() {
		User user = user(1);
		BudgetWeek currentWeek = budgetWeek(user, 101, LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 29));
		Plan activePlan = plan(user, 11, "trip", PlanStatus.ACTIVE, LocalDate.of(2026, 3, 29));
		Plan completedPlan = plan(user, 12, "earbuds", PlanStatus.COMPLETED, LocalDate.of(2026, 3, 29));

		PlanWeekAllocation liveActiveAllocation = allocation(activePlan, currentWeek, 70_000L, 70_000L, 70_000L, "active-live");
		PlanWeekAllocation liveCompletedAllocation = allocation(
			completedPlan,
			currentWeek,
			80_000L,
			80_000L,
			80_000L,
			"completed-live"
		);
		PlanWeekAllocation preservedActiveAllocation = allocation(
			activePlan,
			currentWeek,
			70_000L,
			70_000L,
			70_000L,
			"active-preserved"
		);
		PlanWeekAllocation preservedCompletedAllocation = allocation(
			completedPlan,
			currentWeek,
			80_000L,
			80_000L,
			80_000L,
			"completed-preserved"
		);
		PlanWeekAllocation rebuiltActiveAllocation = allocation(
			activePlan,
			currentWeek,
			70_000L,
			70_000L,
			null,
			"active-rebuilt"
		);

		BudgetAdjustment autoAdd = autoAdjustment(currentWeek, activePlan, BudgetAdjustmentType.PLAN_SAVED_AUTO_ADD, 70_000L);
		BudgetAdjustment autoSubtract = autoAdjustment(
			currentWeek,
			completedPlan,
			BudgetAdjustmentType.PLAN_SAVED_AUTO_SUBTRACT,
			10_000L
		);
		WeeklySavingsDetailDto.Response detail = WeeklySavingsDetailDto.Response.of(
			"2026-W13",
			LocalDate.of(2026, 3, 23),
			LocalDate.of(2026, 3, 29),
			180_000L,
			150_000L,
			List.of()
		);

		given(budgetWeekRepository.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(
			1,
			LocalDate.of(2026, 3, 29),
			LocalDate.of(2026, 3, 29)
		)).willReturn(Optional.of(currentWeek));
		given(budgetWeekRepository.findAllByUserIdAndWeekEndDateGreaterThanEqualAndWeekStartDateLessThanEqualOrderByWeekStartDateAscIdDesc(
			1,
			LocalDate.of(2026, 3, 1),
			LocalDate.of(2026, 3, 29)
		)).willReturn(List.of(currentWeek), List.of(currentWeek));
		given(planWeekAllocationRepository.findByBudgetWeek(currentWeek))
			.willReturn(List.of(liveActiveAllocation, liveCompletedAllocation));
		given(planWeekAllocationRepository.findAllByBudgetWeekIn(List.of(currentWeek)))
			.willReturn(List.of(preservedActiveAllocation, preservedCompletedAllocation), List.of(rebuiltActiveAllocation));
		given(budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(currentWeek))
			.willReturn(List.of(autoAdd, autoSubtract));
		given(planRepository.findAllByUserIdAndDeletedFalse(1)).willReturn(List.of(activePlan, completedPlan));
		given(weeklySavingsDetailQueryService.getDetail(1, "2026-W13")).willReturn(detail);

		BudgetCurrentWeekSavingsReapplyTestService service = new BudgetCurrentWeekSavingsReapplyTestService(
			budgetWeekRepository,
			planWeekAllocationRepository,
			budgetAdjustmentRepository,
			planRepository,
			weeklyGoalSavingAccrualService,
			budgetAllocationTriggerService,
			weeklySavingsDetailQueryService,
			Clock.fixed(Instant.parse("2026-03-29T00:00:00Z"), ZoneId.of("Asia/Seoul"))
		);

		WeeklySavingsDetailDto.Response actual = service.reapplyCurrentWeekSavings(1);

		assertThat(actual).isSameAs(detail);
		assertThat(liveActiveAllocation.getActualAmount()).isNull();
		assertThat(liveCompletedAllocation.getActualAmount()).isEqualTo(80_000L);
		verify(planRepository).adjustSavedAmount(11, -70_000L);
		verify(planRepository).adjustSavedAmount(12, 10_000L);
		verify(budgetAdjustmentRepository).deleteAll(List.of(autoAdd, autoSubtract));
		verify(budgetAllocationTriggerService, times(2))
			.triggerHistorical(1, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 29));
		verify(weeklyGoalSavingAccrualService).applyWeeklyAccrual(1);

		ArgumentCaptor<List<PlanWeekAllocation>> captor = ArgumentCaptor.forClass(List.class);
		verify(planWeekAllocationRepository, times(2)).saveAll(captor.capture());
		List<PlanWeekAllocation> restoredAllocations = captor.getAllValues().get(1);
		assertThat(findAllocation(restoredAllocations, 11, LocalDate.of(2026, 3, 23)).getActualAmount()).isEqualTo(70_000L);
		assertThat(findAllocation(restoredAllocations, 11, LocalDate.of(2026, 3, 23)).getPlannedAmount()).isEqualTo(70_000L);
		assertThat(findAllocation(restoredAllocations, 12, LocalDate.of(2026, 3, 23)).getActualAmount()).isEqualTo(80_000L);
	}

	@Test
	void reapplyCurrentWeekSavingsRestoresCompletedSuccessPartialAndFailedGoalStory() {
		User user = user(1);
		BudgetWeek week1 = budgetWeek(user, 91, LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 15));
		BudgetWeek week2 = budgetWeek(user, 92, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22));
		BudgetWeek week3 = budgetWeek(user, 93, LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 29));
		BudgetWeek currentWeek = budgetWeek(user, 94, LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 5));
		BudgetWeek week5 = budgetWeek(user, 95, LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 12));
		List<BudgetWeek> preservedWeeks = List.of(week1, week2, week3, currentWeek, week5);

		BudgetWeek rebuiltWeek1 = budgetWeek(user, 191, LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 15));
		BudgetWeek rebuiltWeek2 = budgetWeek(user, 192, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22));
		BudgetWeek rebuiltWeek3 = budgetWeek(user, 193, LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 29));
		BudgetWeek rebuiltCurrentWeek = budgetWeek(user, 194, LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 5));
		BudgetWeek rebuiltWeek5 = budgetWeek(user, 195, LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 12));
		List<BudgetWeek> rebuiltWeeks = List.of(rebuiltWeek1, rebuiltWeek2, rebuiltWeek3, rebuiltCurrentWeek, rebuiltWeek5);

		Plan completedPlan = plan(user, 14, "completed", PlanStatus.COMPLETED, LocalDate.of(2026, 4, 5));
		Plan successPlan = plan(user, 11, "success", PlanStatus.ACTIVE, LocalDate.of(2026, 4, 12));
		Plan partialPlan = plan(user, 12, "partial", PlanStatus.ACTIVE, LocalDate.of(2026, 4, 12));
		Plan failedPlan = plan(user, 13, "failed", PlanStatus.ACTIVE, LocalDate.of(2026, 4, 12));

		List<PlanWeekAllocation> preservedAllocations = List.of(
			allocation(completedPlan, week1, 80_000L, 80_000L, 80_000L, "completed-1"),
			allocation(completedPlan, week2, 80_000L, 80_000L, 80_000L, "completed-2"),
			allocation(completedPlan, week3, 80_000L, 80_000L, 80_000L, "completed-3"),
			allocation(completedPlan, currentWeek, 80_000L, 80_000L, 80_000L, "completed-4"),
			allocation(successPlan, week1, 70_000L, 70_000L, 70_000L, "success-1"),
			allocation(successPlan, week2, 70_000L, 70_000L, 70_000L, "success-2"),
			allocation(successPlan, week3, 70_000L, 70_000L, 70_000L, "success-3"),
			allocation(successPlan, currentWeek, 70_000L, 70_000L, 70_000L, "success-4"),
			allocation(successPlan, week5, 70_000L, 70_000L, null, "success-5"),
			allocation(partialPlan, week1, 100_000L, 100_000L, 100_000L, "partial-1"),
			allocation(partialPlan, week2, 100_000L, 100_000L, 100_000L, "partial-2"),
			allocation(partialPlan, week3, 100_000L, 100_000L, 100_000L, "partial-3"),
			allocation(partialPlan, currentWeek, 100_000L, 100_000L, 50_000L, "partial-4"),
			allocation(failedPlan, week1, 30_000L, 30_000L, 30_000L, "failed-1"),
			allocation(failedPlan, week2, 30_000L, 30_000L, 30_000L, "failed-2"),
			allocation(failedPlan, week3, 30_000L, 30_000L, 30_000L, "failed-3"),
			allocation(failedPlan, currentWeek, 30_000L, 30_000L, 0L, "failed-4")
		);

		List<PlanWeekAllocation> rebuiltAllocations = List.of(
			allocation(successPlan, rebuiltWeek1, 70_000L, 70_000L, null, "rebuilt-success-1"),
			allocation(successPlan, rebuiltWeek2, 70_000L, 70_000L, null, "rebuilt-success-2"),
			allocation(successPlan, rebuiltWeek3, 70_000L, 70_000L, null, "rebuilt-success-3"),
			allocation(successPlan, rebuiltCurrentWeek, 70_000L, 70_000L, null, "rebuilt-success-4"),
			allocation(successPlan, rebuiltWeek5, 70_000L, 70_000L, null, "rebuilt-success-5"),
			allocation(partialPlan, rebuiltWeek1, 100_000L, 100_000L, null, "rebuilt-partial-1"),
			allocation(partialPlan, rebuiltWeek2, 100_000L, 100_000L, null, "rebuilt-partial-2"),
			allocation(partialPlan, rebuiltWeek3, 100_000L, 100_000L, null, "rebuilt-partial-3"),
			allocation(partialPlan, rebuiltCurrentWeek, 100_000L, 100_000L, null, "rebuilt-partial-4"),
			allocation(partialPlan, rebuiltWeek5, 50_000L, 50_000L, null, "rebuilt-partial-5"),
			allocation(failedPlan, rebuiltWeek1, 30_000L, 30_000L, null, "rebuilt-failed-1"),
			allocation(failedPlan, rebuiltWeek2, 30_000L, 30_000L, null, "rebuilt-failed-2"),
			allocation(failedPlan, rebuiltWeek3, 30_000L, 30_000L, null, "rebuilt-failed-3"),
			allocation(failedPlan, rebuiltCurrentWeek, 30_000L, 30_000L, null, "rebuilt-failed-4"),
			allocation(failedPlan, rebuiltWeek5, 30_000L, 30_000L, null, "rebuilt-failed-5")
		);

		BudgetAdjustment successAutoAdd = autoAdjustment(
			currentWeek,
			successPlan,
			BudgetAdjustmentType.PLAN_SAVED_AUTO_ADD,
			70_000L
		);
		BudgetAdjustment partialAutoAdd = autoAdjustment(
			currentWeek,
			partialPlan,
			BudgetAdjustmentType.PLAN_SAVED_AUTO_ADD,
			50_000L
		);

		WeeklySavingsDetailDto.Response detail = WeeklySavingsDetailDto.Response.of(
			"2026-W14",
			LocalDate.of(2026, 3, 30),
			LocalDate.of(2026, 4, 5),
			180_000L,
			150_000L,
			List.of()
		);

		given(budgetWeekRepository.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(
			1,
			LocalDate.of(2026, 3, 30),
			LocalDate.of(2026, 3, 30)
		)).willReturn(Optional.of(currentWeek));
		given(budgetWeekRepository.findAllByUserIdAndWeekEndDateGreaterThanEqualAndWeekStartDateLessThanEqualOrderByWeekStartDateAscIdDesc(
			1,
			LocalDate.of(2026, 3, 1),
			LocalDate.of(2026, 4, 12)
		)).willReturn(preservedWeeks, rebuiltWeeks);
		given(planWeekAllocationRepository.findByBudgetWeek(currentWeek)).willReturn(List.of(
			allocation(completedPlan, currentWeek, 80_000L, 80_000L, 80_000L, "completed-live"),
			allocation(successPlan, currentWeek, 70_000L, 70_000L, 70_000L, "success-live"),
			allocation(partialPlan, currentWeek, 100_000L, 100_000L, 50_000L, "partial-live"),
			allocation(failedPlan, currentWeek, 30_000L, 30_000L, 0L, "failed-live")
		));
		given(planWeekAllocationRepository.findAllByBudgetWeekIn(preservedWeeks)).willReturn(preservedAllocations);
		given(planWeekAllocationRepository.findAllByBudgetWeekIn(rebuiltWeeks)).willReturn(rebuiltAllocations);
		given(budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(currentWeek))
			.willReturn(List.of(partialAutoAdd, successAutoAdd));
		given(planRepository.findAllByUserIdAndDeletedFalse(1))
			.willReturn(List.of(completedPlan, successPlan, partialPlan, failedPlan));
		given(weeklySavingsDetailQueryService.getDetail(1, "2026-W14")).willReturn(detail);

		BudgetCurrentWeekSavingsReapplyTestService service = new BudgetCurrentWeekSavingsReapplyTestService(
			budgetWeekRepository,
			planWeekAllocationRepository,
			budgetAdjustmentRepository,
			planRepository,
			weeklyGoalSavingAccrualService,
			budgetAllocationTriggerService,
			weeklySavingsDetailQueryService,
			Clock.fixed(Instant.parse("2026-03-30T00:00:00Z"), ZoneId.of("Asia/Seoul"))
		);

		service.reapplyCurrentWeekSavings(1);

		verify(budgetAllocationTriggerService, times(2))
			.triggerHistorical(1, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 12));

		ArgumentCaptor<List<PlanWeekAllocation>> captor = ArgumentCaptor.forClass(List.class);
		verify(planWeekAllocationRepository, times(2)).saveAll(captor.capture());
		List<PlanWeekAllocation> restoredAllocations = captor.getAllValues().get(1);

		assertThat(restoredAllocations).hasSize(17);
		assertThat(findAllocation(restoredAllocations, 14, LocalDate.of(2026, 3, 30)).getActualAmount()).isEqualTo(80_000L);
		assertThat(findAllocation(restoredAllocations, 11, LocalDate.of(2026, 3, 30)).getActualAmount()).isEqualTo(70_000L);
		assertThat(findAllocation(restoredAllocations, 12, LocalDate.of(2026, 3, 30)).getPlannedAmount()).isEqualTo(100_000L);
		assertThat(findAllocation(restoredAllocations, 12, LocalDate.of(2026, 3, 30)).getActualAmount()).isEqualTo(50_000L);
		assertThat(findAllocation(restoredAllocations, 13, LocalDate.of(2026, 3, 30)).getPlannedAmount()).isEqualTo(30_000L);
		assertThat(findAllocation(restoredAllocations, 13, LocalDate.of(2026, 3, 30)).getActualAmount()).isZero();
	}

	private PlanWeekAllocation findAllocation(List<PlanWeekAllocation> allocations, int planId, LocalDate weekStartDate) {
		return allocations.stream()
			.filter(allocation -> allocation.getPlan().getId().equals(planId))
			.filter(allocation -> allocation.getBudgetWeek().getWeekStartDate().equals(weekStartDate))
			.findFirst()
			.orElseThrow();
	}

	private BudgetAdjustment autoAdjustment(
		BudgetWeek budgetWeek,
		Plan plan,
		BudgetAdjustmentType adjustmentType,
		long adjustmentAmount
	) {
		return BudgetAdjustment.builder()
			.budgetWeek(budgetWeek)
			.plan(plan)
			.adjustmentType(adjustmentType)
			.adjustmentAmount(adjustmentAmount)
			.reason("Weekly goal saving accrual applied")
			.build();
	}

	private User user(int userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}

	private BudgetWeek budgetWeek(User user, int id, LocalDate startDate, LocalDate endDate) {
		BudgetWeek week = BudgetWeek.builder()
			.user(user)
			.weekSequence(1)
			.weekStartDate(startDate)
			.weekEndDate(endDate)
			.periodDays(7)
			.partial(false)
			.status(BudgetWeekStatus.ACTIVE)
			.build();
		ReflectionTestUtils.setField(week, "id", id);
		return week;
	}

	private Plan plan(User user, int id, String title, PlanStatus status, LocalDate endDate) {
		Plan plan = Plan.builder()
			.user(user)
			.title(title)
			.description(title)
			.targetAmount(400_000L)
			.savedAmount(0L)
			.startDate(LocalDate.of(2026, 3, 9))
			.endDate(endDate)
			.targetWeeks(5)
			.priority(1)
			.status(status)
			.build();
		ReflectionTestUtils.setField(plan, "id", id);
		return plan;
	}

	private PlanWeekAllocation allocation(
		Plan plan,
		BudgetWeek budgetWeek,
		long plannedAmount,
		long finalAmount,
		Long actualAmount,
		String note
	) {
		PlanWeekAllocation allocation = PlanWeekAllocation.builder()
			.plan(plan)
			.budgetWeek(budgetWeek)
			.plannedAmount(plannedAmount)
			.finalAmount(finalAmount)
			.actualAmount(actualAmount)
			.note(note)
			.build();
		ReflectionTestUtils.setField(allocation, "budgetWeek", budgetWeek);
		return allocation;
	}
}
