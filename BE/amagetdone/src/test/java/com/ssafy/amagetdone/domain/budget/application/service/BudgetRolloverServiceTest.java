package com.ssafy.amagetdone.domain.budget.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetRollover;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetRolloverRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetRolloverStatus;
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
class BudgetRolloverServiceTest {

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private BudgetRolloverRepository budgetRolloverRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Mock
	private BudgetAdjustmentRepository budgetAdjustmentRepository;

	private final List<BudgetRollover> savedRollovers = new ArrayList<>();
	private final List<BudgetAdjustment> savedAdjustments = new ArrayList<>();
	private final Map<BudgetWeek, List<PlanWeekAllocation>> allocationsByWeek = new HashMap<>();

	private BudgetRolloverService budgetRolloverService;

	@BeforeEach
	void setUp() {
		budgetRolloverService = new BudgetRolloverService(
			budgetWeekRepository,
			budgetRolloverRepository,
			planWeekAllocationRepository,
			budgetAdjustmentRepository
		);

		Mockito.lenient().when(budgetWeekRepository.save(any(BudgetWeek.class))).thenAnswer(invocation -> invocation.getArgument(0));
		Mockito.lenient().when(planWeekAllocationRepository.findByBudgetWeek(any(BudgetWeek.class)))
			.thenAnswer(invocation -> allocationsByWeek.getOrDefault(invocation.getArgument(0), List.of()));
		Mockito.lenient().when(planWeekAllocationRepository.saveAll(anyList()))
			.thenAnswer(invocation -> invocation.getArgument(0));
		Mockito.lenient().when(budgetAdjustmentRepository.save(any(BudgetAdjustment.class))).thenAnswer(invocation -> {
			BudgetAdjustment adjustment = invocation.getArgument(0);
			savedAdjustments.add(adjustment);
			return adjustment;
		});
		Mockito.lenient().when(budgetRolloverRepository.save(any(BudgetRollover.class))).thenAnswer(invocation -> {
			BudgetRollover rollover = invocation.getArgument(0);
			if (!savedRollovers.contains(rollover)) {
				savedRollovers.add(rollover);
			}
			return rollover;
		});
		Mockito.lenient().when(budgetRolloverRepository.findFirstByTargetBudgetWeekOrderByCreatedAtDesc(any(BudgetWeek.class)))
			.thenAnswer(invocation -> savedRollovers.stream()
				.filter(rollover -> rollover.getTargetBudgetWeek() == invocation.getArgument(0))
				.reduce((first, second) -> second));
	}

	@Test
	void executeRolloverDoesNothingWhenSourceWeekHasNoExcess() {
		BudgetWeek sourceWeek = budgetWeek(user(1), 1, 200L, 180L, 0L, 0L, 120L, 120L);

		budgetRolloverService.executeRollover(sourceWeek);

		verifyNoInteractions(budgetWeekRepository, budgetRolloverRepository, planWeekAllocationRepository, budgetAdjustmentRepository);
	}

	@Test
	void executeRolloverAppliesExcessToNextWeekAndKeepsAppliedStatusWhenFreeBudgetRemains() {
		User user = user(1);
		BudgetWeek sourceWeek = budgetWeek(user, 1, 0L, 0L, 0L, 0L, 100L, 150L);
		BudgetWeek nextWeek = budgetWeek(user, 2, 500L, 100L, 0L, 0L, 0L, 0L);
		PlanWeekAllocation vacationAllocation = allocation(nextWeek, plan(user, 10, "Vacation", 2), 150L);
		allocationsByWeek.put(nextWeek, new ArrayList<>(List.of(vacationAllocation)));

		Mockito.when(budgetWeekRepository.findFirstByUserAndWeekSequenceGreaterThanOrderByWeekSequenceAsc(eq(user), eq(1)))
			.thenReturn(Optional.of(nextWeek));

		budgetRolloverService.executeRollover(sourceWeek);

		assertEquals(50L, nextWeek.getRolloverInAmount());
		assertEquals(200L, nextWeek.getFinalFreeAmount());

		BudgetRollover savedRollover = savedRollovers.get(0);
		assertEquals(sourceWeek, savedRollover.getSourceBudgetWeek());
		assertEquals(nextWeek, savedRollover.getTargetBudgetWeek());
		assertEquals(1, savedRollover.getRolloverStep());
		assertEquals(50L, savedRollover.getRolloverAmount());
		assertEquals(BudgetRolloverStatus.APPLIED, savedRollover.getStatus());
		assertNotNull(savedRollover.getResolvedAt());
		assertEquals(0, savedAdjustments.size());
	}

	@Test
	void executeRolloverDeductsLowerPriorityPlansWhenStepFourWouldOverflow() {
		User user = user(1);
		BudgetWeek sourceWeek = budgetWeek(user, 1, 0L, 0L, 0L, 0L, 100L, 150L);
		BudgetWeek week2 = budgetWeek(user, 2, 50L, 100L, 0L, 0L, 0L, 0L);
		BudgetWeek week3 = budgetWeek(user, 3, 70L, 100L, 0L, 0L, 0L, 0L);
		BudgetWeek week4 = budgetWeek(user, 4, 100L, 100L, 0L, 0L, 0L, 0L);
		BudgetWeek week5 = budgetWeek(user, 5, 200L, 50L, 0L, 0L, 0L, 0L);

		PlanWeekAllocation lowPriorityAllocation = allocation(week5, plan(user, 20, "Low Priority", 9), 20L);
		PlanWeekAllocation highPriorityAllocation = allocation(week5, plan(user, 21, "High Priority", 3), 50L);
		allocationsByWeek.put(week2, List.of());
		allocationsByWeek.put(week3, List.of());
		allocationsByWeek.put(week4, List.of());
		allocationsByWeek.put(week5, new ArrayList<>(List.of(lowPriorityAllocation, highPriorityAllocation)));

		Mockito.when(budgetWeekRepository.findFirstByUserAndWeekSequenceGreaterThanOrderByWeekSequenceAsc(eq(user), anyInt()))
			.thenAnswer(invocation -> switch ((Integer)invocation.getArgument(1)) {
				case 1 -> Optional.of(week2);
				case 2 -> Optional.of(week3);
				case 3 -> Optional.of(week4);
				case 4 -> Optional.of(week5);
				default -> Optional.empty();
			});

		budgetRolloverService.executeRollover(sourceWeek);

		assertEquals(50L, week2.getRolloverInAmount());
		assertEquals(-100L, week2.getFinalFreeAmount());
		assertEquals(100L, week3.getRolloverInAmount());
		assertEquals(-130L, week3.getFinalFreeAmount());
		assertEquals(130L, week4.getRolloverInAmount());
		assertEquals(-130L, week4.getFinalFreeAmount());
		assertEquals(130L, week5.getRolloverInAmount());
		assertEquals(0L, week5.getFinalFreeAmount());

		assertEquals(4, savedRollovers.size());
		assertEquals(BudgetRolloverStatus.EXHAUSTED, rolloverForTarget(week2).getStatus());
		assertEquals(BudgetRolloverStatus.EXHAUSTED, rolloverForTarget(week3).getStatus());
		assertEquals(BudgetRolloverStatus.EXHAUSTED, rolloverForTarget(week4).getStatus());
		assertEquals(BudgetRolloverStatus.PLAN_DEDUCTED, rolloverForTarget(week5).getStatus());

		assertEquals(0L, lowPriorityAllocation.getFinalAmount());
		assertEquals(20L, highPriorityAllocation.getFinalAmount());
		assertEquals(2, savedAdjustments.size());
		assertEquals(BudgetAdjustmentType.ROLLOVER_APPLIED, savedAdjustments.get(0).getAdjustmentType());
		assertEquals(lowPriorityAllocation.getPlan(), savedAdjustments.get(0).getPlan());
		assertEquals(20L, savedAdjustments.get(0).getAdjustmentAmount());
		assertEquals(BudgetAdjustmentType.ROLLOVER_APPLIED, savedAdjustments.get(1).getAdjustmentType());
		assertEquals(highPriorityAllocation.getPlan(), savedAdjustments.get(1).getPlan());
		assertEquals(30L, savedAdjustments.get(1).getAdjustmentAmount());
	}

	private User user(int userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}

	private BudgetWeek budgetWeek(
		User user,
		int weekSequence,
		long plannedIncomeAmount,
		long plannedEssentialAmount,
		long manualAdjustmentAmount,
		long rolloverInAmount,
		long finalFreeAmount,
		long actualSpentAmount
	) {
		return BudgetWeek.builder()
			.user(user)
			.weekSequence(weekSequence)
			.weekStartDate(LocalDate.of(2026, 3, 3).plusWeeks(weekSequence - 1L))
			.weekEndDate(LocalDate.of(2026, 3, 9).plusWeeks(weekSequence - 1L))
			.plannedIncomeAmount(plannedIncomeAmount)
			.plannedEssentialAmount(plannedEssentialAmount)
			.manualAdjustmentAmount(manualAdjustmentAmount)
			.rolloverInAmount(rolloverInAmount)
			.finalFreeAmount(finalFreeAmount)
			.actualSpentAmount(actualSpentAmount)
			.build();
	}

	private Plan plan(User user, int id, String title, int priority) {
		return Plan.builder()
			.id(id)
			.user(user)
			.title(title)
			.description(title)
			.targetAmount(100_000L)
			.savedAmount(0L)
			.startDate(LocalDate.of(2026, 3, 3))
			.targetWeeks(4)
			.priority(priority)
			.endDate(LocalDate.of(2026, 3, 31))
			.build();
	}

	private PlanWeekAllocation allocation(BudgetWeek budgetWeek, Plan plan, long finalAmount) {
		return PlanWeekAllocation.builder()
			.budgetWeek(budgetWeek)
			.plan(plan)
			.plannedAmount(finalAmount)
			.finalAmount(finalAmount)
			.build();
	}

	private BudgetRollover rolloverForTarget(BudgetWeek budgetWeek) {
		return savedRollovers.stream()
			.filter(rollover -> rollover.getTargetBudgetWeek() == budgetWeek)
			.findFirst()
			.orElseThrow();
	}
}
