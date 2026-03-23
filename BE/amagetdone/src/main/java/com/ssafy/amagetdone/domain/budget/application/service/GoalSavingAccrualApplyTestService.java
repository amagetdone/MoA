package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
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
public class GoalSavingAccrualApplyTestService {

	private static final String APPLY_REASON_PREFIX = "TEST_APPLY";
	private static final WeekFields ISO_WEEK_FIELDS = WeekFields.ISO;

	private final BudgetWeekRepository budgetWeekRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetAdjustmentRepository budgetAdjustmentRepository;
	private final PlanRepository planRepository;
	private final Clock clock;

	public GoalSavingAccrualForceDto.Response apply(Integer userId, GoalSavingAccrualForceDto.ApplyRequest request) {
		GoalSavingAccrualForceDto.ApplyRequest safeRequest = request == null
			? new GoalSavingAccrualForceDto.ApplyRequest(null, GoalSavingAccrualForceDto.Scope.WEEK)
			: request;
		LocalDate executedDate = LocalDate.now(clock);
		GoalSavingAccrualForceDto.Scope scope = safeRequest.resolvedScope();
		ApplyPeriod period = periodFor(scope, executedDate);

		if (safeRequest.planId() != null) {
			planRepository.findByIdAndUserId(safeRequest.planId(), userId)
				.orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));
		}

		List<PlanWeekAllocation> allocations = findAllocations(userId, safeRequest.planId(), period.startDate(), period.endDate(), safeRequest.planId() == null);
		if (allocations.isEmpty()) {
			return new GoalSavingAccrualForceDto.Response(
				GoalSavingAccrualForceDto.OperationType.APPLY,
				scope,
				safeRequest.planId() == null ? GoalSavingAccrualForceDto.TargetType.ALL : GoalSavingAccrualForceDto.TargetType.SINGLE,
				safeRequest.planId(),
				0L,
				executedDate,
				executedDate,
				period.startDate(),
				period.endDate(),
				period.label(),
				List.of()
			);
		}

		Map<Integer, PlanAggregate> planAggregates = new LinkedHashMap<>();
		Map<Integer, Long> currentSavedAmounts = new LinkedHashMap<>();
		Map<Integer, Long> savedAmountDeltas = new LinkedHashMap<>();
		long appliedAmount = 0L;
		String reasonKey = reasonKey(scope, period.label());

		for (PlanWeekAllocation allocation : allocations) {
			Plan plan = allocation.getPlan();
			BudgetWeek budgetWeek = allocation.getBudgetWeek();
			LocalDate overlapStart = laterOf(period.startDate(), budgetWeek.getWeekStartDate());
			LocalDate overlapEnd = earlierOf(period.endDate(), budgetWeek.getWeekEndDate());
			if (overlapStart.isAfter(overlapEnd)) {
				continue;
			}

			long plannedAmount = scopedAmount(allocation, zero(allocation.getPlannedAmount()), overlapStart, overlapEnd);
			long finalAmount = scopedAmount(allocation, zero(allocation.getFinalAmount()), overlapStart, overlapEnd);
			long accruedAmount = finalAmount;
			long savedAmount = currentSavedAmounts.computeIfAbsent(plan.getId(), ignored -> zero(plan.getSavedAmount()));

			if (accruedAmount > 0L) {
				savedAmount += accruedAmount;
				currentSavedAmounts.put(plan.getId(), savedAmount);
				savedAmountDeltas.merge(plan.getId(), accruedAmount, Long::sum);
				budgetAdjustmentRepository.save(BudgetAdjustment.builder()
					.budgetWeek(budgetWeek)
					.plan(plan)
					.adjustmentType(BudgetAdjustmentType.PLAN_SAVED_ADD)
					.adjustmentAmount(accruedAmount)
					.reason(reasonKey)
					.build());
				appliedAmount += accruedAmount;
			}

			planAggregates.computeIfAbsent(plan.getId(), ignored -> new PlanAggregate(plan))
				.accumulate(plannedAmount, finalAmount, accruedAmount, savedAmount);
		}

		savedAmountDeltas.forEach((planId, delta) -> planRepository.adjustSavedAmount(planId, delta));

		List<GoalSavingAccrualForceDto.PlanResult> plans = planAggregates.values().stream()
			.map(PlanAggregate::toResponse)
			.toList();

		return new GoalSavingAccrualForceDto.Response(
			GoalSavingAccrualForceDto.OperationType.APPLY,
			scope,
			safeRequest.planId() == null ? GoalSavingAccrualForceDto.TargetType.ALL : GoalSavingAccrualForceDto.TargetType.SINGLE,
			safeRequest.planId(),
			appliedAmount,
			executedDate,
			executedDate,
			period.startDate(),
			period.endDate(),
			period.label(),
			new ArrayList<>(plans)
		);
	}

	private List<PlanWeekAllocation> findAllocations(
		Integer userId,
		Integer planId,
		LocalDate startDate,
		LocalDate endDate,
		boolean activeOnly
	) {
		List<BudgetWeek> weeks = budgetWeekRepository.findAll().stream()
			.filter(Objects::nonNull)
			.filter(week -> week.getUser() != null && Objects.equals(week.getUser().getId(), userId))
			.filter(week -> week.getStatus() != BudgetWeekStatus.CLOSED)
			.filter(week -> !week.getWeekEndDate().isBefore(startDate))
			.filter(week -> !week.getWeekStartDate().isAfter(endDate))
			.sorted(Comparator.comparing(BudgetWeek::getWeekStartDate))
			.toList();
		if (weeks.isEmpty()) {
			return List.of();
		}

		return planWeekAllocationRepository.findAllByBudgetWeekIn(weeks).stream()
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

	private String reasonKey(GoalSavingAccrualForceDto.Scope scope, String periodLabel) {
		return "%s|%s|%s".formatted(APPLY_REASON_PREFIX, scope.name(), periodLabel);
	}

	private long scopedAmount(PlanWeekAllocation allocation, long totalAmount, LocalDate startDate, LocalDate endDate) {
		if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
			return 0L;
		}
		return Math.max(
			cumulativeAmount(allocation, totalAmount, endDate) - cumulativeAmount(allocation, totalAmount, startDate.minusDays(1L)),
			0L
		);
	}

	private long cumulativeAmount(PlanWeekAllocation allocation, long totalAmount, LocalDate date) {
		BudgetWeek budgetWeek = allocation.getBudgetWeek();
		if (budgetWeek == null || date == null || date.isBefore(budgetWeek.getWeekStartDate())) {
			return 0L;
		}

		int periodDays = budgetWeek.getPeriodDays() == null || budgetWeek.getPeriodDays() <= 0 ? 0 : budgetWeek.getPeriodDays();
		if (periodDays == 0 || totalAmount <= 0L) {
			return 0L;
		}

		LocalDate effectiveEndDate = date.isBefore(budgetWeek.getWeekEndDate()) ? date : budgetWeek.getWeekEndDate();
		int elapsedDays = (int)(java.time.temporal.ChronoUnit.DAYS.between(budgetWeek.getWeekStartDate(), effectiveEndDate) + 1L);
		elapsedDays = Math.max(0, Math.min(elapsedDays, periodDays));

		long baseDailyAmount = totalAmount / periodDays;
		int remainderDays = (int)(totalAmount % periodDays);
		int elapsedRemainderDays = Math.max(0, elapsedDays - (periodDays - remainderDays));
		return (baseDailyAmount * elapsedDays) + elapsedRemainderDays;
	}

	private ApplyPeriod periodFor(GoalSavingAccrualForceDto.Scope scope, LocalDate executedDate) {
		if (scope == GoalSavingAccrualForceDto.Scope.DAY) {
			return new ApplyPeriod(executedDate, executedDate, executedDate.toString());
		}
		if (scope == GoalSavingAccrualForceDto.Scope.MONTH) {
			YearMonth yearMonth = YearMonth.from(executedDate);
			return new ApplyPeriod(yearMonth.atDay(1), yearMonth.atEndOfMonth(), yearMonth.toString());
		}

		LocalDate startDate = executedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		int weekBasedYear = executedDate.get(ISO_WEEK_FIELDS.weekBasedYear());
		int weekNumber = executedDate.get(ISO_WEEK_FIELDS.weekOfWeekBasedYear());
		return new ApplyPeriod(startDate, startDate.plusDays(6L), "%d-W%02d".formatted(weekBasedYear, weekNumber));
	}

	private LocalDate laterOf(LocalDate first, LocalDate second) {
		return first.isAfter(second) ? first : second;
	}

	private LocalDate earlierOf(LocalDate first, LocalDate second) {
		return first.isBefore(second) ? first : second;
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}

	private record ApplyPeriod(
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

		private PlanAggregate(Plan plan) {
			this.planId = plan.getId();
			this.planTitle = plan.getTitle();
			this.priority = plan.getPriority();
		}

		private void accumulate(long plannedAmount, long finalAmount, long accruedAmount, long savedAmount) {
			this.plannedAmount += plannedAmount;
			this.finalAmount += finalAmount;
			this.accruedAmount += accruedAmount;
			this.savedAmount = savedAmount;
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
				0L
			);
		}
	}
}
