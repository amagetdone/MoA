package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.application.service.BudgetAllocationTriggerService;

import org.springframework.stereotype.Component;

@Component
public class TriggerBudgetAllocationUseCase {

	private final BudgetAllocationTriggerService budgetAllocationTriggerService;

	public TriggerBudgetAllocationUseCase(BudgetAllocationTriggerService budgetAllocationTriggerService) {
		this.budgetAllocationTriggerService = budgetAllocationTriggerService;
	}

	public void execute(Integer userId) {
		budgetAllocationTriggerService.trigger(userId);
	}
}
