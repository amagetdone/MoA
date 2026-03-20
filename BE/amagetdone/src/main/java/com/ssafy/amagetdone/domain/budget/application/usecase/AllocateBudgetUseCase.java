package com.ssafy.amagetdone.domain.budget.application.usecase;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.application.service.BudgetAllocationService;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;

import org.springframework.stereotype.Component;

@Component
public class AllocateBudgetUseCase {

	private final BudgetAllocationService budgetAllocationService;

	public AllocateBudgetUseCase(BudgetAllocationService budgetAllocationService) {
		this.budgetAllocationService = budgetAllocationService;
	}

	public BudgetAllocationService.AllocationResult execute(
		User user,
		LocalDate serviceStartDate,
		Long monthlyIncome,
		List<EssentialExpenseMonth> essentials,
		List<Plan> activePlans
	) {
		return budgetAllocationService.allocateBudget(user, serviceStartDate, monthlyIncome, essentials, activePlans);
	}

	public BudgetAllocationService.AllocationResult execute(
		User user,
		LocalDate serviceStartDate,
		Long monthlyIncome,
		List<EssentialExpenseMonth> essentials,
		List<Plan> activePlans,
		LocalDate allocationEndDate
	) {
		return budgetAllocationService.allocateBudget(
			user,
			serviceStartDate,
			monthlyIncome,
			essentials,
			activePlans,
			allocationEndDate
		);
	}
}
