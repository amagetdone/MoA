package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
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
public class BudgetCurrentWeekSavingsReapplyTestService {

	private static final WeekFields ISO_WEEK_FIELDS = WeekFields.of(Locale.UK);

	private final BudgetWeekRepository budgetWeekRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetAdjustmentRepository budgetAdjustmentRepository;
	private final PlanRepository planRepository;
	private final WeeklyGoalSavingAccrualService weeklyGoalSavingAccrualService;
	private final TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase;
	private final WeeklySavingsDetailQueryService weeklySavingsDetailQueryService;
	private final Clock clock;

	public WeeklySavingsDetailDto.Response reapplyCurrentWeekSavings(Integer userId) {
		LocalDate today = LocalDate.now(clock);
		LocalDate monthStart = today.withDayOfMonth(1);
		LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
		BudgetWeek currentWeek = budgetWeekRepository
			.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(userId, today, today)
			.orElseThrow(() -> new IllegalArgumentException("Current budget week not found"));
		List<PlanWeekAllocation> preservedMonthlyAllocations = snapshotMonthlyAllocations(userId, monthStart, monthEnd);

		List<BudgetAdjustment> autoAdjustments = budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(currentWeek)
			.stream()
			.filter(this::isCurrentWeekAutoAdjustment)
			.toList();
		revertAutoSavedAmounts(autoAdjustments);
		if (!autoAdjustments.isEmpty()) {
			budgetAdjustmentRepository.deleteAll(autoAdjustments);
		}

		List<PlanWeekAllocation> allocations = planWeekAllocationRepository.findByBudgetWeek(currentWeek);
		allocations.stream()
			.filter(allocation -> allocation.getPlan() != null)
			.filter(allocation -> allocation.getPlan().getStatus() == PlanStatus.ACTIVE)
			.forEach(allocation -> allocation.setActualAmount(null));
		planWeekAllocationRepository.saveAll(allocations);

		weeklyGoalSavingAccrualService.applyWeeklyAccrual(userId);
		triggerBudgetAllocationUseCase.execute(userId);
		restoreNonActiveAllocations(userId, monthStart, monthEnd, preservedMonthlyAllocations);
		return weeklySavingsDetailQueryService.getDetail(userId, formatYearWeek(today));
	}

	private List<PlanWeekAllocation> snapshotMonthlyAllocations(Integer userId, LocalDate monthStart, LocalDate monthEnd) {
		List<BudgetWeek> affectedWeeks = findAffectedWeeks(userId, monthStart, monthEnd);
		if (affectedWeeks.isEmpty()) {
			return List.of();
		}
		return new ArrayList<>(planWeekAllocationRepository.findAllByBudgetWeekIn(affectedWeeks));
	}

	private void restoreNonActiveAllocations(
		Integer userId,
		LocalDate monthStart,
		LocalDate monthEnd,
		List<PlanWeekAllocation> preservedMonthlyAllocations
	) {
		if (preservedMonthlyAllocations == null || preservedMonthlyAllocations.isEmpty()) {
			return;
		}

		List<BudgetWeek> affectedWeeks = findAffectedWeeks(userId, monthStart, monthEnd);
		if (affectedWeeks.isEmpty()) {
			return;
		}

		Map<LocalDate, BudgetWeek> weeksByStartDate = affectedWeeks.stream()
			.filter(Objects::nonNull)
			.filter(week -> week.getWeekStartDate() != null)
			.sorted(Comparator.comparing(BudgetWeek::getWeekStartDate).thenComparing(BudgetWeek::getId, Comparator.nullsLast(Integer::compareTo)))
			.collect(LinkedHashMap::new, (map, week) -> map.putIfAbsent(week.getWeekStartDate(), week), Map::putAll);

		Map<AllocationKey, PlanWeekAllocation> existingAllocationsByKey = planWeekAllocationRepository.findAllByBudgetWeekIn(affectedWeeks)
			.stream()
			.filter(allocation -> allocation.getPlan() != null)
			.filter(allocation -> allocation.getPlan().getId() != null)
			.filter(allocation -> allocation.getBudgetWeek() != null)
			.filter(allocation -> allocation.getBudgetWeek().getWeekStartDate() != null)
			.collect(LinkedHashMap::new,
				(map, allocation) -> map.putIfAbsent(AllocationKey.from(allocation), allocation),
				Map::putAll);

		List<PlanWeekAllocation> restoredAllocations = preservedMonthlyAllocations.stream()
			.filter(allocation -> allocation.getPlan() != null)
			.filter(allocation -> allocation.getPlan().getId() != null)
			.filter(allocation -> allocation.getBudgetWeek() != null)
			.filter(allocation -> allocation.getBudgetWeek().getWeekStartDate() != null)
			.filter(allocation -> allocation.getPlan().getStatus() != PlanStatus.ACTIVE)
			.filter(allocation -> !existingAllocationsByKey.containsKey(AllocationKey.from(allocation)))
			.map(allocation -> restoreAllocation(allocation, weeksByStartDate))
			.filter(Objects::nonNull)
			.toList();

		if (!restoredAllocations.isEmpty()) {
			planWeekAllocationRepository.saveAll(restoredAllocations);
		}
	}

	private List<BudgetWeek> findAffectedWeeks(Integer userId, LocalDate monthStart, LocalDate monthEnd) {
		return budgetWeekRepository.findAllByUserIdAndWeekEndDateGreaterThanEqualAndWeekStartDateLessThanEqualOrderByWeekStartDateAscIdDesc(
			userId,
			monthStart,
			monthEnd
		);
	}

	private PlanWeekAllocation restoreAllocation(PlanWeekAllocation allocation, Map<LocalDate, BudgetWeek> weeksByStartDate) {
		BudgetWeek restoredWeek = weeksByStartDate.get(allocation.getBudgetWeek().getWeekStartDate());
		if (restoredWeek == null) {
			return null;
		}
		return PlanWeekAllocation.builder()
			.budgetWeek(restoredWeek)
			.plan(allocation.getPlan())
			.allocationType(allocation.getAllocationType())
			.plannedAmount(allocation.getPlannedAmount())
			.finalAmount(allocation.getFinalAmount())
			.actualAmount(allocation.getActualAmount())
			.note(allocation.getNote())
			.build();
	}

	private boolean isCurrentWeekAutoAdjustment(BudgetAdjustment adjustment) {
		return adjustment != null
			&& (adjustment.getAdjustmentType() == BudgetAdjustmentType.PLAN_SAVED_AUTO_ADD
				|| adjustment.getAdjustmentType() == BudgetAdjustmentType.PLAN_SAVED_AUTO_SUBTRACT);
	}

	private void revertAutoSavedAmounts(List<BudgetAdjustment> autoAdjustments) {
		Map<Integer, Long> deltasByPlanId = new LinkedHashMap<>();
		for (BudgetAdjustment adjustment : autoAdjustments) {
			if (adjustment.getPlan() == null || adjustment.getPlan().getId() == null) {
				continue;
			}
			deltasByPlanId.merge(adjustment.getPlan().getId(), reverseDelta(adjustment), Long::sum);
		}
		deltasByPlanId.entrySet().stream()
			.filter(entry -> entry.getKey() != null)
			.filter(entry -> !Objects.equals(entry.getValue(), 0L))
			.forEach(entry -> planRepository.adjustSavedAmount(entry.getKey(), entry.getValue()));
	}

	private long reverseDelta(BudgetAdjustment adjustment) {
		return switch (adjustment.getAdjustmentType()) {
			case PLAN_SAVED_AUTO_ADD -> -zero(adjustment.getAdjustmentAmount());
			case PLAN_SAVED_AUTO_SUBTRACT -> zero(adjustment.getAdjustmentAmount());
			default -> 0L;
		};
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}

	private String formatYearWeek(LocalDate date) {
		int weekBasedYear = date.get(ISO_WEEK_FIELDS.weekBasedYear());
		int weekOfYear = date.get(ISO_WEEK_FIELDS.weekOfWeekBasedYear());
		return "%d-W%02d".formatted(weekBasedYear, weekOfYear);
	}

	private record AllocationKey(Integer planId, LocalDate weekStartDate) {
		private static AllocationKey from(PlanWeekAllocation allocation) {
			return new AllocationKey(
				allocation.getPlan().getId(),
				allocation.getBudgetWeek().getWeekStartDate()
			);
		}
	}
}
