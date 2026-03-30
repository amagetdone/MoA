package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.application.service.WeeklySavingsDetailQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetWeeklySavingsDetailUseCase {

	private final SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase;
	private final WeeklySavingsDetailQueryService weeklySavingsDetailQueryService;

	public GetWeeklySavingsDetailUseCase(
		SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase,
		WeeklySavingsDetailQueryService weeklySavingsDetailQueryService
	) {
		this.syncBudgetStateOnReadUseCase = syncBudgetStateOnReadUseCase;
		this.weeklySavingsDetailQueryService = weeklySavingsDetailQueryService;
	}

	public WeeklySavingsDetailDto.Response execute(Integer userId, String yearWeek) {
		syncBudgetStateOnReadUseCase.execute(userId);
		return weeklySavingsDetailQueryService.getDetail(userId, yearWeek);
	}
}
