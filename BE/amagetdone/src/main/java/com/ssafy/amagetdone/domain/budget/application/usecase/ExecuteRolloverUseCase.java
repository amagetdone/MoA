package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.application.service.BudgetRolloverService;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;

import org.springframework.stereotype.Component;

@Component
public class ExecuteRolloverUseCase {

	private final BudgetRolloverService budgetRolloverService;

	public ExecuteRolloverUseCase(BudgetRolloverService budgetRolloverService) {
		this.budgetRolloverService = budgetRolloverService;
	}

	public void execute(BudgetWeek sourceWeek) {
		budgetRolloverService.executeRollover(sourceWeek);
	}
}
