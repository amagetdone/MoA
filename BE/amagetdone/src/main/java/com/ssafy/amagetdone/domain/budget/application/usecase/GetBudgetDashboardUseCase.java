package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetDashboardDto;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetDashboardQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetBudgetDashboardUseCase {

	private final SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase;
	private final BudgetDashboardQueryService budgetDashboardQueryService;

	public GetBudgetDashboardUseCase(
		SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase,
		BudgetDashboardQueryService budgetDashboardQueryService
	) {
		this.syncBudgetStateOnReadUseCase = syncBudgetStateOnReadUseCase;
		this.budgetDashboardQueryService = budgetDashboardQueryService;
	}

	public BudgetDashboardDto.Response execute(Integer userId) {
		syncBudgetStateOnReadUseCase.execute(userId);
		return budgetDashboardQueryService.getDashboard(userId);
	}
}
