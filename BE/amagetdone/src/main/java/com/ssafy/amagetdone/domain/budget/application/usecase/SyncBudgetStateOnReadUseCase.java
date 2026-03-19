package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.application.service.BudgetReadSyncService;

import org.springframework.stereotype.Component;

@Component
public class SyncBudgetStateOnReadUseCase {

	private final BudgetReadSyncService budgetReadSyncService;

	public SyncBudgetStateOnReadUseCase(BudgetReadSyncService budgetReadSyncService) {
		this.budgetReadSyncService = budgetReadSyncService;
	}

	public void execute(Integer userId) {
		budgetReadSyncService.sync(userId);
	}
}
