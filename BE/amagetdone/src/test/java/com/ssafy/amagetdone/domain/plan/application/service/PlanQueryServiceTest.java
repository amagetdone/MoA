package com.ssafy.amagetdone.domain.plan.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanDetailResponse;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanWeeklyPlansPageResponse;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlanQueryServiceTest {

	@Mock
	private PlanRepository planRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	private PlanQueryService service;

	@BeforeEach
	void setUp() {
		service = new PlanQueryService(
			planRepository,
			planWeekAllocationRepository,
			budgetWeekRepository,
			Clock.fixed(Instant.parse("2026-03-19T00:00:00Z"), ZoneId.of("Asia/Seoul"))
		);
	}

	@Test
	void getPlanDetailReturnsYearlyGraphAndWeeklyPlans() {
		Plan plan = plan(1, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 3_000_000L, 1_200_000L);
		List<PlanWeekAllocation> allocations = List.of(
			allocation(plan, budgetWeek(10, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 11)), 50_000L, 45_000L),
			allocation(plan, budgetWeek(11, LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 18)), 70_000L, 60_000L),
			allocation(plan, budgetWeek(12, LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 4)), 30_000L, 20_000L),
			allocation(plan, budgetWeek(99, LocalDate.of(2027, 1, 4), LocalDate.of(2027, 1, 10)), 90_000L, 80_000L)
		);

		given(planRepository.findByIdAndUserId(1, 1)).willReturn(Optional.of(plan));
		given(planWeekAllocationRepository.findAllByPlanIdOrderByBudgetWeekWeekSequenceAsc(1)).willReturn(allocations);
		given(budgetWeekRepository.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(any(), any(), any()))
			.willReturn(Optional.of(currentBudgetWeek(11)));

		PlanDetailResponse response = service.getPlanDetail(1, 1);

		assertThat(response.planId()).isEqualTo(1);
		assertThat(response.graphData().weeks()).containsExactly(10, 11, 12);
		assertThat(response.graphData().planned()).containsExactly(50_000L, 70_000L, 30_000L);
		assertThat(response.graphData().actual()).containsExactly(45_000L, 60_000L, 20_000L);
		assertThat(response.weeklyPlans()).containsExactly(
			new PlanDetailResponse.WeeklyPlan(10, 50_000L),
			new PlanDetailResponse.WeeklyPlan(11, 70_000L),
			new PlanDetailResponse.WeeklyPlan(12, 30_000L)
		);
		assertThat(response.currentWeek()).isEqualTo(11);
		assertThat(response.totalWeeks()).isEqualTo(3);
		assertThat(response.achievementRate()).isEqualTo(40);
		assertThat(response.dday()).isEqualTo(287L);
	}

	@Test
	void getPlanDetailReturnsEmptyYearlyDataAndZeroCurrentWeekWhenNoMatchingData() {
		Plan plan = plan(1, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 3_000_000L, 1_200_000L);

		given(planRepository.findByIdAndUserId(1, 1)).willReturn(Optional.of(plan));
		given(planWeekAllocationRepository.findAllByPlanIdOrderByBudgetWeekWeekSequenceAsc(1)).willReturn(List.of());
		given(budgetWeekRepository.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(any(), any(), any()))
			.willReturn(Optional.empty());

		PlanDetailResponse response = service.getPlanDetail(1, 1);

		assertThat(response.graphData().weeks()).isEmpty();
		assertThat(response.graphData().planned()).isEmpty();
		assertThat(response.graphData().actual()).isEmpty();
		assertThat(response.weeklyPlans()).isEmpty();
		assertThat(response.totalWeeks()).isEqualTo(0);
		assertThat(response.currentWeek()).isEqualTo(0);
	}

	@Test
	void listPlanWeeklyPlansReturnsRequestedPage() {
		Plan plan = plan(1, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 3_000_000L, 1_200_000L);

		given(planRepository.findByIdAndUserId(1, 1)).willReturn(Optional.of(plan));
		given(planWeekAllocationRepository.findAllByPlanIdOrderByBudgetWeekWeekSequenceAsc(1)).willReturn(sevenYearAllocations(plan));

		PlanWeeklyPlansPageResponse response = service.listPlanWeeklyPlans(1, 1, 1, 3);

		assertThat(response.planId()).isEqualTo(1);
		assertThat(response.weeklyPlans()).extracting(PlanWeeklyPlansPageResponse.WeeklyPlan::week)
			.containsExactly(13, 14, 15);
		assertThat(response.page()).isEqualTo(1);
		assertThat(response.size()).isEqualTo(3);
		assertThat(response.totalElements()).isEqualTo(7);
		assertThat(response.totalPages()).isEqualTo(3);
		assertThat(response.hasNext()).isTrue();
	}

	@Test
	void listPlanWeeklyPlansThrowsWhenPageOrSizeIsInvalid() {
		assertThatThrownBy(() -> service.listPlanWeeklyPlans(1, 1, -1, 3))
			.isInstanceOf(PlanException.class);
		assertThatThrownBy(() -> service.listPlanWeeklyPlans(1, 1, 0, 0))
			.isInstanceOf(PlanException.class);
	}

	private List<PlanWeekAllocation> sevenYearAllocations(Plan plan) {
		return List.of(
			allocation(plan, budgetWeek(10, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 11)), 10_000L, 9_000L),
			allocation(plan, budgetWeek(11, LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 18)), 20_000L, 19_000L),
			allocation(plan, budgetWeek(12, LocalDate.of(2026, 1, 19), LocalDate.of(2026, 1, 25)), 30_000L, 29_000L),
			allocation(plan, budgetWeek(13, LocalDate.of(2026, 1, 26), LocalDate.of(2026, 2, 1)), 40_000L, 39_000L),
			allocation(plan, budgetWeek(14, LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 8)), 50_000L, 49_000L),
			allocation(plan, budgetWeek(15, LocalDate.of(2026, 2, 9), LocalDate.of(2026, 2, 15)), 60_000L, 59_000L),
			allocation(plan, budgetWeek(16, LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 22)), 70_000L, 69_000L)
		);
	}

	private Plan plan(
		int planId,
		int userId,
		LocalDate startDate,
		LocalDate endDate,
		long targetAmount,
		long savedAmount
	) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		Plan plan = Plan.builder()
			.user(user)
			.title("맥북 구매")
			.description("맥북 프로 14인치")
			.targetAmount(targetAmount)
			.savedAmount(savedAmount)
			.startDate(startDate)
			.endDate(endDate)
			.priority(1)
			.targetWeeks(21)
			.build();
		ReflectionTestUtils.setField(plan, "id", planId);
		return plan;
	}

	private BudgetWeek budgetWeek(int weekSequence, LocalDate startDate, LocalDate endDate) {
		BudgetWeek budgetWeek = BudgetWeek.create(null, User.createPendingSocialUser(), weekSequence, startDate, endDate);
		ReflectionTestUtils.setField(budgetWeek, "weekSequence", weekSequence);
		ReflectionTestUtils.setField(budgetWeek, "weekStartDate", startDate);
		ReflectionTestUtils.setField(budgetWeek, "weekEndDate", endDate);
		return budgetWeek;
	}

	private BudgetWeek currentBudgetWeek(int weekSequence) {
		BudgetWeek budgetWeek = BudgetWeek.create(null, User.createPendingSocialUser(), weekSequence,
			LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22));
		ReflectionTestUtils.setField(budgetWeek, "weekSequence", weekSequence);
		return budgetWeek;
	}

	private PlanWeekAllocation allocation(Plan plan, BudgetWeek budgetWeek, long plannedAmount, long finalAmount) {
		PlanWeekAllocation allocation = PlanWeekAllocation.builder()
			.plan(plan)
			.budgetWeek(budgetWeek)
			.plannedAmount(plannedAmount)
			.finalAmount(finalAmount)
			.build();
		ReflectionTestUtils.setField(allocation, "budgetWeek", budgetWeek);
		return allocation;
	}
}
