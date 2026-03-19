package com.ssafy.amagetdone.domain.plan.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateResponse;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlanCreationBudgetSummaryService {

	private final BudgetWeekRepository budgetWeekRepository;
	private final EssentialWeekAllocationRepository essentialWeekAllocationRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final Clock clock;

	public PlanCreationBudgetSummaryService(
		BudgetWeekRepository budgetWeekRepository,
		EssentialWeekAllocationRepository essentialWeekAllocationRepository,
		PlanWeekAllocationRepository planWeekAllocationRepository,
		Clock clock
	) {
		this.budgetWeekRepository = budgetWeekRepository;
		this.essentialWeekAllocationRepository = essentialWeekAllocationRepository;
		this.planWeekAllocationRepository = planWeekAllocationRepository;
		this.clock = clock;
	}

	public PlanCreateResponse build(Plan plan) {
		BudgetWeek summaryWeek = resolveSummaryWeek(plan);
		long weeklySavingAmount = resolveBaseWeeklyAmount(plan);
		if (summaryWeek == null) {
			return new PlanCreateResponse(
				plan.getId(),
				plan.getTitle(),
				new PlanCreateResponse.WeeklyMission(weeklySavingAmount, zero(plan.getTargetWeeks())),
				new PlanCreateResponse.WeeklyBudget(
					0L,
					weeklySavingAmount,
					new PlanCreateResponse.BudgetBucket(0L, List.of()),
					new PlanCreateResponse.BudgetBucket(0L, List.of()),
					0L
				),
				plan.getStatus().name()
			);
		}

		List<EssentialWeekAllocation> essentials = essentialWeekAllocationRepository.findByBudgetWeek(summaryWeek);
		List<PlanWeekAllocation> savings = planWeekAllocationRepository.findByBudgetWeek(summaryWeek);
		weeklySavingAmount = resolveWeeklySavingAmount(plan, savings);

		return new PlanCreateResponse(
			plan.getId(),
			plan.getTitle(),
			new PlanCreateResponse.WeeklyMission(weeklySavingAmount, zero(plan.getTargetWeeks())),
			new PlanCreateResponse.WeeklyBudget(
				zero(summaryWeek.getPlannedIncomeAmount()),
				weeklySavingAmount,
				toBudgetBucket(essentials, ExpenseScope.FIXED),
				toBudgetBucket(essentials, ExpenseScope.VARIABLE),
				zero(summaryWeek.getFinalFreeAmount())
			),
			plan.getStatus().name()
		);
	}

	private BudgetWeek resolveSummaryWeek(Plan plan) {
		LocalDate summaryDate = plan.getStatus() == PlanStatus.ACTIVE ? LocalDate.now(clock) : plan.getStartDate();
		return budgetWeekRepository.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(
			plan.getUser(),
			summaryDate,
			summaryDate
		).orElseGet(() -> budgetWeekRepository.findFirstByUserOrderByWeekEndDateDesc(plan.getUser()).orElse(null));
	}

	private PlanCreateResponse.BudgetBucket toBudgetBucket(List<EssentialWeekAllocation> essentials, ExpenseScope scope) {
		List<PlanCreateResponse.BudgetItem> items = essentials.stream()
			.filter(allocation -> allocation.getExpenseScope() == scope)
			.sorted(Comparator.comparing(allocation -> allocation.getCategory().getId()))
			.map(allocation -> new PlanCreateResponse.BudgetItem(
				allocation.getCategory().getName(),
				zero(allocation.getPlannedAmount())
			))
			.toList();

		long totalAmount = items.stream()
			.mapToLong(PlanCreateResponse.BudgetItem::amount)
			.sum();

		return new PlanCreateResponse.BudgetBucket(totalAmount, items);
	}

	private long resolveWeeklySavingAmount(Plan plan, List<PlanWeekAllocation> savings) {
		return savings.stream()
			.filter(allocation -> allocation.getPlan() != null)
			.filter(allocation -> plan.getId() != null && plan.getId().equals(allocation.getPlan().getId()))
			.map(PlanWeekAllocation::getPlannedAmount)
			.findFirst()
			.map(this::zero)
			.orElseGet(() -> resolveBaseWeeklyAmount(plan));
	}

	private long resolveBaseWeeklyAmount(Plan plan) {
		long targetAmount = zero(plan.getTargetAmount());
		int targetWeeks = zero(plan.getTargetWeeks());
		if (targetAmount <= 0L || targetWeeks <= 0) {
			return 0L;
		}
		return targetAmount / targetWeeks;
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}

	private int zero(Integer value) {
		return value == null ? 0 : value;
	}
}
