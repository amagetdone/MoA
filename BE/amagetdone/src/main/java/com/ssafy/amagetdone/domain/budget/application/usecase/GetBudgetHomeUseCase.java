package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetHomeDto;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetHomeQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetBudgetHomeUseCase {

	private final SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase;
	private final BudgetHomeQueryService budgetHomeQueryService;

	public GetBudgetHomeUseCase(
		SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase,
		BudgetHomeQueryService budgetHomeQueryService
	) {
		this.syncBudgetStateOnReadUseCase = syncBudgetStateOnReadUseCase;
		this.budgetHomeQueryService = budgetHomeQueryService;
	}

	public BudgetHomeDto.Response execute(Integer userId) {
		syncBudgetStateOnReadUseCase.execute(userId);
		return budgetHomeQueryService.getHome(userId);
	}
}
