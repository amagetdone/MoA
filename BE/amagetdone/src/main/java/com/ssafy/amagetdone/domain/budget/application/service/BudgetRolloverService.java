package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetRollover;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetRolloverRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetRolloverStatus;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetRolloverService {

	private static final int MAX_ROLLOVER_STEP = 4;
	private static final String ROLLOVER_ADJUSTMENT_REASON = "Free-budget shortage deducted from goal savings";

	private final BudgetWeekRepository budgetWeekRepository;
	private final BudgetRolloverRepository budgetRolloverRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetAdjustmentRepository budgetAdjustmentRepository;

	public void executeRollover(BudgetWeek sourceWeek) {
		if (sourceWeek == null) {
			throw new IllegalArgumentException("Source budget week is required");
		}

		long excessAmount = zero(sourceWeek.getActualSpentAmount()) - zero(sourceWeek.getFinalFreeAmount());
		if (excessAmount <= 0L) {
			return;
		}

		List<PlanWeekAllocation> sourceAllocations = planWeekAllocationRepository.findByBudgetWeek(sourceWeek);
		long deductedFromSourceWeek = deductFromPlans(sourceWeek, sourceAllocations, excessAmount);
		if (deductedFromSourceWeek > 0L) {
			budgetWeekRepository.save(sourceWeek);
		}

		long remainingRolloverAmount = excessAmount - deductedFromSourceWeek;
		if (remainingRolloverAmount <= 0L) {
			return;
		}

		BudgetWeek currentSourceWeek = sourceWeek;
		long rolloverAmount = remainingRolloverAmount;
		int rolloverStep = 1;

		while (rolloverAmount > 0L) {
			BudgetWeek targetWeek = budgetWeekRepository
				.findFirstByUserAndWeekSequenceGreaterThanOrderByWeekSequenceAsc(
					currentSourceWeek.getUser(),
					currentSourceWeek.getWeekSequence()
				)
				.orElseThrow(() -> new IllegalStateException("Next budget week not found"));

			List<PlanWeekAllocation> targetAllocations = planWeekAllocationRepository.findByBudgetWeek(targetWeek);
			applyRollover(targetWeek, targetAllocations, rolloverAmount);
			budgetWeekRepository.save(targetWeek);

			BudgetRollover rollover = budgetRolloverRepository.save(BudgetRollover.builder()
				.sourceBudgetWeek(currentSourceWeek)
				.targetBudgetWeek(targetWeek)
				.rolloverStep(rolloverStep)
				.rolloverAmount(rolloverAmount)
				.status(BudgetRolloverStatus.APPLIED)
				.build());

			if (zero(targetWeek.getFinalFreeAmount()) >= 0L) {
				rollover.setResolvedAt(LocalDateTime.now());
				budgetRolloverRepository.save(rollover);
				return;
			}

			if (rolloverStep < MAX_ROLLOVER_STEP) {
				rollover.setStatus(BudgetRolloverStatus.EXHAUSTED);
				rollover.setResolvedAt(LocalDateTime.now());
				budgetRolloverRepository.save(rollover);
				currentSourceWeek = targetWeek;
				rolloverAmount = -zero(targetWeek.getFinalFreeAmount());
				rolloverStep++;
				continue;
			}

			long deductedAmount = deductFromPlans(targetWeek, targetAllocations, -zero(targetWeek.getFinalFreeAmount()));
			budgetWeekRepository.save(targetWeek);

			if (deductedAmount > 0L) {
				BudgetRollover latestIncomingRollover = budgetRolloverRepository
					.findFirstByTargetBudgetWeekOrderByCreatedAtDesc(targetWeek)
					.orElse(rollover);
				latestIncomingRollover.setStatus(BudgetRolloverStatus.PLAN_DEDUCTED);
				latestIncomingRollover.setResolvedAt(LocalDateTime.now());
				budgetRolloverRepository.save(latestIncomingRollover);
			}
			return;
		}
	}

	private void applyRollover(BudgetWeek targetWeek, List<PlanWeekAllocation> allocations, long rolloverAmount) {
		targetWeek.setRolloverInAmount(zero(targetWeek.getRolloverInAmount()) + rolloverAmount);
		recalculateFinalFreeAmount(targetWeek, allocations);
	}

	private long deductFromPlans(BudgetWeek targetWeek, List<PlanWeekAllocation> allocations, long shortageAmount) {
		if (shortageAmount <= 0L || allocations.isEmpty()) {
			return 0L;
		}

		long remainingShortage = shortageAmount;
		List<PlanWeekAllocation> updatedAllocations = new ArrayList<>();

		for (PlanWeekAllocation allocation : allocations.stream().sorted(planDeductionOrder()).toList()) {
			long currentFinalAmount = zero(allocation.getFinalAmount());
			long deductionAmount = Math.min(remainingShortage, currentFinalAmount);
			if (deductionAmount <= 0L) {
				continue;
			}

			allocation.setFinalAmount(currentFinalAmount - deductionAmount);
			updatedAllocations.add(allocation);
			budgetAdjustmentRepository.save(BudgetAdjustment.builder()
				.budgetWeek(targetWeek)
				.plan(allocation.getPlan())
				.adjustmentType(BudgetAdjustmentType.ROLLOVER_APPLIED)
				.adjustmentAmount(deductionAmount)
				.reason(ROLLOVER_ADJUSTMENT_REASON)
				.build());
			remainingShortage -= deductionAmount;
			if (remainingShortage == 0L) {
				break;
			}
		}

		if (!updatedAllocations.isEmpty()) {
			planWeekAllocationRepository.saveAll(updatedAllocations);
			recalculateFinalFreeAmount(targetWeek, allocations);
		}

		return shortageAmount - remainingShortage;
	}

	private void recalculateFinalFreeAmount(BudgetWeek budgetWeek, List<PlanWeekAllocation> allocations) {
		long finalSavingAmount = allocations.stream()
			.mapToLong(allocation -> zero(allocation.getFinalAmount()))
			.sum();
		long planSavedAmount = sumPlanSavedAmount(budgetWeek);

		budgetWeek.setFinalFreeAmount(
			zero(budgetWeek.getPlannedIncomeAmount())
				- zero(budgetWeek.getPlannedEssentialAmount())
				- finalSavingAmount
				- zero(budgetWeek.getRolloverInAmount())
				+ zero(budgetWeek.getManualAdjustmentAmount())
				- planSavedAmount
		);
	}

	private long sumPlanSavedAmount(BudgetWeek budgetWeek) {
		return budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(budgetWeek).stream()
			.filter(adjustment -> adjustment.getAdjustmentType() == BudgetAdjustmentType.PLAN_SAVED_ADD)
			.mapToLong(adjustment -> zero(adjustment.getAdjustmentAmount()))
			.sum();
	}

	private Comparator<PlanWeekAllocation> planDeductionOrder() {
		return Comparator
			.comparingInt((PlanWeekAllocation allocation) -> allocation.getPlan().getPriority())
			.reversed()
			.thenComparing(allocation -> allocation.getPlan().getId(), Comparator.nullsLast(Integer::compareTo));
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}
}
