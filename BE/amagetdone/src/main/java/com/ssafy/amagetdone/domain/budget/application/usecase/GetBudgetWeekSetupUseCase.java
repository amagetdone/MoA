package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSetupDto;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetWeekSetupQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetBudgetWeekSetupUseCase {

	private final SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase;
	private final BudgetWeekSetupQueryService budgetWeekSetupQueryService;

	public GetBudgetWeekSetupUseCase(
		SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase,
		BudgetWeekSetupQueryService budgetWeekSetupQueryService
	) {
		this.syncBudgetStateOnReadUseCase = syncBudgetStateOnReadUseCase;
		this.budgetWeekSetupQueryService = budgetWeekSetupQueryService;
	}

	public BudgetWeekSetupDto.Response execute(Integer userId, String yearWeek) {
		syncBudgetStateOnReadUseCase.execute(userId);
		return budgetWeekSetupQueryService.getSetup(userId, yearWeek);
	}
}
