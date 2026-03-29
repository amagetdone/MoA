package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetRolloverRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetMonthStatus;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import jakarta.persistence.EntityManager;
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
	private final EssentialExpenseMonthRepository essentialExpenseMonthRepository;
	private final EssentialWeekAllocationRepository essentialWeekAllocationRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetAllocationTriggerService budgetAllocationTriggerService;
	private final BudgetActualSpentRefreshService budgetActualSpentRefreshService;
	private final DailyGoalSavingAccrualService dailyGoalSavingAccrualService;
	private final WeeklyGoalSavingAccrualService weeklyGoalSavingAccrualService;
	private final EntityManager entityManager;
	private final Clock clock;

	public GoalSavingAccrualForceDto.Response rebuild(Integer userId, Integer planId) {
		Plan plan = planRepository.findByIdAndUserId(planId, userId)
			.orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));

		LocalDate today = LocalDate.now(clock);
		LocalDate rebuildEndDate = today.withDayOfMonth(today.lengthOfMonth());
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

		List<BudgetMonth> budgetMonths = budgetMonthsWithSeedFallback(userId, serviceStartDate, rebuildEndDate);
		List<BudgetWeek> affectedWeeks = budgetWeekRepository.findAllByUser_IdOrderByWeekStartDateAsc(userId).stream()
			.filter(week -> overlaps(week, serviceStartDate, rebuildEndDate))
			.toList();

		List<BudgetMonth> affectedMonths = budgetMonths.stream()
			.filter(month -> month != null)
			.filter(month -> month.getUser() != null && Objects.equals(month.getUser().getId(), userId))
			.filter(month -> !month.getBaseDate().isBefore(serviceStartDate.withDayOfMonth(1)))
			.filter(month -> !month.getBaseDate().isAfter(rebuildEndDate.withDayOfMonth(1)))
			.toList();

		List<BudgetAdjustment> adjustments = affectedWeeks.isEmpty()
			? List.of()
			: budgetAdjustmentRepository.findAllByBudgetWeekIn(affectedWeeks);
		planRepository.resetSavedAmount(planId);
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
			affectedMonths = new ArrayList<>(budgetMonthRepository.saveAll(affectedMonths));
			seedMissingEssentialExpenseMonths(userId, affectedMonths);
		}
		flushAndClear();

		BudgetAllocationService.AllocationResult allocationResult = budgetAllocationTriggerService.triggerHistorical(
			userId,
			serviceStartDate,
			rebuildEndDate
		);
		if (!allocationResult.budgetWeeks().isEmpty()) {
			budgetActualSpentRefreshService.refresh(userId, allocationResult.budgetWeeks());
		}
		flushAndClear();

		GoalSavingAccrualForceDto.Response response = dailyGoalSavingAccrualService.backfillAccrual(
			userId,
			new GoalSavingAccrualForceDto.BackfillRequest(null)
		);
		weeklyGoalSavingAccrualService.applyWeeklyAccrual(userId);
		return response;
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private List<BudgetMonth> budgetMonthsWithSeedFallback(Integer userId, LocalDate serviceStartDate, LocalDate today) {
		List<BudgetMonth> existingMonths = budgetMonthRepository.findAllByUser_IdOrderByBaseDateAsc(userId);
		if (existingMonths.isEmpty()) {
			return existingMonths;
		}

		Map<LocalDate, BudgetMonth> monthsByBaseDate = new LinkedHashMap<>();
		existingMonths.stream()
			.sorted(java.util.Comparator.comparing(BudgetMonth::getBaseDate))
			.forEach(month -> monthsByBaseDate.put(month.getBaseDate(), month));

		BudgetMonth referenceMonth = monthsByBaseDate.values().iterator().next();
		LocalDate cursor = serviceStartDate.withDayOfMonth(1);
		LocalDate endMonth = today.withDayOfMonth(1);
		List<BudgetMonth> seededMonths = new ArrayList<>();

		while (!cursor.isAfter(endMonth)) {
			if (!monthsByBaseDate.containsKey(cursor)) {
				BudgetMonth seededMonth = BudgetMonth.builder()
					.user(referenceMonth.getUser())
					.baseDate(cursor)
					.status(referenceMonth.getStatus() == null ? BudgetMonthStatus.OPEN : referenceMonth.getStatus())
					.totalIncomeAmount(zero(referenceMonth.getTotalIncomeAmount()))
					.totalFixedEssentialAmount(zero(referenceMonth.getTotalFixedEssentialAmount()))
					.totalVariableEssentialAmount(zero(referenceMonth.getTotalVariableEssentialAmount()))
					.memo(referenceMonth.getMemo())
					.build();
				seededMonths.add(seededMonth);
				monthsByBaseDate.put(cursor, seededMonth);
			}
			cursor = cursor.plusMonths(1);
		}

		if (!seededMonths.isEmpty()) {
			List<BudgetMonth> orderedMonths = monthsByBaseDate.values().stream()
				.sorted(java.util.Comparator.comparing(BudgetMonth::getBaseDate))
				.toList();
			return orderedMonths;
		}
		return existingMonths;
	}

	private void seedMissingEssentialExpenseMonths(Integer userId, List<BudgetMonth> affectedMonths) {
		if (affectedMonths == null || affectedMonths.isEmpty()) {
			return;
		}

		List<EssentialExpenseMonth> existingEssentials = essentialExpenseMonthRepository
			.findAllByBudgetMonth_User_IdOrderByBudgetMonth_BaseDateAscIdAsc(userId);
		if (existingEssentials.isEmpty()) {
			return;
		}

		Map<LocalDate, List<EssentialExpenseMonth>> essentialsByBaseDate = new LinkedHashMap<>();
		for (EssentialExpenseMonth existingEssential : existingEssentials) {
			if (existingEssential.getBudgetMonth() == null || existingEssential.getBudgetMonth().getBaseDate() == null) {
				continue;
			}
			essentialsByBaseDate.computeIfAbsent(existingEssential.getBudgetMonth().getBaseDate(), ignored -> new ArrayList<>())
				.add(existingEssential);
		}
		if (essentialsByBaseDate.isEmpty()) {
			return;
		}

		List<EssentialExpenseMonth> referenceRows = essentialsByBaseDate.values().iterator().next();
		List<EssentialExpenseMonth> seededEssentials = new ArrayList<>();
		for (BudgetMonth affectedMonth : affectedMonths) {
			if (affectedMonth == null || affectedMonth.getBaseDate() == null) {
				continue;
			}
			if (essentialsByBaseDate.containsKey(affectedMonth.getBaseDate())) {
				continue;
			}
			for (EssentialExpenseMonth referenceRow : referenceRows) {
				seededEssentials.add(EssentialExpenseMonth.builder()
					.category(referenceRow.getCategory())
					.budgetMonth(affectedMonth)
					.expenseScope(referenceRow.getExpenseScope())
					.amount(zero(referenceRow.getAmount()))
					.sourceProvider(referenceRow.getSourceProvider())
					.build());
			}
		}

		if (!seededEssentials.isEmpty()) {
			essentialExpenseMonthRepository.saveAll(seededEssentials);
		}
	}

	private void resetPlanSavedAmounts(List<BudgetAdjustment> adjustments) {
		Map<Integer, Long> savedAmountDeltasByPlanId = new LinkedHashMap<>();

		for (BudgetAdjustment adjustment : adjustments) {
			if (adjustment.getPlan() == null || adjustment.getPlan().getId() == null) {
				continue;
			}
			long signedAmount = signedSavedAdjustmentAmount(adjustment);
			if (signedAmount == 0L) {
				continue;
			}
			savedAmountDeltasByPlanId.merge(adjustment.getPlan().getId(), -signedAmount, Long::sum);
		}

		if (savedAmountDeltasByPlanId.isEmpty()) {
			return;
		}

		savedAmountDeltasByPlanId.forEach(planRepository::adjustSavedAmount);
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
