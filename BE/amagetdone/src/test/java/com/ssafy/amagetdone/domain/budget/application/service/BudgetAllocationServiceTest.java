package com.ssafy.amagetdone.domain.budget.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetMonthStatus;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BudgetAllocationServiceTest {

	@Mock
	private BudgetAdjustmentRepository budgetAdjustmentRepository;

	@Mock
	private BudgetMonthRepository budgetMonthRepository;

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private EssentialExpenseMonthRepository essentialExpenseMonthRepository;

	@Mock
	private EssentialWeekAllocationRepository essentialWeekAllocationRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	private BudgetAllocationService budgetAllocationService;

	@BeforeEach
	void setUp() {
		budgetAllocationService = new BudgetAllocationService(
			budgetAdjustmentRepository,
			budgetMonthRepository,
			budgetWeekRepository,
			essentialExpenseMonthRepository,
			essentialWeekAllocationRepository,
			planWeekAllocationRepository
		);

		Mockito.lenient().when(budgetMonthRepository.findFirstByUserAndBaseDateOrderByIdDesc(any(User.class), any(LocalDate.class)))
			.thenReturn(java.util.Optional.empty());
		Mockito.lenient().when(
			budgetWeekRepository.findByUserAndWeekStartDateBetweenOrderByWeekSequence(any(User.class), any(LocalDate.class), any(LocalDate.class))
		)
			.thenReturn(List.of());
		Mockito.lenient().when(
			budgetWeekRepository.findAllByUserIdAndWeekEndDateGreaterThanEqualAndWeekStartDateLessThanEqualOrderByWeekStartDateAscIdDesc(
				any(Integer.class),
				any(LocalDate.class),
				any(LocalDate.class)
			)
		)
			.thenReturn(List.of());
		Mockito.lenient().when(budgetWeekRepository.findFirstByUserOrderByWeekEndDateDesc(any(User.class)))
			.thenReturn(java.util.Optional.empty());
		Mockito.lenient().when(essentialWeekAllocationRepository.findByBudgetWeek(any(BudgetWeek.class))).thenReturn(List.of());
		Mockito.lenient().when(planWeekAllocationRepository.findByBudgetWeek(any(BudgetWeek.class))).thenReturn(List.of());
		Mockito.lenient().when(budgetMonthRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
		Mockito.lenient().when(budgetWeekRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void allocateBudgetCreatesPartialFirstWeekAndExpectedFreeBudgetForStandardCase() {
		User user = user(1);
		LocalDate serviceStartDate = LocalDate.of(2026, 3, 6);
		BudgetMonth marchBudgetMonth = budgetMonth(user, LocalDate.of(2026, 3, 1));
		EssentialExpenseMonth essential = essentialExpenseMonth(101, marchBudgetMonth, 1_450_000L);
		Plan emergencyFund = plan(1, user, "Emergency Fund", 1_000_000L, 8, 1, serviceStartDate);
		Plan carFund = plan(2, user, "Car Fund", 5_000_000L, 24, 2, serviceStartDate);

		BudgetAllocationService.AllocationResult result = budgetAllocationService.allocateBudget(
			user,
			serviceStartDate,
			3_100_000L,
			List.of(essential),
			List.of(emergencyFund, carFund)
		);

		BudgetWeek week1 = budgetWeek(result.budgetWeeks(), 1);
		BudgetWeek week2 = budgetWeek(result.budgetWeeks(), 2);

		assertEquals(LocalDate.of(2026, 3, 6), week1.getWeekStartDate());
		assertEquals(LocalDate.of(2026, 3, 8), week1.getWeekEndDate());
		assertEquals(3, week1.getPeriodDays());
		assertTrue(week1.isPartial());
		assertEquals(300_000L, week1.getPlannedIncomeAmount());
		assertEquals(140_323L, week1.getPlannedEssentialAmount());
		assertEquals(142_857L, week1.getPlannedSavingAmount());
		assertTrue(Math.abs(week1.getFinalFreeAmount() - 16_820L) <= 1L);

		assertEquals(700_000L, week2.getPlannedIncomeAmount());
		assertEquals(327_419L, week2.getPlannedEssentialAmount());
		assertEquals(333_334L, week2.getPlannedSavingAmount());
		assertTrue(Math.abs(week2.getFinalFreeAmount() - 39_247L) <= 1L);

		assertEquals(53_571L, planAllocation(result.planAllocations(), 1, "Emergency Fund").getPlannedAmount());
		assertEquals(89_286L, planAllocation(result.planAllocations(), 1, "Car Fund").getPlannedAmount());
		assertEquals(125_000L, planAllocation(result.planAllocations(), 2, "Emergency Fund").getPlannedAmount());
		assertEquals(208_334L, planAllocation(result.planAllocations(), 2, "Car Fund").getPlannedAmount());
	}

	@Test
	void allocateBudgetDeductsShortageFromLowerPriorityPlanFirstAndTracksExtraWeeks() {
		User user = user(1);
		LocalDate serviceStartDate = LocalDate.of(2026, 3, 2);
		BudgetMonth marchBudgetMonth = budgetMonth(user, LocalDate.of(2026, 3, 1));
		EssentialExpenseMonth essential = essentialExpenseMonth(201, marchBudgetMonth, 1_000_000L);
		Plan highPriorityPlan = plan(1, user, "High Priority", 140_000L, 2, 1, serviceStartDate);
		Plan lowPriorityPlan = plan(2, user, "Low Priority", 200_000L, 2, 5, serviceStartDate);

		BudgetAllocationService.AllocationResult result = budgetAllocationService.allocateBudget(
			user,
			serviceStartDate,
			1_530_000L,
			List.of(essential),
			List.of(highPriorityPlan, lowPriorityPlan)
		);

		BudgetWeek week1 = budgetWeek(result.budgetWeeks(), 1);
		BudgetWeek week2 = budgetWeek(result.budgetWeeks(), 2);
		PlanWeekAllocation highPriorityWeek1 = planAllocation(result.planAllocations(), 1, "High Priority");
		PlanWeekAllocation lowPriorityWeek1 = planAllocation(result.planAllocations(), 1, "Low Priority");
		PlanWeekAllocation highPriorityWeek2 = planAllocation(result.planAllocations(), 2, "High Priority");
		PlanWeekAllocation lowPriorityWeek2 = planAllocation(result.planAllocations(), 2, "Low Priority");
		BudgetAllocationService.PlanShortageSummary highPrioritySummary = shortageSummary(
			result.planShortageSummaries(),
			"High Priority"
		);
		BudgetAllocationService.PlanShortageSummary lowPrioritySummary = shortageSummary(
			result.planShortageSummaries(),
			"Low Priority"
		);

		assertEquals(70_000L, highPriorityWeek1.getPlannedAmount());
		assertEquals(70_000L, highPriorityWeek1.getFinalAmount());
		assertEquals(100_000L, lowPriorityWeek1.getPlannedAmount());
		assertEquals(49_677L, lowPriorityWeek1.getFinalAmount());
		assertEquals(0L, week1.getFinalFreeAmount());

		assertEquals(70_000L, highPriorityWeek2.getPlannedAmount());
		assertEquals(70_000L, highPriorityWeek2.getFinalAmount());
		assertEquals(100_000L, lowPriorityWeek2.getPlannedAmount());
		assertEquals(49_677L, lowPriorityWeek2.getFinalAmount());
		assertEquals(0L, week2.getFinalFreeAmount());

		assertEquals(70_000L, highPrioritySummary.baseWeeklyAmount());
		assertEquals(0L, highPrioritySummary.deductedAmount());
		assertEquals(0, highPrioritySummary.extraWeeks());

		assertEquals(100_000L, lowPrioritySummary.baseWeeklyAmount());
		assertEquals(100_646L, lowPrioritySummary.deductedAmount());
		assertEquals(2, lowPrioritySummary.extraWeeks());
	}

	@Test
	void allocateBudgetFillsHigherPriorityPlansFirstWhenWeeklyFreeBudgetIsInsufficient() {
		User user = user(1);
		LocalDate serviceStartDate = LocalDate.of(2026, 3, 2);
		BudgetMonth marchBudgetMonth = budgetMonth(user, LocalDate.of(2026, 3, 1));
		EssentialExpenseMonth essential = essentialExpenseMonth(250, marchBudgetMonth, 1_000_000L);
		Plan topPriorityPlan = plan(10, user, "Top Priority", 140_000L, 2, 1, serviceStartDate);
		Plan middlePriorityPlan = plan(11, user, "Middle Priority", 120_000L, 2, 2, serviceStartDate);
		Plan lowPriorityPlan = plan(12, user, "Low Priority", 100_000L, 2, 3, serviceStartDate);

		BudgetAllocationService.AllocationResult result = budgetAllocationService.allocateBudget(
			user,
			serviceStartDate,
			1_530_000L,
			List.of(essential),
			List.of(topPriorityPlan, middlePriorityPlan, lowPriorityPlan)
		);

		assertEquals(70_000L, planAllocation(result.planAllocations(), 1, "Top Priority").getFinalAmount());
		assertEquals(49_677L, planAllocation(result.planAllocations(), 1, "Middle Priority").getFinalAmount());
		assertEquals(0L, planAllocation(result.planAllocations(), 1, "Low Priority").getFinalAmount());
		assertEquals(70_000L, planAllocation(result.planAllocations(), 2, "Top Priority").getFinalAmount());
		assertEquals(49_677L, planAllocation(result.planAllocations(), 2, "Middle Priority").getFinalAmount());
		assertEquals(0L, planAllocation(result.planAllocations(), 2, "Low Priority").getFinalAmount());
	}

	@Test
	void allocateBudgetWithNoPlansLeavesSavingAtZeroAndFreeBudgetAsIncomeMinusEssential() {
		User user = user(1);
		LocalDate serviceStartDate = LocalDate.of(2026, 3, 6);
		BudgetMonth marchBudgetMonth = budgetMonth(user, LocalDate.of(2026, 3, 1));
		EssentialExpenseMonth essential = essentialExpenseMonth(301, marchBudgetMonth, 1_450_000L);

		BudgetAllocationService.AllocationResult result = budgetAllocationService.allocateBudget(
			user,
			serviceStartDate,
			3_100_000L,
			List.of(essential),
			List.of()
		);

		assertTrue(result.planAllocations().isEmpty());
		assertTrue(result.planShortageSummaries().isEmpty());

		for (BudgetWeek budgetWeek : result.budgetWeeks()) {
			assertEquals(0L, budgetWeek.getPlannedSavingAmount());
			assertEquals(
				budgetWeek.getPlannedIncomeAmount() - budgetWeek.getPlannedEssentialAmount(),
				budgetWeek.getFinalFreeAmount()
			);
		}
	}

	@Test
	void allocateBudgetProratesIncomeAndEssentialAmountsAcrossMonthBoundaryWeek() {
		User user = user(1);
		LocalDate serviceStartDate = LocalDate.of(2026, 3, 30);
		BudgetMonth marchBudgetMonth = budgetMonth(user, LocalDate.of(2026, 3, 1));
		BudgetMonth aprilBudgetMonth = budgetMonth(user, LocalDate.of(2026, 4, 1));
		EssentialExpenseMonth marchEssential = essentialExpenseMonth(401, marchBudgetMonth, 310_000L);
		EssentialExpenseMonth aprilEssential = essentialExpenseMonth(402, aprilBudgetMonth, 300_000L);

		BudgetAllocationService.AllocationResult result = budgetAllocationService.allocateBudget(
			user,
			serviceStartDate,
			930_000L,
			List.of(marchEssential, aprilEssential),
			List.of()
		);

		BudgetWeek week1 = budgetWeek(result.budgetWeeks(), 1);
		BudgetWeek week5 = budgetWeek(result.budgetWeeks(), 5);

		assertEquals(LocalDate.of(2026, 3, 30), week1.getWeekStartDate());
		assertEquals(LocalDate.of(2026, 4, 5), week1.getWeekEndDate());
		assertEquals(215_000L, week1.getPlannedIncomeAmount());
		assertEquals(70_000L, week1.getPlannedEssentialAmount());
		assertEquals(145_000L, week1.getFinalFreeAmount());

		assertEquals(LocalDate.of(2026, 4, 27), week5.getWeekStartDate());
		assertEquals(LocalDate.of(2026, 4, 30), week5.getWeekEndDate());
		assertEquals(4, week5.getPeriodDays());
		assertTrue(week5.isPartial());
		assertEquals(124_000L, week5.getPlannedIncomeAmount());
		assertEquals(40_000L, week5.getPlannedEssentialAmount());
		assertEquals(84_000L, week5.getFinalFreeAmount());

		assertEquals(990_000L, result.budgetWeeks().stream().mapToLong(BudgetWeek::getPlannedIncomeAmount).sum());
		assertEquals(320_000L, result.budgetWeeks().stream().mapToLong(BudgetWeek::getPlannedEssentialAmount).sum());
	}

	@Test
	void allocateBudgetConservesShortPlanAcrossPartialWeeks() {
		User user = user(1);
		LocalDate serviceStartDate = LocalDate.of(2026, 3, 4);
		BudgetMonth marchBudgetMonth = budgetMonth(user, LocalDate.of(2026, 3, 1));
		EssentialExpenseMonth essential = essentialExpenseMonth(501, marchBudgetMonth, 62_000L);
		Plan shortPlan = plan(3, user, "Short Plan", 140_000L, 2, 1, serviceStartDate);

		BudgetAllocationService.AllocationResult result = budgetAllocationService.allocateBudget(
			user,
			serviceStartDate,
			620_000L,
			List.of(essential),
			List.of(shortPlan)
		);

		BudgetWeek week1 = budgetWeek(result.budgetWeeks(), 1);
		BudgetWeek week3 = budgetWeek(result.budgetWeeks(), 3);
		BudgetWeek week4 = budgetWeek(result.budgetWeeks(), 4);
		BudgetWeek week5 = budgetWeek(result.budgetWeeks(), 5);

		assertEquals(5, week1.getPeriodDays());
		assertTrue(week1.isPartial());
		assertEquals(50_000L, planAllocation(result.planAllocations(), 1, "Short Plan").getPlannedAmount());
		assertEquals(70_000L, planAllocation(result.planAllocations(), 2, "Short Plan").getPlannedAmount());
		assertEquals(20_000L, planAllocation(result.planAllocations(), 3, "Short Plan").getPlannedAmount());
		assertEquals(40_000L, week1.getFinalFreeAmount());
		assertEquals(106_000L, week3.getFinalFreeAmount());
		assertEquals(0L, week4.getPlannedSavingAmount());
		assertEquals(2, week5.getPeriodDays());
		assertTrue(week5.isPartial());

		assertEquals(140_000L, result.planAllocations().stream().mapToLong(PlanWeekAllocation::getPlannedAmount).sum());
		assertEquals(140_000L, result.planAllocations().stream().mapToLong(PlanWeekAllocation::getFinalAmount).sum());
		assertEquals(560_000L, result.budgetWeeks().stream().mapToLong(BudgetWeek::getPlannedIncomeAmount).sum());
		assertEquals(56_000L, result.budgetWeeks().stream().mapToLong(BudgetWeek::getPlannedEssentialAmount).sum());
	}

	@Test
	void allocateBudgetRenumbersOverlappingWeeksAndDeletesLegacyFrames() {
		User user = user(1);
		LocalDate serviceStartDate = LocalDate.of(2026, 3, 1);
		LocalDate allocationEndDate = LocalDate.of(2026, 3, 31);
		BudgetWeek crossingMonthWeek = existingBudgetWeek(100, user, 10, LocalDate.of(2026, 2, 23), LocalDate.of(2026, 3, 1));
		BudgetWeek week2 = existingBudgetWeek(101, user, 11, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8));
		BudgetWeek week3 = existingBudgetWeek(102, user, 12, LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 15));
		BudgetWeek week4 = existingBudgetWeek(103, user, 13, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22));
		BudgetWeek week5 = existingBudgetWeek(104, user, 14, LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 29));
		BudgetWeek staleFutureWeek = existingBudgetWeek(105, user, 6, LocalDate.of(2026, 3, 30), LocalDate.of(2026, 3, 31));

		Mockito.when(
			budgetWeekRepository.findAllByUserIdAndWeekEndDateGreaterThanEqualAndWeekStartDateLessThanEqualOrderByWeekStartDateAscIdDesc(
				eq(user.getId()),
				eq(serviceStartDate),
				eq(allocationEndDate)
			)
		).thenReturn(List.of(crossingMonthWeek, week2, week3, week4, week5, staleFutureWeek));

		BudgetAllocationService.AllocationResult result = budgetAllocationService.allocateBudget(
			user,
			serviceStartDate,
			0L,
			List.of(),
			List.of(),
			allocationEndDate
		);

		assertEquals(6, result.budgetWeeks().size());
		assertBudgetWeek(result.budgetWeeks(), LocalDate.of(2026, 3, 1), 10);
		assertBudgetWeek(result.budgetWeeks(), LocalDate.of(2026, 3, 2), 11);
		assertBudgetWeek(result.budgetWeeks(), LocalDate.of(2026, 3, 9), 12);
		assertBudgetWeek(result.budgetWeeks(), LocalDate.of(2026, 3, 16), 13);
		assertBudgetWeek(result.budgetWeeks(), LocalDate.of(2026, 3, 23), 14);
		assertBudgetWeek(result.budgetWeeks(), LocalDate.of(2026, 3, 30), 15);

		@SuppressWarnings("unchecked")
		org.mockito.ArgumentCaptor<Iterable<? extends BudgetWeek>> captor =
			org.mockito.ArgumentCaptor.forClass(Iterable.class);
		Mockito.verify(budgetWeekRepository).deleteAll(captor.capture());
		List<Integer> deletedIds = new java.util.ArrayList<>();
		captor.getValue().forEach(week -> deletedIds.add(week.getId()));
		assertEquals(List.of(100), deletedIds);
	}

	private User user(int userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}

	private BudgetMonth budgetMonth(User user, LocalDate baseDate) {
		return BudgetMonth.builder()
			.id(baseDate.getMonthValue())
			.user(user)
			.baseDate(baseDate)
			.status(BudgetMonthStatus.OPEN)
			.build();
	}

	private EssentialExpenseMonth essentialExpenseMonth(int id, BudgetMonth budgetMonth, long amount) {
		return EssentialExpenseMonth.builder()
			.id(id)
			.category(Category.create(id, "Essential " + id))
			.budgetMonth(budgetMonth)
			.expenseScope("FIXED")
			.amount(amount)
			.build();
	}

	private Plan plan(
		int id,
		User user,
		String title,
		long targetAmount,
		int targetWeeks,
		int priority,
		LocalDate startDate
	) {
		return Plan.builder()
			.id(id)
			.user(user)
			.title(title)
			.description(title)
			.targetAmount(targetAmount)
			.savedAmount(0L)
			.startDate(startDate)
			.targetWeeks(targetWeeks)
			.priority(priority)
			.autoAllocateEnabled(true)
			.endDate(startDate.plusDays((long)targetWeeks * 7L - 1L))
			.build();
	}

	private BudgetWeek budgetWeek(List<BudgetWeek> budgetWeeks, int weekSequence) {
		return budgetWeeks.stream()
			.filter(budgetWeek -> budgetWeek.getWeekSequence() == weekSequence)
			.findFirst()
			.orElseThrow();
	}

	private void assertBudgetWeek(List<BudgetWeek> budgetWeeks, LocalDate startDate, int weekSequence) {
		BudgetWeek budgetWeek = budgetWeeks.stream()
			.filter(candidate -> candidate.getWeekStartDate().equals(startDate))
			.findFirst()
			.orElseThrow();
		assertEquals(weekSequence, budgetWeek.getWeekSequence());
	}

	private PlanWeekAllocation planAllocation(List<PlanWeekAllocation> allocations, int weekSequence, String title) {
		return allocations.stream()
			.filter(allocation -> allocation.getBudgetWeek().getWeekSequence() == weekSequence)
			.filter(allocation -> allocation.getPlan().getTitle().equals(title))
			.findFirst()
			.orElseThrow();
	}

	private BudgetAllocationService.PlanShortageSummary shortageSummary(
		List<BudgetAllocationService.PlanShortageSummary> summaries,
		String title
	) {
		return summaries.stream()
			.filter(summary -> summary.plan().getTitle().equals(title))
			.findFirst()
			.orElseThrow();
	}

	private BudgetWeek existingBudgetWeek(int id, User user, int weekSequence, LocalDate startDate, LocalDate endDate) {
		return BudgetWeek.builder()
			.id(id)
			.user(user)
			.weekSequence(weekSequence)
			.weekStartDate(startDate)
			.weekEndDate(endDate)
			.periodDays((int)(java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1L))
			.partial(true)
			.status(com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus.PLANNED)
			.build();
	}
}
