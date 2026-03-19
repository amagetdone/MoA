package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
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
public class DailyGoalSavingAccrualService {

	private static final String AUTO_ADD_REASON = "Automatic daily goal saving accrual applied";
	private static final String AUTO_SUBTRACT_REASON = "Automatic daily goal saving accrual reconciled";
	private static final WeekFields ISO_WEEK_FIELDS = WeekFields.ISO;

	private final BudgetWeekRepository budgetWeekRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetAdjustmentRepository budgetAdjustmentRepository;
	private final PlanRepository planRepository;
	private final Clock clock;

	public GoalSavingAccrualForceDto.Response applyDailyAccrual(Integer userId) {
		LocalDate executedDate = LocalDate.now(clock);
		LocalDate cutoffDate = executedDate.minusDays(1L);
		AccrualPeriod period = periodFor(GoalSavingAccrualForceDto.Scope.DAY, cutoffDate);
		return reconcileDailyCatchUp(userId, cutoffDate, executedDate, period);
	}

	public GoalSavingAccrualForceDto.Response backfillAccrual(Integer userId, GoalSavingAccrualForceDto.BackfillRequest request) {
		GoalSavingAccrualForceDto.BackfillRequest safeRequest = request == null
			? new GoalSavingAccrualForceDto.BackfillRequest(null)
			: request;
		LocalDate cutoffDate = LocalDate.now(clock).minusDays(1L);
		LocalDate executedDate = LocalDate.now(clock);
		return reconcileBackfillScope(userId, safeRequest.planId(), cutoffDate, executedDate);
	}

	private BudgetAdjustment autoAdjustment(
		BudgetWeek budgetWeek,
		Plan plan,
		BudgetAdjustmentType adjustmentType,
		long adjustmentAmount,
		String reason,
		LocalDateTime createdAt
	) {
		BudgetAdjustment adjustment = BudgetAdjustment.builder()
			.budgetWeek(budgetWeek)
			.plan(plan)
			.adjustmentType(adjustmentType)
			.adjustmentAmount(adjustmentAmount)
			.reason(reason)
			.build();
		adjustment.setCreatedAt(createdAt);
		return adjustment;
	}

	private GoalSavingAccrualForceDto.Response reconcileDailyCatchUp(
		Integer userId,
		LocalDate cutoffDate,
		LocalDate executedDate,
		AccrualPeriod period
	) {
		if (userId == null || cutoffDate == null) {
			return emptyResponse(
				executedDate,
				cutoffDate,
				GoalSavingAccrualForceDto.OperationType.BACKFILL,
				GoalSavingAccrualForceDto.Scope.DAY,
				GoalSavingAccrualForceDto.TargetType.ALL,
				null,
				period
			);
		}

		List<PlanWeekAllocation> allocations = findAllocations(userId, null, null, cutoffDate, true);
		if (allocations.isEmpty()) {
			return emptyResponse(
				executedDate,
				cutoffDate,
				GoalSavingAccrualForceDto.OperationType.BACKFILL,
				GoalSavingAccrualForceDto.Scope.DAY,
				GoalSavingAccrualForceDto.TargetType.ALL,
				null,
				period
			);
		}

		return applyAccruals(
			allocations,
			GoalSavingAccrualForceDto.Scope.DAY,
			GoalSavingAccrualForceDto.OperationType.BACKFILL,
			GoalSavingAccrualForceDto.TargetType.ALL,
			null,
			executedDate,
			cutoffDate,
			period,
			true
		);
	}

	private GoalSavingAccrualForceDto.Response reconcileBackfillScope(
		Integer userId,
		Integer planId,
		LocalDate cutoffDate,
		LocalDate executedDate
	) {
		AccrualPeriod period = new AccrualPeriod(null, cutoffDate, cutoffDate == null ? "" : "UP_TO_%s".formatted(cutoffDate));
		if (userId == null || cutoffDate == null) {
			return emptyResponse(
				executedDate,
				cutoffDate,
				GoalSavingAccrualForceDto.OperationType.BACKFILL,
				null,
				planId == null ? GoalSavingAccrualForceDto.TargetType.ALL : GoalSavingAccrualForceDto.TargetType.SINGLE,
				planId,
				period
			);
		}

		List<PlanWeekAllocation> allocations = findAllocations(userId, planId, null, cutoffDate, planId == null);
		if (allocations.isEmpty()) {
			if (planId != null) {
				var ownedPlan = planRepository.findByIdAndUserId(planId, userId);
				if (ownedPlan != null && ownedPlan.isEmpty()) {
					throw new PlanException(PlanErrorCode.PLAN_NOT_FOUND);
				}
			}
			return emptyResponse(
				executedDate,
				cutoffDate,
				GoalSavingAccrualForceDto.OperationType.BACKFILL,
				null,
				planId == null ? GoalSavingAccrualForceDto.TargetType.ALL : GoalSavingAccrualForceDto.TargetType.SINGLE,
				planId,
				period
			);
		}
		LocalDate periodStartDate = allocations.stream()
			.map(PlanWeekAllocation::getBudgetWeek)
			.filter(Objects::nonNull)
			.map(BudgetWeek::getWeekStartDate)
			.min(LocalDate::compareTo)
			.orElse(null);

		return applyAccruals(
			allocations,
			null,
			GoalSavingAccrualForceDto.OperationType.BACKFILL,
			planId == null ? GoalSavingAccrualForceDto.TargetType.ALL : GoalSavingAccrualForceDto.TargetType.SINGLE,
			planId,
			executedDate,
			cutoffDate,
			new AccrualPeriod(periodStartDate, cutoffDate, "UP_TO_%s".formatted(cutoffDate)),
			true
		);
	}

	private GoalSavingAccrualForceDto.Response emptyResponse(
		LocalDate executedDate,
		LocalDate cutoffDate,
		GoalSavingAccrualForceDto.OperationType operationType,
		GoalSavingAccrualForceDto.Scope scope,
		GoalSavingAccrualForceDto.TargetType targetType,
		Integer planId,
		AccrualPeriod period
	) {
		return new GoalSavingAccrualForceDto.Response(
			operationType,
			scope,
			targetType,
			planId,
			0L,
			executedDate,
			cutoffDate,
			period.startDate(),
			period.endDate(),
			period.label(),
			List.of()
		);
	}

	private GoalSavingAccrualForceDto.Response applyAccruals(
		List<PlanWeekAllocation> allocations,
		GoalSavingAccrualForceDto.Scope scope,
		GoalSavingAccrualForceDto.OperationType operationType,
		GoalSavingAccrualForceDto.TargetType targetType,
		Integer requestedPlanId,
		LocalDate executedDate,
		LocalDate cutoffDate,
		AccrualPeriod period,
		boolean catchUpMode
	) {
		long appliedAmount = 0L;
		Map<Integer, PlanAggregate> aggregates = new LinkedHashMap<>();
		Map<Integer, Plan> updatedPlans = new LinkedHashMap<>();
		LocalDateTime adjustmentCreatedAt = LocalDateTime.now(clock);

		for (PlanWeekAllocation allocation : allocations) {
			Plan plan = allocation.getPlan();
			BudgetWeek budgetWeek = allocation.getBudgetWeek();
			if (plan == null || budgetWeek == null) {
				continue;
			}

			LocalDate scopedStart = catchUpMode
				? budgetWeek.getWeekStartDate()
				: laterOf(period.startDate(), budgetWeek.getWeekStartDate());
			LocalDate scopedEnd = catchUpMode
				? budgetWeek.getWeekEndDate()
				: earlierOf(period.endDate(), budgetWeek.getWeekEndDate());
			if (scopedStart == null || scopedEnd == null || scopedStart.isAfter(scopedEnd)) {
				continue;
			}

			LocalDate accrualEnd = earlierOf(cutoffDate, scopedEnd);
			long plannedAmount = calculateScheduledAmountBetweenDates(allocation, zero(allocation.getPlannedAmount()), scopedStart, scopedEnd);
			long finalAmount = calculateScheduledAmountBetweenDates(allocation, zero(allocation.getFinalAmount()), scopedStart, scopedEnd);
			long expectedAccruedAmount = accrualEnd.isBefore(scopedStart)
				? 0L
				: calculateScheduledAmountBetweenDates(allocation, zero(allocation.getFinalAmount()), scopedStart, accrualEnd);
			long remainingScheduledAmount = Math.max(finalAmount - expectedAccruedAmount, 0L);
			long deltaAmount = catchUpMode
				? calculateCatchUpDelta(allocation, cutoffDate)
				: calculateForcedDelta(allocation, scope, scopedStart, accrualEnd, executedDate);

			if (deltaAmount > 0L) {
				plan.setSavedAmount(zero(plan.getSavedAmount()) + deltaAmount);
				budgetAdjustmentRepository.save(autoAdjustment(
					budgetWeek,
					plan,
					BudgetAdjustmentType.PLAN_SAVED_AUTO_ADD,
					deltaAmount,
					AUTO_ADD_REASON,
					adjustmentCreatedAt
				));
				appliedAmount += deltaAmount;
				updatedPlans.put(plan.getId(), plan);
			} else if (deltaAmount < 0L) {
				long deductionAmount = Math.min(zero(plan.getSavedAmount()), Math.abs(deltaAmount));
				plan.setSavedAmount(zero(plan.getSavedAmount()) - deductionAmount);
				budgetAdjustmentRepository.save(autoAdjustment(
					budgetWeek,
					plan,
					BudgetAdjustmentType.PLAN_SAVED_AUTO_SUBTRACT,
					Math.abs(deltaAmount),
					AUTO_SUBTRACT_REASON,
					adjustmentCreatedAt
				));
				appliedAmount -= Math.abs(deltaAmount);
				updatedPlans.put(plan.getId(), plan);
			}

			aggregates.computeIfAbsent(plan.getId(), ignored -> new PlanAggregate(plan))
				.accumulate(plannedAmount, finalAmount, deltaAmount, plan.getSavedAmount(), remainingScheduledAmount);
		}

		if (!updatedPlans.isEmpty()) {
			planRepository.saveAll(updatedPlans.values());
		}

		List<GoalSavingAccrualForceDto.PlanResult> plans = aggregates.values().stream()
			.map(PlanAggregate::toResponse)
			.toList();

		return new GoalSavingAccrualForceDto.Response(
			operationType,
			scope,
			targetType,
			requestedPlanId,
			appliedAmount,
			executedDate,
			cutoffDate,
			period.startDate(),
			period.endDate(),
			period.label(),
			new ArrayList<>(plans)
		);
	}

	private List<PlanWeekAllocation> findAllocations(
		Integer userId,
		Integer planId,
		LocalDate rangeStartDate,
		LocalDate rangeEndDate,
		boolean activeOnly
	) {
		List<BudgetWeek> candidateWeeks = budgetWeekRepository.findAll().stream()
			.filter(Objects::nonNull)
			.filter(week -> week.getUser() != null && Objects.equals(week.getUser().getId(), userId))
			.filter(week -> week.getStatus() != BudgetWeekStatus.CLOSED)
			.filter(week -> rangeEndDate == null || !week.getWeekStartDate().isAfter(rangeEndDate))
			.filter(week -> rangeStartDate == null || overlaps(week, rangeStartDate, rangeEndDate == null ? week.getWeekEndDate() : rangeEndDate))
			.sorted(Comparator.comparing(BudgetWeek::getWeekStartDate))
			.toList();
		if (candidateWeeks.isEmpty()) {
			return List.of();
		}

		return planWeekAllocationRepository.findAllByBudgetWeekIn(candidateWeeks).stream()
			.filter(allocation -> allocation.getPlan() != null)
			.filter(allocation -> allocation.getPlan().getUser() != null)
			.filter(allocation -> Objects.equals(allocation.getPlan().getUser().getId(), userId))
			.filter(allocation -> planId == null || Objects.equals(allocation.getPlan().getId(), planId))
			.filter(allocation -> !activeOnly || allocation.getPlan().getStatus() == PlanStatus.ACTIVE)
			.sorted(Comparator.comparing((PlanWeekAllocation allocation) -> allocation.getPlan().getPriority(), Comparator.nullsLast(Integer::compareTo))
				.thenComparing(allocation -> allocation.getPlan().getId(), Comparator.nullsLast(Integer::compareTo))
				.thenComparing(allocation -> allocation.getBudgetWeek().getWeekStartDate()))
			.toList();
	}

	private long calculateCatchUpDelta(PlanWeekAllocation allocation, LocalDate cutoffDate) {
		long expectedAccruedAmount = calculateCumulativeScheduledAmount(allocation, zero(allocation.getFinalAmount()), cutoffDate);
		long appliedAutoAccrualAmount = calculateAppliedAutoAccrualAmount(allocation.getBudgetWeek(), allocation.getPlan());
		return expectedAccruedAmount - appliedAutoAccrualAmount;
	}

	private long calculateForcedDelta(
		PlanWeekAllocation allocation,
		GoalSavingAccrualForceDto.Scope scope,
		LocalDate scopedStart,
		LocalDate accrualEnd,
		LocalDate executedDate
	) {
		if (accrualEnd == null || accrualEnd.isBefore(scopedStart)) {
			return 0L;
		}

		if (scope == GoalSavingAccrualForceDto.Scope.DAY) {
			long expectedAmount = calculateScheduledAmountBetweenDates(allocation, zero(allocation.getFinalAmount()), scopedStart, accrualEnd);
			long appliedTodayAmount = calculateAppliedAmountByExecutionDate(allocation.getBudgetWeek(), allocation.getPlan(), executedDate);
			return expectedAmount - Math.min(Math.max(appliedTodayAmount, 0L), expectedAmount);
		}

		long expectedUntilEnd = calculateCumulativeScheduledAmount(allocation, zero(allocation.getFinalAmount()), accrualEnd);
		long expectedBeforePeriod = calculateCumulativeScheduledAmount(allocation, zero(allocation.getFinalAmount()), scopedStart.minusDays(1L));
		long expectedInPeriod = Math.max(expectedUntilEnd - expectedBeforePeriod, 0L);
		long appliedCumulativeAmount = calculateAppliedAutoAccrualAmount(allocation.getBudgetWeek(), allocation.getPlan());
		long appliedInPeriod = Math.max(appliedCumulativeAmount - expectedBeforePeriod, 0L);
		return expectedInPeriod - appliedInPeriod;
	}

	private long calculateAppliedAutoAccrualAmount(BudgetWeek budgetWeek, Plan plan) {
		return budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(budgetWeek).stream()
			.filter(adjustment -> adjustment.getPlan() != null && Objects.equals(adjustment.getPlan().getId(), plan.getId()))
			.mapToLong(this::signedAutoAdjustmentAmount)
			.sum();
	}

	private long calculateAppliedAmountByExecutionDate(BudgetWeek budgetWeek, Plan plan, LocalDate executedDate) {
		return budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(budgetWeek).stream()
			.filter(adjustment -> adjustment.getPlan() != null && Objects.equals(adjustment.getPlan().getId(), plan.getId()))
			.filter(adjustment -> adjustment.getCreatedAt() != null && Objects.equals(adjustment.getCreatedAt().toLocalDate(), executedDate))
			.mapToLong(this::signedAutoAdjustmentAmount)
			.sum();
	}

	private long signedAutoAdjustmentAmount(BudgetAdjustment adjustment) {
		return switch (adjustment.getAdjustmentType()) {
			case PLAN_SAVED_AUTO_ADD -> zero(adjustment.getAdjustmentAmount());
			case PLAN_SAVED_AUTO_SUBTRACT -> -zero(adjustment.getAdjustmentAmount());
			default -> 0L;
		};
	}

	private long calculateScheduledAmountBetweenDates(
		PlanWeekAllocation allocation,
		long weeklyAmount,
		LocalDate startDate,
		LocalDate endDate
	) {
		if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
			return 0L;
		}
		return Math.max(
			calculateCumulativeScheduledAmount(allocation, weeklyAmount, endDate)
				- calculateCumulativeScheduledAmount(allocation, weeklyAmount, startDate.minusDays(1L)),
			0L
		);
	}

	private long calculateCumulativeScheduledAmount(PlanWeekAllocation allocation, long weeklyAmount, LocalDate date) {
		BudgetWeek budgetWeek = allocation.getBudgetWeek();
		if (budgetWeek == null || date == null || date.isBefore(budgetWeek.getWeekStartDate())) {
			return 0L;
		}

		int periodDays = budgetWeek.getPeriodDays() == null || budgetWeek.getPeriodDays() <= 0 ? 0 : budgetWeek.getPeriodDays();
		if (periodDays == 0 || weeklyAmount <= 0L) {
			return 0L;
		}

		LocalDate effectiveEndDate = date.isBefore(budgetWeek.getWeekEndDate()) ? date : budgetWeek.getWeekEndDate();
		int elapsedDays = (int)(java.time.temporal.ChronoUnit.DAYS.between(budgetWeek.getWeekStartDate(), effectiveEndDate) + 1L);
		elapsedDays = Math.max(0, Math.min(elapsedDays, periodDays));

		long baseDailyAmount = weeklyAmount / periodDays;
		int remainderDays = (int)(weeklyAmount % periodDays);
		int elapsedRemainderDays = Math.max(0, elapsedDays - (periodDays - remainderDays));
		return (baseDailyAmount * elapsedDays) + elapsedRemainderDays;
	}

	private AccrualPeriod periodFor(GoalSavingAccrualForceDto.Scope scope, LocalDate cutoffDate) {
		if (cutoffDate == null) {
			return new AccrualPeriod(null, null, "");
		}

		if (scope == GoalSavingAccrualForceDto.Scope.DAY) {
			return new AccrualPeriod(cutoffDate, cutoffDate, cutoffDate.toString());
		}
		if (scope == GoalSavingAccrualForceDto.Scope.MONTH) {
			YearMonth yearMonth = YearMonth.from(cutoffDate);
			return new AccrualPeriod(
				yearMonth.atDay(1),
				yearMonth.atEndOfMonth(),
				yearMonth.toString()
			);
		}

		LocalDate startDate = cutoffDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		int weekBasedYear = cutoffDate.get(ISO_WEEK_FIELDS.weekBasedYear());
		int weekNumber = cutoffDate.get(ISO_WEEK_FIELDS.weekOfWeekBasedYear());
		return new AccrualPeriod(startDate, startDate.plusDays(6L), "%d-W%02d".formatted(weekBasedYear, weekNumber));
	}

	private LocalDate laterOf(LocalDate first, LocalDate second) {
		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}
		return first.isAfter(second) ? first : second;
	}

	private LocalDate earlierOf(LocalDate first, LocalDate second) {
		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}
		return first.isBefore(second) ? first : second;
	}

	private boolean overlaps(BudgetWeek budgetWeek, LocalDate rangeStartDate, LocalDate rangeEndDate) {
		return !budgetWeek.getWeekEndDate().isBefore(rangeStartDate) && !budgetWeek.getWeekStartDate().isAfter(rangeEndDate);
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}

	private record AccrualPeriod(
		LocalDate startDate,
		LocalDate endDate,
		String label
	) {
	}

	private static final class PlanAggregate {
		private final Integer planId;
		private final String planTitle;
		private final Integer priority;
		private long plannedAmount;
		private long finalAmount;
		private long accruedAmount;
		private long savedAmount;
		private long remainingScheduledAmount;

		private PlanAggregate(Plan plan) {
			this.planId = plan.getId();
			this.planTitle = plan.getTitle();
			this.priority = plan.getPriority();
		}

		private void accumulate(
			long plannedAmount,
			long finalAmount,
			long accruedAmount,
			long savedAmount,
			long remainingScheduledAmount
		) {
			this.plannedAmount += plannedAmount;
			this.finalAmount += finalAmount;
			this.accruedAmount += accruedAmount;
			this.savedAmount = savedAmount;
			this.remainingScheduledAmount += remainingScheduledAmount;
		}

		private GoalSavingAccrualForceDto.PlanResult toResponse() {
			return new GoalSavingAccrualForceDto.PlanResult(
				planId,
				planTitle,
				priority,
				plannedAmount,
				finalAmount,
				accruedAmount,
				savedAmount,
				remainingScheduledAmount
			);
		}
	}
}
