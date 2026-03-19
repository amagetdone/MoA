package com.ssafy.amagetdone.domain.budget.application.usecase;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.application.service.WeeklySavingsDetailQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetWeeklySavingsDetailUseCase {

	private final WeeklySavingsDetailQueryService weeklySavingsDetailQueryService;

	public GetWeeklySavingsDetailUseCase(WeeklySavingsDetailQueryService weeklySavingsDetailQueryService) {
		this.weeklySavingsDetailQueryService = weeklySavingsDetailQueryService;
	}

	public WeeklySavingsDetailDto.Response execute(Integer userId, String yearWeek) {
		return weeklySavingsDetailQueryService.getDetail(userId, yearWeek);
	}
}
