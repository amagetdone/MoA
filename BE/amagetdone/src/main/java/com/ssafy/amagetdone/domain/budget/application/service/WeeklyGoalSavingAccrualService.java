package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.plan.application.service.PlanPriorityService;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WeeklyGoalSavingAccrualService {

	private static final String WEEKLY_ACCRUAL_REASON = "Weekly goal saving accrual applied";

	private final BudgetWeekRepository budgetWeekRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetAdjustmentRepository budgetAdjustmentRepository;
	private final PlanRepository planRepository;
	private final PlanPriorityService planPriorityService;
	private final Clock clock;

	public void applyWeeklyAccrual(Integer userId) {
		LocalDate today = LocalDate.now(clock);
		BudgetWeek currentWeek = findCurrentWeek(userId, today);
		if (currentWeek == null) {
			return;
		}

		List<PlanWeekAllocation> unaccrued = findUnaccruedAllocations(currentWeek);
		if (unaccrued.isEmpty()) {
			return;
		}

		long pool = calculatePool(currentWeek);
		applyAccruals(unaccrued, pool, currentWeek);
	}

	public void applyImmediateAccrual(Integer userId, Integer planId) {
		LocalDate today = LocalDate.now(clock);
		BudgetWeek currentWeek = findCurrentWeek(userId, today);
		if (currentWeek == null) {
			return;
		}

		List<PlanWeekAllocation> unaccrued = findUnaccruedAllocations(currentWeek).stream()
			.filter(a -> a.getPlan() != null && Objects.equals(a.getPlan().getId(), planId))
			.toList();
		if (unaccrued.isEmpty()) {
			return;
		}

		long pool = calculatePool(currentWeek);
		applyAccruals(unaccrued, pool, currentWeek);
	}

	private BudgetWeek findCurrentWeek(Integer userId, LocalDate today) {
		return budgetWeekRepository
			.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(userId, today, today)
			.orElse(null);
	}

	private List<PlanWeekAllocation> findUnaccruedAllocations(BudgetWeek budgetWeek) {
		return planWeekAllocationRepository.findByBudgetWeekAndActualAmountIsNull(budgetWeek).stream()
			.filter(a -> a.getPlan() != null)
			.filter(a -> a.getPlan().getStatus() == PlanStatus.ACTIVE)
			.sorted(Comparator.comparing((PlanWeekAllocation a) -> a.getPlan().getPriority(),
					Comparator.nullsLast(Integer::compareTo))
				.thenComparing(a -> a.getPlan().getId(), Comparator.nullsLast(Integer::compareTo)))
			.toList();
	}

	private long calculatePool(BudgetWeek week) {
		long planSavedAmount = sumPlanSavedAmount(week);
		long totalBudget = zero(week.getPlannedIncomeAmount())
			- zero(week.getPlannedEssentialAmount())
			- zero(week.getRolloverInAmount())
			+ zero(week.getManualAdjustmentAmount())
			- planSavedAmount;

		long alreadyAccrued = planWeekAllocationRepository.findByBudgetWeek(week).stream()
			.filter(a -> a.getActualAmount() != null)
			.mapToLong(a -> zero(a.getActualAmount()))
			.sum();

		return Math.max(totalBudget - alreadyAccrued, 0L);
	}

	private long sumPlanSavedAmount(BudgetWeek budgetWeek) {
		return budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(budgetWeek).stream()
			.filter(adjustment -> adjustment.getAdjustmentType() == BudgetAdjustmentType.PLAN_SAVED_ADD)
			.mapToLong(adjustment -> zero(adjustment.getAdjustmentAmount()))
			.sum();
	}

	private void applyAccruals(List<PlanWeekAllocation> allocations, long pool, BudgetWeek budgetWeek) {
		long remaining = pool;
		Map<Integer, Long> currentSavedAmounts = new LinkedHashMap<>();
		List<Integer> completedPriorities = new ArrayList<>();

		for (PlanWeekAllocation allocation : allocations) {
			Plan plan = allocation.getPlan();
			long desired = zero(allocation.getFinalAmount());

			long accrual = Math.min(desired, Math.max(remaining, 0L));
			allocation.setActualAmount(accrual);

			long savedAmount = currentSavedAmounts.computeIfAbsent(plan.getId(), ignored -> zero(plan.getSavedAmount()));

			if (accrual > 0L) {
				savedAmount += accrual;
				currentSavedAmounts.put(plan.getId(), savedAmount);
				plan.setSavedAmount(savedAmount);
				budgetAdjustmentRepository.save(BudgetAdjustment.builder()
					.budgetWeek(budgetWeek)
					.plan(plan)
					.adjustmentType(BudgetAdjustmentType.PLAN_SAVED_AUTO_ADD)
					.adjustmentAmount(accrual)
					.reason(WEEKLY_ACCRUAL_REASON)
					.build());
				remaining -= accrual;
			}

			if (accrual < desired) {
				long shortfall = desired - accrual;
				extendPlanForShortfall(plan, shortfall);
			}

			if (savedAmount >= zero(plan.getTargetAmount())) {
				completedPriorities.add(plan.getPriority());
				plan.setStatus(PlanStatus.COMPLETED);
			}
		}

		planWeekAllocationRepository.saveAll(allocations);
		List<Plan> updatedPlans = allocations.stream()
			.map(PlanWeekAllocation::getPlan)
			.filter(Objects::nonNull)
			.filter(plan -> plan.getStatus() == PlanStatus.COMPLETED)
			.distinct()
			.toList();
		if (!updatedPlans.isEmpty()) {
			planRepository.saveAll(updatedPlans);
			completedPriorities.stream()
				.filter(Objects::nonNull)
				.sorted(Comparator.reverseOrder())
				.forEach(priority -> planPriorityService.compactPrioritiesAfterCompletion(
					budgetWeek.getUser().getId(),
					priority
				));
		}
	}

	private void extendPlanForShortfall(Plan plan, long shortfallAmount) {
		if (shortfallAmount <= 0L) {
			return;
		}

		int targetWeeks = plan.getTargetWeeks() == null || plan.getTargetWeeks() <= 0 ? 1 : plan.getTargetWeeks();
		long targetAmount = zero(plan.getTargetAmount());
		if (targetAmount <= 0L) {
			return;
		}

		long weeklyRate = targetAmount / targetWeeks;
		if (weeklyRate <= 0L) {
			return;
		}

		int additionalWeeks = (int) Math.ceil((double) shortfallAmount / weeklyRate);
		plan.extendEndDate(additionalWeeks);
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}
}
