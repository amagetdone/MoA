package com.ssafy.amagetdone.domain.transaction.application.usecase;

import java.time.YearMonth;

import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetExpenseSummaryUseCase {

	private final TransactionQueryService transactionQueryService;

	public GetExpenseSummaryUseCase(TransactionQueryService transactionQueryService) {
		this.transactionQueryService = transactionQueryService;
	}

	public ExpenseSummaryDto.Response execute(Integer userId, YearMonth fromMonth, YearMonth toMonth, String bucket) {
		return transactionQueryService.getExpenseSummary(userId, fromMonth, toMonth, bucket);
	}
}
