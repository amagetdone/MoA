package com.ssafy.amagetdone.domain.transaction.application.usecase;

import java.time.YearMonth;

import com.ssafy.amagetdone.domain.transaction.application.service.TransactionAnalysisService;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;

import org.springframework.stereotype.Component;

@Component
public class RunTransactionAnalysisUseCase {

	private final TransactionAnalysisService transactionAnalysisService;

	public RunTransactionAnalysisUseCase(TransactionAnalysisService transactionAnalysisService) {
		this.transactionAnalysisService = transactionAnalysisService;
	}

	public TransactionAnalysisMetadata execute(Integer userId, Integer months) {
		return transactionAnalysisService.analyzeTransactions(userId, months);
	}

	public TransactionAnalysisMetadata execute(Integer userId, YearMonth month) {
		return transactionAnalysisService.analyzeTransactions(userId, month);
	}
}
