package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetSummaryDto;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetSummaryQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetBudgetSummaryUseCase {

	private final BudgetSummaryQueryService budgetSummaryQueryService;

	public GetBudgetSummaryUseCase(BudgetSummaryQueryService budgetSummaryQueryService) {
		this.budgetSummaryQueryService = budgetSummaryQueryService;
	}

	public BudgetSummaryDto.Response execute(Integer userId) {
		return budgetSummaryQueryService.getSummary(userId);
	}
}
