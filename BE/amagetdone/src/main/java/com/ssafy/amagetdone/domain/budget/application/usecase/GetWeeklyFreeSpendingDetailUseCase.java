package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyFreeSpendingDetailDto;
import com.ssafy.amagetdone.domain.budget.application.service.WeeklyFreeSpendingDetailQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetWeeklyFreeSpendingDetailUseCase {

	private final WeeklyFreeSpendingDetailQueryService weeklyFreeSpendingDetailQueryService;

	public GetWeeklyFreeSpendingDetailUseCase(
		WeeklyFreeSpendingDetailQueryService weeklyFreeSpendingDetailQueryService
	) {
		this.weeklyFreeSpendingDetailQueryService = weeklyFreeSpendingDetailQueryService;
	}

	public WeeklyFreeSpendingDetailDto.Response execute(Integer userId, String yearWeek) {
		return weeklyFreeSpendingDetailQueryService.getDetail(userId, yearWeek);
	}
}
