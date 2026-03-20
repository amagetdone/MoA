package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetRolloverRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetMonthStatus;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetHistoricalRebuildService {

	private final PlanRepository planRepository;
	private final BudgetWeekRepository budgetWeekRepository;
	private final BudgetMonthRepository budgetMonthRepository;
	private final BudgetAdjustmentRepository budgetAdjustmentRepository;
	private final BudgetRolloverRepository budgetRolloverRepository;
	private final EssentialWeekAllocationRepository essentialWeekAllocationRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetAllocationTriggerService budgetAllocationTriggerService;
	private final BudgetActualSpentRefreshService budgetActualSpentRefreshService;
	private final BudgetRolloverResyncService budgetRolloverResyncService;
	private final DailyGoalSavingAccrualService dailyGoalSavingAccrualService;
	private final Clock clock;

	public GoalSavingAccrualForceDto.Response rebuild(Integer userId, Integer planId) {
		Plan plan = planRepository.findByIdAndUserId(planId, userId)
			.orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));

		LocalDate today = LocalDate.now(clock);
		LocalDate serviceStartDate = plan.getStartDate();
		if (serviceStartDate == null || serviceStartDate.isAfter(today)) {
			return new GoalSavingAccrualForceDto.Response(
				GoalSavingAccrualForceDto.OperationType.BACKFILL,
				null,
				GoalSavingAccrualForceDto.TargetType.SINGLE,
				planId,
				0L,
				today,
				today.minusDays(1L),
				null,
				null,
				"",
				List.of()
			);
		}

		List<BudgetWeek> affectedWeeks = budgetWeekRepository.findAll().stream()
			.filter(week -> week != null)
			.filter(week -> week.getUser() != null && Objects.equals(week.getUser().getId(), userId))
			.filter(week -> overlaps(week, serviceStartDate, today))
			.sorted(java.util.Comparator.comparing(BudgetWeek::getWeekStartDate))
			.toList();

		List<BudgetMonth> affectedMonths = budgetMonthRepository.findAll().stream()
			.filter(month -> month != null)
			.filter(month -> month.getUser() != null && Objects.equals(month.getUser().getId(), userId))
			.filter(month -> !month.getBaseDate().isBefore(serviceStartDate.withDayOfMonth(1)))
			.filter(month -> !month.getBaseDate().isAfter(today.withDayOfMonth(1)))
			.toList();

		List<BudgetAdjustment> adjustments = affectedWeeks.isEmpty()
			? List.of()
			: budgetAdjustmentRepository.findAllByBudgetWeekIn(affectedWeeks);
		resetPlanSavedAmounts(adjustments);

		if (!adjustments.isEmpty()) {
			budgetAdjustmentRepository.deleteAll(adjustments);
		}
		if (!affectedWeeks.isEmpty()) {
			budgetRolloverRepository.deleteAllBySourceBudgetWeekIn(affectedWeeks);
			budgetRolloverRepository.deleteAllByTargetBudgetWeekIn(affectedWeeks);

			List<EssentialWeekAllocation> essentialAllocations = essentialWeekAllocationRepository.findAllByBudgetWeekIn(affectedWeeks);
			if (!essentialAllocations.isEmpty()) {
				essentialWeekAllocationRepository.deleteAll(essentialAllocations);
			}

			List<PlanWeekAllocation> planAllocations = planWeekAllocationRepository.findAllByBudgetWeekIn(affectedWeeks);
			if (!planAllocations.isEmpty()) {
				planWeekAllocationRepository.deleteAll(planAllocations);
			}

			budgetWeekRepository.deleteAll(affectedWeeks);
		}

		if (!affectedMonths.isEmpty()) {
			affectedMonths.forEach(month -> month.setStatus(BudgetMonthStatus.OPEN));
			budgetMonthRepository.saveAll(affectedMonths);
		}

		BudgetAllocationService.AllocationResult allocationResult = budgetAllocationTriggerService.triggerHistorical(
			userId,
			serviceStartDate,
			today
		);
		if (!allocationResult.budgetWeeks().isEmpty()) {
			budgetActualSpentRefreshService.refresh(userId, allocationResult.budgetWeeks());
		}
		budgetRolloverResyncService.resync(userId);

		return dailyGoalSavingAccrualService.backfillAccrual(userId, new GoalSavingAccrualForceDto.BackfillRequest(null));
	}

	private void resetPlanSavedAmounts(List<BudgetAdjustment> adjustments) {
		Map<Integer, Plan> plansById = new LinkedHashMap<>();
		Map<Integer, Long> signedAdjustmentsByPlanId = new LinkedHashMap<>();

		for (BudgetAdjustment adjustment : adjustments) {
			if (adjustment.getPlan() == null || adjustment.getPlan().getId() == null) {
				continue;
			}
			long signedAmount = signedSavedAdjustmentAmount(adjustment);
			if (signedAmount == 0L) {
				continue;
			}
			plansById.putIfAbsent(adjustment.getPlan().getId(), adjustment.getPlan());
			signedAdjustmentsByPlanId.merge(adjustment.getPlan().getId(), signedAmount, Long::sum);
		}

		if (plansById.isEmpty()) {
			return;
		}

		for (Map.Entry<Integer, Plan> entry : plansById.entrySet()) {
			long signedAmount = signedAdjustmentsByPlanId.getOrDefault(entry.getKey(), 0L);
			Plan plan = entry.getValue();
			plan.setSavedAmount(Math.max(0L, zero(plan.getSavedAmount()) - signedAmount));
		}

		planRepository.saveAll(plansById.values());
	}

	private long signedSavedAdjustmentAmount(BudgetAdjustment adjustment) {
		return switch (adjustment.getAdjustmentType()) {
			case PLAN_SAVED_ADD, PLAN_SAVED_AUTO_ADD -> zero(adjustment.getAdjustmentAmount());
			case PLAN_SAVED_AUTO_SUBTRACT -> -zero(adjustment.getAdjustmentAmount());
			default -> 0L;
		};
	}

	private boolean overlaps(BudgetWeek budgetWeek, LocalDate startDate, LocalDate endDate) {
		return !budgetWeek.getWeekEndDate().isBefore(startDate) && !budgetWeek.getWeekStartDate().isAfter(endDate);
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}
}
