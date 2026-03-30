package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyFreeSpendingDetailDto;
import com.ssafy.amagetdone.domain.budget.application.service.WeeklyFreeSpendingDetailQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetWeeklyFreeSpendingDetailUseCase {

	private final SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase;
	private final WeeklyFreeSpendingDetailQueryService weeklyFreeSpendingDetailQueryService;

	public GetWeeklyFreeSpendingDetailUseCase(
		SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase,
		WeeklyFreeSpendingDetailQueryService weeklyFreeSpendingDetailQueryService
	) {
		this.syncBudgetStateOnReadUseCase = syncBudgetStateOnReadUseCase;
		this.weeklyFreeSpendingDetailQueryService = weeklyFreeSpendingDetailQueryService;
	}

	public WeeklyFreeSpendingDetailDto.Response execute(Integer userId, String yearWeek) {
		syncBudgetStateOnReadUseCase.execute(userId);
		return weeklyFreeSpendingDetailQueryService.getDetail(userId, yearWeek);
	}
}
