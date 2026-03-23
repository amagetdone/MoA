package com.ssafy.amagetdone.domain.transaction.application.usecase;

import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetMonthlyExpenseGraphUseCase {

	private final TransactionQueryService transactionQueryService;

	public GetMonthlyExpenseGraphUseCase(TransactionQueryService transactionQueryService) {
		this.transactionQueryService = transactionQueryService;
	}

	public MonthlyExpenseGraphDto.Response execute(Integer userId, String yearMonth) {
		return transactionQueryService.getMonthlyExpenseGraph(userId, yearMonth);
	}
}
