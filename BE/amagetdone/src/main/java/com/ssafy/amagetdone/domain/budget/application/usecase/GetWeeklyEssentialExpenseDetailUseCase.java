package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDetailDto;
import com.ssafy.amagetdone.domain.budget.application.service.WeeklyEssentialExpenseDetailQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetWeeklyEssentialExpenseDetailUseCase {

	private final SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase;
	private final WeeklyEssentialExpenseDetailQueryService weeklyEssentialExpenseDetailQueryService;

	public GetWeeklyEssentialExpenseDetailUseCase(
		SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase,
		WeeklyEssentialExpenseDetailQueryService weeklyEssentialExpenseDetailQueryService
	) {
		this.syncBudgetStateOnReadUseCase = syncBudgetStateOnReadUseCase;
		this.weeklyEssentialExpenseDetailQueryService = weeklyEssentialExpenseDetailQueryService;
	}

	public WeeklyEssentialExpenseDetailDto.Response execute(Integer userId, String yearWeek) {
		syncBudgetStateOnReadUseCase.execute(userId);
		return weeklyEssentialExpenseDetailQueryService.getDetail(userId, yearWeek);
	}
}
