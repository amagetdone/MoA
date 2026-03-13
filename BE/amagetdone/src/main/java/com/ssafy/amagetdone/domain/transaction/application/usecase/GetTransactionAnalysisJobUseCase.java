package com.ssafy.amagetdone.domain.transaction.application.usecase;

import com.ssafy.amagetdone.domain.transaction.application.service.TransactionAnalysisService;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisJob;

import org.springframework.stereotype.Component;

@Component
public class GetTransactionAnalysisJobUseCase {

	private final TransactionAnalysisService transactionAnalysisService;

	public GetTransactionAnalysisJobUseCase(TransactionAnalysisService transactionAnalysisService) {
		this.transactionAnalysisService = transactionAnalysisService;
	}

	public TransactionAnalysisJob execute(String jobId) {
		return transactionAnalysisService.getJob(jobId);
	}
}
