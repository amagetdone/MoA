package com.ssafy.amagetdone.domain.transaction.application.usecase;

import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetWeeklyTransactionAnalysisUseCase {

	private final TransactionQueryService transactionQueryService;

	public GetWeeklyTransactionAnalysisUseCase(TransactionQueryService transactionQueryService) {
		this.transactionQueryService = transactionQueryService;
	}

	public WeeklyTransactionAnalysisDto.Response execute(Integer userId, String yearWeek) {
		return transactionQueryService.getWeeklyTransactionAnalysis(userId, yearWeek);
	}
}
