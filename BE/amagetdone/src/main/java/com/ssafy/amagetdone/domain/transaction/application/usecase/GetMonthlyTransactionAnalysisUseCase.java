package com.ssafy.amagetdone.domain.transaction.application.usecase;

import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetMonthlyTransactionAnalysisUseCase {

	private final TransactionQueryService transactionQueryService;

	public GetMonthlyTransactionAnalysisUseCase(TransactionQueryService transactionQueryService) {
		this.transactionQueryService = transactionQueryService;
	}

	public MonthlyTransactionAnalysisDto.Response execute(Integer userId, String yearMonth) {
		return transactionQueryService.getMonthlyTransactionAnalysis(userId, yearMonth);
	}
}
