package com.ssafy.amagetdone.domain.budget.application.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.budget.application.usecase.ExecuteRolloverUseCase;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetRolloverRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetRolloverResyncService {

	private final BudgetWeekRepository budgetWeekRepository;
	private final BudgetRolloverRepository budgetRolloverRepository;
	private final BudgetAdjustmentRepository budgetAdjustmentRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final ExecuteRolloverUseCase executeRolloverUseCase;

	public void resync(Integer userId) {
		List<BudgetWeek> openWeeks = budgetWeekRepository.findAllByUser_IdOrderByWeekSequenceAsc(userId).stream()
			.filter(budgetWeek -> budgetWeek.getStatus() != BudgetWeekStatus.CLOSED)
			.toList();
		if (openWeeks.isEmpty()) {
			return;
		}

		budgetRolloverRepository.deleteAllBySourceBudgetWeekIn(openWeeks);
		budgetRolloverRepository.deleteAllByTargetBudgetWeekIn(openWeeks);
		budgetAdjustmentRepository.deleteAllByBudgetWeekInAndAdjustmentType(openWeeks, BudgetAdjustmentType.ROLLOVER_APPLIED);

		List<PlanWeekAllocation> allocations = planWeekAllocationRepository.findAllByBudgetWeekIn(openWeeks);
		Map<Integer, List<PlanWeekAllocation>> allocationsByWeekId = allocations.stream()
			.filter(allocation -> allocation.getBudgetWeek() != null && allocation.getBudgetWeek().getId() != null)
			.collect(Collectors.groupingBy(allocation -> allocation.getBudgetWeek().getId()));

		allocations.forEach(allocation -> allocation.setFinalAmount(zero(allocation.getPlannedAmount())));

		for (BudgetWeek openWeek : openWeeks) {
			reapplyManualPlanAdjustments(openWeek, allocationsByWeekId.getOrDefault(openWeek.getId(), List.of()));
			recalculateFinalFreeAmount(openWeek, allocationsByWeekId.getOrDefault(openWeek.getId(), List.of()));
		}

		if (!allocations.isEmpty()) {
			planWeekAllocationRepository.saveAll(allocations);
		}
		budgetWeekRepository.saveAll(openWeeks);

		for (BudgetWeek openWeek : openWeeks) {
			if (zero(openWeek.getActualSpentAmount()) > zero(openWeek.getFinalFreeAmount())) {
				executeRolloverUseCase.execute(openWeek);
			}
		}
	}

	private void reapplyManualPlanAdjustments(BudgetWeek budgetWeek, List<PlanWeekAllocation> weekAllocations) {
		Map<Integer, PlanWeekAllocation> allocationsByPlanId = weekAllocations.stream()
			.filter(allocation -> allocation.getPlan() != null && allocation.getPlan().getId() != null)
			.collect(Collectors.toMap(
				allocation -> allocation.getPlan().getId(),
				allocation -> allocation,
				(left, right) -> left
			));

		for (BudgetAdjustment adjustment : budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtAsc(budgetWeek)) {
			if (adjustment.getPlan() == null || adjustment.getPlan().getId() == null) {
				continue;
			}
			PlanWeekAllocation allocation = allocationsByPlanId.get(adjustment.getPlan().getId());
			if (allocation == null) {
				continue;
			}
			if (adjustment.getAdjustmentType() == BudgetAdjustmentType.PLAN_ADD) {
				allocation.setFinalAmount(zero(allocation.getFinalAmount()) + zero(adjustment.getAdjustmentAmount()));
			}
			if (adjustment.getAdjustmentType() == BudgetAdjustmentType.PLAN_SUBTRACT) {
				allocation.setFinalAmount(Math.max(0L, zero(allocation.getFinalAmount()) - zero(adjustment.getAdjustmentAmount())));
			}
		}
	}

	private void recalculateFinalFreeAmount(BudgetWeek budgetWeek, List<PlanWeekAllocation> weekAllocations) {
		long finalSavingAmount = weekAllocations.stream().mapToLong(allocation -> zero(allocation.getFinalAmount())).sum();
		long planSavedAmount = budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(budgetWeek).stream()
			.filter(adjustment -> adjustment.getAdjustmentType() == BudgetAdjustmentType.PLAN_SAVED_ADD)
			.mapToLong(adjustment -> zero(adjustment.getAdjustmentAmount()))
			.sum();
		budgetWeek.setRolloverInAmount(0L);
		budgetWeek.setFinalFreeAmount(
			zero(budgetWeek.getPlannedIncomeAmount())
				- zero(budgetWeek.getPlannedEssentialAmount())
				- finalSavingAmount
				+ zero(budgetWeek.getManualAdjustmentAmount())
				- planSavedAmount
		);
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}
}
