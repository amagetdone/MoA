package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDetailDto;
import com.ssafy.amagetdone.domain.budget.application.service.WeeklyEssentialExpenseDetailQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetWeeklyEssentialExpenseDetailUseCase {

	private final WeeklyEssentialExpenseDetailQueryService weeklyEssentialExpenseDetailQueryService;

	public GetWeeklyEssentialExpenseDetailUseCase(
		WeeklyEssentialExpenseDetailQueryService weeklyEssentialExpenseDetailQueryService
	) {
		this.weeklyEssentialExpenseDetailQueryService = weeklyEssentialExpenseDetailQueryService;
	}

	public WeeklyEssentialExpenseDetailDto.Response execute(Integer userId, String yearWeek) {
		return weeklyEssentialExpenseDetailQueryService.getDetail(userId, yearWeek);
	}
}
