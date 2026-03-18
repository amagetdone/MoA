package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetIncomeDto;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetIncomeQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetBudgetIncomeUseCase {

	private final BudgetIncomeQueryService budgetIncomeQueryService;

	public GetBudgetIncomeUseCase(BudgetIncomeQueryService budgetIncomeQueryService) {
		this.budgetIncomeQueryService = budgetIncomeQueryService;
	}

	public BudgetIncomeDto.Response execute(Integer userId) {
		return budgetIncomeQueryService.getCurrentIncome(userId);
	}
}
