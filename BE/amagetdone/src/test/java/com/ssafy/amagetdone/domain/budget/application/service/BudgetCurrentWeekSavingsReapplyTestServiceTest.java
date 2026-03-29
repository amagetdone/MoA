package com.ssafy.amagetdone.domain.budget.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
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
import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
	private TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase;

	@Mock
	private WeeklySavingsDetailQueryService weeklySavingsDetailQueryService;

	@Test
	void reapplyCurrentWeekSavingsRemovesAutoAdjustmentsThenRunsWeeklyAccrualAgain() {
		User user = user(1);
		BudgetWeek currentWeek = budgetWeek(user, 101, LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 29));
		Plan activePlan = plan(user, 11, "제주도 여행 경비 모으기", PlanStatus.ACTIVE);
		Plan completedPlan = plan(user, 12, "블루투스 이어폰 구매하기", PlanStatus.COMPLETED);
		PlanWeekAllocation activeAllocation = PlanWeekAllocation.builder()
			.plan(activePlan)
			.budgetWeek(currentWeek)
			.plannedAmount(70_000L)
			.finalAmount(70_000L)
			.actualAmount(70_000L)
			.build();
		PlanWeekAllocation completedAllocation = PlanWeekAllocation.builder()
			.plan(completedPlan)
			.budgetWeek(currentWeek)
			.plannedAmount(80_000L)
			.finalAmount(80_000L)
			.actualAmount(80_000L)
			.build();
		BudgetAdjustment autoAdd = BudgetAdjustment.builder()
			.budgetWeek(currentWeek)
			.plan(activePlan)
			.adjustmentType(BudgetAdjustmentType.PLAN_SAVED_AUTO_ADD)
			.adjustmentAmount(70_000L)
			.reason("Weekly goal saving accrual applied")
			.build();
		BudgetAdjustment autoSubtract = BudgetAdjustment.builder()
			.budgetWeek(currentWeek)
			.plan(completedPlan)
			.adjustmentType(BudgetAdjustmentType.PLAN_SAVED_AUTO_SUBTRACT)
			.adjustmentAmount(10_000L)
			.reason("Automatic daily goal saving accrual reconciled")
			.build();
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
		given(planWeekAllocationRepository.findByBudgetWeek(currentWeek)).willReturn(List.of(activeAllocation, completedAllocation));
		given(budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(currentWeek)).willReturn(List.of(autoAdd, autoSubtract));
		given(weeklySavingsDetailQueryService.getDetail(1, "2026-W13")).willReturn(detail);

		BudgetCurrentWeekSavingsReapplyTestService service = new BudgetCurrentWeekSavingsReapplyTestService(
			budgetWeekRepository,
			planWeekAllocationRepository,
			budgetAdjustmentRepository,
			planRepository,
			weeklyGoalSavingAccrualService,
			triggerBudgetAllocationUseCase,
			weeklySavingsDetailQueryService,
			Clock.fixed(Instant.parse("2026-03-29T00:00:00Z"), ZoneId.of("Asia/Seoul"))
		);

		WeeklySavingsDetailDto.Response actual = service.reapplyCurrentWeekSavings(1);

		assertThat(actual).isSameAs(detail);
		assertThat(activeAllocation.getActualAmount()).isNull();
		assertThat(completedAllocation.getActualAmount()).isEqualTo(80_000L);
		verify(planRepository).adjustSavedAmount(11, -70_000L);
		verify(planRepository).adjustSavedAmount(12, 10_000L);
		verify(budgetAdjustmentRepository).deleteAll(List.of(autoAdd, autoSubtract));
		verify(planWeekAllocationRepository).saveAll(List.of(activeAllocation, completedAllocation));
		verify(weeklyGoalSavingAccrualService).applyWeeklyAccrual(1);
		verify(triggerBudgetAllocationUseCase).execute(1);

		InOrder inOrder = inOrder(
			planRepository,
			budgetAdjustmentRepository,
			planWeekAllocationRepository,
			weeklyGoalSavingAccrualService,
			triggerBudgetAllocationUseCase,
			weeklySavingsDetailQueryService
		);
		inOrder.verify(planRepository).adjustSavedAmount(11, -70_000L);
		inOrder.verify(planRepository).adjustSavedAmount(12, 10_000L);
		inOrder.verify(budgetAdjustmentRepository).deleteAll(List.of(autoAdd, autoSubtract));
		inOrder.verify(planWeekAllocationRepository).saveAll(List.of(activeAllocation, completedAllocation));
		inOrder.verify(weeklyGoalSavingAccrualService).applyWeeklyAccrual(1);
		inOrder.verify(triggerBudgetAllocationUseCase).execute(1);
		inOrder.verify(weeklySavingsDetailQueryService).getDetail(1, "2026-W13");
	}

	@Test
	void reapplyCurrentWeekSavingsRestoresCompletedPlanAllocationsRemovedByReallocation() {
		User user = user(1);
		BudgetWeek week1 = budgetWeek(user, 91, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8));
		BudgetWeek week2 = budgetWeek(user, 92, LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 15));
		BudgetWeek week3 = budgetWeek(user, 93, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22));
		BudgetWeek currentWeek = budgetWeek(user, 94, LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 29));
		List<BudgetWeek> beforeTriggerWeeks = List.of(week1, week2, week3, currentWeek);

		BudgetWeek rebuiltWeek1 = budgetWeek(user, 191, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8));
		BudgetWeek rebuiltWeek2 = budgetWeek(user, 192, LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 15));
		BudgetWeek rebuiltWeek3 = budgetWeek(user, 193, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22));
		BudgetWeek rebuiltCurrentWeek = budgetWeek(user, 194, LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 29));
		List<BudgetWeek> afterTriggerWeeks = List.of(rebuiltWeek1, rebuiltWeek2, rebuiltWeek3, rebuiltCurrentWeek);

		Plan successPlan = plan(user, 11, "제주도 여행 경비 모으기", PlanStatus.ACTIVE);
		Plan completedPlan = plan(user, 12, "블루투스 이어폰 구매하기", PlanStatus.COMPLETED);

		PlanWeekAllocation activeCurrentAllocation = allocation(successPlan, currentWeek, 70_000L, 70_000L, 70_000L, "active-current");
		PlanWeekAllocation completedAllocation1 = allocation(completedPlan, week1, 80_000L, 80_000L, 80_000L, "completed-1");
		PlanWeekAllocation completedAllocation2 = allocation(completedPlan, week2, 80_000L, 80_000L, 80_000L, "completed-2");
		PlanWeekAllocation completedAllocation3 = allocation(completedPlan, week3, 80_000L, 80_000L, 80_000L, "completed-3");
		PlanWeekAllocation completedAllocation4 = allocation(completedPlan, currentWeek, 80_000L, 80_000L, 80_000L, "completed-4");

		BudgetAdjustment autoAdd = BudgetAdjustment.builder()
			.budgetWeek(currentWeek)
			.plan(successPlan)
			.adjustmentType(BudgetAdjustmentType.PLAN_SAVED_AUTO_ADD)
			.adjustmentAmount(70_000L)
			.reason("Weekly goal saving accrual applied")
			.build();
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
			LocalDate.of(2026, 3, 31)
		)).willReturn(beforeTriggerWeeks, afterTriggerWeeks);
		given(planWeekAllocationRepository.findByBudgetWeek(currentWeek)).willReturn(List.of(activeCurrentAllocation, completedAllocation4));
		given(planWeekAllocationRepository.findAllByBudgetWeekIn(beforeTriggerWeeks)).willReturn(List.of(
			activeCurrentAllocation,
			completedAllocation1,
			completedAllocation2,
			completedAllocation3,
			completedAllocation4
		));
		given(planWeekAllocationRepository.findAllByBudgetWeekIn(afterTriggerWeeks)).willReturn(List.of(
			allocation(successPlan, rebuiltCurrentWeek, 70_000L, 70_000L, 70_000L, "rebuilt-active-current")
		));
		given(budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(currentWeek)).willReturn(List.of(autoAdd));
		given(weeklySavingsDetailQueryService.getDetail(1, "2026-W13")).willReturn(detail);

		BudgetCurrentWeekSavingsReapplyTestService service = new BudgetCurrentWeekSavingsReapplyTestService(
			budgetWeekRepository,
			planWeekAllocationRepository,
			budgetAdjustmentRepository,
			planRepository,
			weeklyGoalSavingAccrualService,
			triggerBudgetAllocationUseCase,
			weeklySavingsDetailQueryService,
			Clock.fixed(Instant.parse("2026-03-29T00:00:00Z"), ZoneId.of("Asia/Seoul"))
		);

		service.reapplyCurrentWeekSavings(1);

		ArgumentCaptor<List<PlanWeekAllocation>> captor = ArgumentCaptor.forClass(List.class);
		verify(planWeekAllocationRepository, times(2)).saveAll(captor.capture());
		List<List<PlanWeekAllocation>> savedBatches = captor.getAllValues();
		List<PlanWeekAllocation> restoredAllocations = savedBatches.get(1);

		assertThat(restoredAllocations).hasSize(4);
		assertThat(restoredAllocations)
			.extracting(allocation -> allocation.getBudgetWeek().getWeekStartDate())
			.containsExactly(
				LocalDate.of(2026, 3, 2),
				LocalDate.of(2026, 3, 9),
				LocalDate.of(2026, 3, 16),
				LocalDate.of(2026, 3, 23)
			);
		assertThat(restoredAllocations)
			.extracting(allocation -> allocation.getBudgetWeek().getId())
			.containsExactly(191, 192, 193, 194);
		assertThat(restoredAllocations)
			.extracting(allocation -> allocation.getPlan().getId())
			.containsOnly(12);
		assertThat(restoredAllocations)
			.extracting(PlanWeekAllocation::getActualAmount)
			.containsExactly(80_000L, 80_000L, 80_000L, 80_000L);
		assertThat(restoredAllocations)
			.extracting(PlanWeekAllocation::getNote)
			.containsExactly("completed-1", "completed-2", "completed-3", "completed-4");
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

	private Plan plan(User user, int id, String title, PlanStatus status) {
		Plan plan = Plan.builder()
			.user(user)
			.title(title)
			.description(title)
			.targetAmount(400_000L)
			.savedAmount(0L)
			.startDate(LocalDate.of(2026, 2, 23))
			.endDate(LocalDate.of(2026, 3, 29))
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
