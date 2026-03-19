package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDto;
import com.ssafy.amagetdone.domain.budget.application.service.EssentialExpenseSnapshotQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetWeeklyEssentialExpensesUseCase {

	private final EssentialExpenseSnapshotQueryService essentialExpenseSnapshotQueryService;

	public GetWeeklyEssentialExpensesUseCase(EssentialExpenseSnapshotQueryService essentialExpenseSnapshotQueryService) {
		this.essentialExpenseSnapshotQueryService = essentialExpenseSnapshotQueryService;
	}

	public WeeklyEssentialExpenseDto.Response execute(Integer userId, String yearWeek) {
		return essentialExpenseSnapshotQueryService.getWeeklyEssentialExpenses(userId, yearWeek);
	}
}
