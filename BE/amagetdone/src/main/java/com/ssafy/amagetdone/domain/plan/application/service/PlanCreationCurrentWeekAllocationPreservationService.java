package com.ssafy.amagetdone.domain.plan.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlanCreationCurrentWeekAllocationPreservationService {

	private final BudgetWeekRepository budgetWeekRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public List<PreservedAllocation> snapshotOtherPlanAllocations(Integer userId, Integer excludedPlanId) {
		return findCurrentWeek(userId)
			.map(planWeekAllocationRepository::findByBudgetWeek)
			.orElse(List.of())
			.stream()
			.filter(allocation -> allocation.getPlan() != null)
			.filter(allocation -> allocation.getPlan().getId() != null)
			.filter(allocation -> !Objects.equals(allocation.getPlan().getId(), excludedPlanId))
			.map(allocation -> new PreservedAllocation(
				allocation.getPlan(),
				allocation.getAllocationType(),
				allocation.getPlannedAmount(),
				allocation.getFinalAmount(),
				allocation.getActualAmount(),
				allocation.getNote()
			))
			.toList();
	}

	@Transactional
	public void restoreOtherPlanAllocations(
		Integer userId,
		Integer excludedPlanId,
		List<PreservedAllocation> preservedAllocations
	) {
		if (preservedAllocations == null) {
			return;
		}

		BudgetWeek currentWeek = findCurrentWeek(userId).orElse(null);
		if (currentWeek == null) {
			return;
		}

		List<PlanWeekAllocation> currentAllocations = planWeekAllocationRepository.findByBudgetWeek(currentWeek);
		Map<Integer, PlanWeekAllocation> currentAllocationsByPlanId = currentAllocations.stream()
			.filter(allocation -> allocation.getPlan() != null)
			.filter(allocation -> allocation.getPlan().getId() != null)
			.filter(allocation -> !Objects.equals(allocation.getPlan().getId(), excludedPlanId))
			.collect(Collectors.toMap(
				allocation -> allocation.getPlan().getId(),
				allocation -> allocation,
				(left, right) -> left,
				LinkedHashMap::new
			));
		Set<Integer> preservedPlanIds = preservedAllocations.stream()
			.map(PreservedAllocation::plan)
			.filter(Objects::nonNull)
			.map(Plan::getId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		List<PlanWeekAllocation> allocationsToDelete = currentAllocationsByPlanId.entrySet().stream()
			.filter(entry -> !preservedPlanIds.contains(entry.getKey()))
			.map(Map.Entry::getValue)
			.toList();
		if (!allocationsToDelete.isEmpty()) {
			planWeekAllocationRepository.deleteAll(allocationsToDelete);
		}

		List<PlanWeekAllocation> allocationsToSave = preservedAllocations.stream()
			.map(preservedAllocation -> restoreAllocation(currentWeek, currentAllocationsByPlanId, preservedAllocation))
			.filter(Objects::nonNull)
			.toList();
		if (!allocationsToSave.isEmpty()) {
			planWeekAllocationRepository.saveAll(allocationsToSave);
		}
	}

	private PlanWeekAllocation restoreAllocation(
		BudgetWeek currentWeek,
		Map<Integer, PlanWeekAllocation> currentAllocationsByPlanId,
		PreservedAllocation preservedAllocation
	) {
		Plan plan = preservedAllocation.plan();
		if (plan == null || plan.getId() == null) {
			return null;
		}

		PlanWeekAllocation allocation = currentAllocationsByPlanId.get(plan.getId());
		if (allocation == null) {
			return PlanWeekAllocation.builder()
				.budgetWeek(currentWeek)
				.plan(plan)
				.allocationType(preservedAllocation.allocationType())
				.plannedAmount(preservedAllocation.plannedAmount())
				.finalAmount(preservedAllocation.finalAmount())
				.actualAmount(preservedAllocation.actualAmount())
				.note(preservedAllocation.note())
				.build();
		}

		allocation.setAllocationType(preservedAllocation.allocationType());
		allocation.setPlannedAmount(preservedAllocation.plannedAmount());
		allocation.setFinalAmount(preservedAllocation.finalAmount());
		allocation.setActualAmount(preservedAllocation.actualAmount());
		allocation.setNote(preservedAllocation.note());
		return allocation;
	}

	private java.util.Optional<BudgetWeek> findCurrentWeek(Integer userId) {
		LocalDate today = LocalDate.now(clock);
		return budgetWeekRepository
			.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(userId, today, today);
	}

	public record PreservedAllocation(
		Plan plan,
		String allocationType,
		Long plannedAmount,
		Long finalAmount,
		Long actualAmount,
		String note
	) {
	}
}
