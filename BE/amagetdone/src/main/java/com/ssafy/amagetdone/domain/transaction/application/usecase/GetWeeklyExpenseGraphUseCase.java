package com.ssafy.amagetdone.domain.transaction.application.usecase;

import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetWeeklyExpenseGraphUseCase {

	private final TransactionQueryService transactionQueryService;

	public GetWeeklyExpenseGraphUseCase(TransactionQueryService transactionQueryService) {
		this.transactionQueryService = transactionQueryService;
	}

	public WeeklyExpenseGraphDto.Response execute(Integer userId, String yearWeek) {
		return transactionQueryService.getWeeklyExpenseGraph(userId, yearWeek);
	}
}
