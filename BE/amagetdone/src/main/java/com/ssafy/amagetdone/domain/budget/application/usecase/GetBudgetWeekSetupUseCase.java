package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSetupDto;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetWeekSetupQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetBudgetWeekSetupUseCase {

	private final BudgetWeekSetupQueryService budgetWeekSetupQueryService;

	public GetBudgetWeekSetupUseCase(BudgetWeekSetupQueryService budgetWeekSetupQueryService) {
		this.budgetWeekSetupQueryService = budgetWeekSetupQueryService;
	}

	public BudgetWeekSetupDto.Response execute(Integer userId, String yearWeek) {
		return budgetWeekSetupQueryService.getSetup(userId, yearWeek);
	}
}
