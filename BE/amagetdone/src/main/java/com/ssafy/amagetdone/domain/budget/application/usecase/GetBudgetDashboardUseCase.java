package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetDashboardDto;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetDashboardQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetBudgetDashboardUseCase {

	private final BudgetDashboardQueryService budgetDashboardQueryService;

	public GetBudgetDashboardUseCase(BudgetDashboardQueryService budgetDashboardQueryService) {
		this.budgetDashboardQueryService = budgetDashboardQueryService;
	}

	public BudgetDashboardDto.Response execute(Integer userId) {
		return budgetDashboardQueryService.getDashboard(userId);
	}
}
