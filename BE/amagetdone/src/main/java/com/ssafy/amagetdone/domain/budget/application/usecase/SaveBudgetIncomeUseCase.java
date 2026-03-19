package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetIncomeDto;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetIncomeCommandService;

import org.springframework.stereotype.Component;

@Component
public class SaveBudgetIncomeUseCase {

	private final BudgetIncomeCommandService budgetIncomeCommandService;

	public SaveBudgetIncomeUseCase(BudgetIncomeCommandService budgetIncomeCommandService) {
		this.budgetIncomeCommandService = budgetIncomeCommandService;
	}

	public BudgetIncomeDto.Response execute(Integer userId, BudgetIncomeDto.Request request) {
		return budgetIncomeCommandService.saveIncome(userId, request);
	}
}
