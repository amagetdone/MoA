package com.ssafy.amagetdone.domain.plan.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateResponse;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlanCreationBudgetSummaryServiceTest {

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private EssentialWeekAllocationRepository essentialWeekAllocationRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Test
	void usesActualPlanAllocationForWeeklyMissionAndSavingAmountWhenPresent() {
		Clock clock = Clock.fixed(Instant.parse("2026-03-19T00:00:00Z"), ZoneId.of("Asia/Seoul"));
		PlanCreationBudgetSummaryService service = new PlanCreationBudgetSummaryService(
			budgetWeekRepository,
			essentialWeekAllocationRepository,
			planWeekAllocationRepository,
			clock
		);

		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 1);

		Plan plan = Plan.create(
			user,
			"유럽여행",
			"여행 자금 모으기",
			7_200_000L,
			LocalDate.of(2026, 3, 19),
			LocalDate.of(2026, 8, 31),
			1
		);
		ReflectionTestUtils.setField(plan, "id", 24);
		plan.setStatus(PlanStatus.ACTIVE);

		BudgetWeek budgetWeek = BudgetWeek.builder()
			.user(user)
			.weekSequence(12)
			.weekStartDate(LocalDate.of(2026, 3, 16))
			.weekEndDate(LocalDate.of(2026, 3, 22))
			.plannedIncomeAmount(1_500_000L)
			.plannedSavingAmount(0L)
			.finalFreeAmount(200_000L)
			.build();

		given(budgetWeekRepository.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(
			1,
			LocalDate.of(2026, 3, 19),
			LocalDate.of(2026, 3, 19)
		)).willReturn(Optional.of(budgetWeek));

		given(essentialWeekAllocationRepository.findByBudgetWeek(budgetWeek)).willReturn(List.of(
			allocation(budgetWeek, 1, "주거비", ExpenseScope.FIXED, 350_000L),
			allocation(budgetWeek, 2, "통신비", ExpenseScope.FIXED, 200_000L),
			allocation(budgetWeek, 3, "구독비", ExpenseScope.FIXED, 150_000L),
			allocation(budgetWeek, 4, "식비", ExpenseScope.VARIABLE, 180_000L),
			allocation(budgetWeek, 5, "여가비", ExpenseScope.VARIABLE, 70_000L),
			allocation(budgetWeek, 6, "생활비", ExpenseScope.VARIABLE, 50_000L)
		));

		given(planWeekAllocationRepository.findByBudgetWeek(budgetWeek)).willReturn(List.of(
			PlanWeekAllocation.builder()
				.budgetWeek(budgetWeek)
				.plan(plan)
				.plannedAmount(300_000L)
				.finalAmount(300_000L)
				.build()
		));

		PlanCreateResponse response = service.build(plan);

		assertEquals(24, response.planId());
		assertEquals("유럽여행", response.planTitle());
		assertEquals(300_000L, response.weeklyMission().weeklySavingsAmount());
		assertEquals(24, response.weeklyMission().weeksToGoal());
		assertEquals(1_500_000L, response.weeklyBudget().totalIncomeAmount());
		assertEquals(300_000L, response.weeklyBudget().savingAmount());
		assertEquals(700_000L, response.weeklyBudget().fixedExpense().totalAmount());
		assertEquals("주거비", response.weeklyBudget().fixedExpense().items().get(0).category());
		assertEquals(300_000L, response.weeklyBudget().variableExpense().totalAmount());
		assertEquals("식비", response.weeklyBudget().variableExpense().items().get(0).category());
		assertEquals(200_000L, response.weeklyBudget().freeAmount());
	}

	@Test
	void usesSameFallbackWeeklySavingAmountForMissionAndBudgetWhenAllocationIsMissing() {
		Clock clock = Clock.fixed(Instant.parse("2026-03-19T00:00:00Z"), ZoneId.of("Asia/Seoul"));
		PlanCreationBudgetSummaryService service = new PlanCreationBudgetSummaryService(
			budgetWeekRepository,
			essentialWeekAllocationRepository,
			planWeekAllocationRepository,
			clock
		);

		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 1);

		Plan plan = Plan.create(
			user,
			"유럽여행",
			"여행 자금 모으기",
			7_200_000L,
			LocalDate.of(2026, 3, 19),
			LocalDate.of(2026, 8, 31),
			1
		);
		ReflectionTestUtils.setField(plan, "id", 24);
		plan.setStatus(PlanStatus.ACTIVE);

		BudgetWeek budgetWeek = BudgetWeek.builder()
			.user(user)
			.weekSequence(12)
			.weekStartDate(LocalDate.of(2026, 3, 16))
			.weekEndDate(LocalDate.of(2026, 3, 22))
			.plannedIncomeAmount(1_500_000L)
			.plannedSavingAmount(0L)
			.finalFreeAmount(200_000L)
			.build();

		given(budgetWeekRepository.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(
			1,
			LocalDate.of(2026, 3, 19),
			LocalDate.of(2026, 3, 19)
		)).willReturn(Optional.of(budgetWeek));

		given(essentialWeekAllocationRepository.findByBudgetWeek(budgetWeek)).willReturn(List.of());
		given(planWeekAllocationRepository.findByBudgetWeek(budgetWeek)).willReturn(List.of());

		PlanCreateResponse response = service.build(plan);

		assertEquals(300_000L, response.weeklyMission().weeklySavingsAmount());
		assertEquals(300_000L, response.weeklyBudget().savingAmount());
	}

	@Test
	void usesSameFallbackWeeklySavingAmountForMissionAndBudgetWhenSummaryWeekIsMissing() {
		Clock clock = Clock.fixed(Instant.parse("2026-03-19T00:00:00Z"), ZoneId.of("Asia/Seoul"));
		PlanCreationBudgetSummaryService service = new PlanCreationBudgetSummaryService(
			budgetWeekRepository,
			essentialWeekAllocationRepository,
			planWeekAllocationRepository,
			clock
		);

		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 1);

		Plan plan = Plan.create(
			user,
			"유럽여행",
			"여행 자금 모으기",
			7_200_000L,
			LocalDate.of(2026, 3, 19),
			LocalDate.of(2026, 8, 31),
			1
		);
		ReflectionTestUtils.setField(plan, "id", 24);
		plan.setStatus(PlanStatus.ACTIVE);

		given(budgetWeekRepository.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(
			1,
			LocalDate.of(2026, 3, 19),
			LocalDate.of(2026, 3, 19)
		)).willReturn(Optional.empty());
		given(budgetWeekRepository.findFirstByUserOrderByWeekEndDateDesc(user)).willReturn(Optional.empty());

		PlanCreateResponse response = service.build(plan);

		assertEquals(300_000L, response.weeklyMission().weeklySavingsAmount());
		assertEquals(300_000L, response.weeklyBudget().savingAmount());
	}

	private EssentialWeekAllocation allocation(
		BudgetWeek budgetWeek,
		int categoryId,
		String categoryName,
		ExpenseScope expenseScope,
		long plannedAmount
	) {
		return EssentialWeekAllocation.builder()
			.budgetWeek(budgetWeek)
			.category(Category.create(categoryId, categoryName))
			.expenseScope(expenseScope)
			.plannedAmount(plannedAmount)
			.actualSpentAmount(0L)
			.build();
	}
}
