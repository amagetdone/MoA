package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetAdjustmentDto;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetAdjustmentCommandService;

import org.springframework.stereotype.Component;

@Component
public class SaveBudgetAdjustmentUseCase {

	private final BudgetAdjustmentCommandService budgetAdjustmentCommandService;

	public SaveBudgetAdjustmentUseCase(BudgetAdjustmentCommandService budgetAdjustmentCommandService) {
		this.budgetAdjustmentCommandService = budgetAdjustmentCommandService;
	}

	public BudgetAdjustmentDto.Response execute(Integer userId, String yearWeek, BudgetAdjustmentDto.Request request) {
		return budgetAdjustmentCommandService.saveAdjustment(userId, yearWeek, request);
	}
}
