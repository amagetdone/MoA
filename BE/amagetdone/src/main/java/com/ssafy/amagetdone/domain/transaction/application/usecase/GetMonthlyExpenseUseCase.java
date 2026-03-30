package com.ssafy.amagetdone.domain.transaction.application.usecase;

import java.time.YearMonth;

import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetMonthlyExpenseUseCase {

	private final TransactionQueryService transactionQueryService;

	public GetMonthlyExpenseUseCase(TransactionQueryService transactionQueryService) {
		this.transactionQueryService = transactionQueryService;
	}

	public MonthlyExpenseDto.Response execute(Integer userId, YearMonth month) {
		return transactionQueryService.getMonthlyExpense(userId, month);
	}
}
