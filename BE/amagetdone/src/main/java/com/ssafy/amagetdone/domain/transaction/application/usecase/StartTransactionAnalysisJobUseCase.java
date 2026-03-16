package com.ssafy.amagetdone.domain.transaction.application.usecase;

import java.util.concurrent.CompletableFuture;

import com.ssafy.amagetdone.domain.transaction.application.service.TransactionAnalysisService;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisJob;

import org.springframework.stereotype.Component;

@Component
public class StartTransactionAnalysisJobUseCase {

	private final TransactionAnalysisService transactionAnalysisService;

	public StartTransactionAnalysisJobUseCase(TransactionAnalysisService transactionAnalysisService) {
		this.transactionAnalysisService = transactionAnalysisService;
	}

	public TransactionAnalysisJob execute(Integer userId, Integer months, boolean forceFullSync) {
		TransactionAnalysisJob job = transactionAnalysisService.createJob(userId, months);
		CompletableFuture.runAsync(() -> transactionAnalysisService.runAnalysis(job.getId(), forceFullSync));
		return job;
	}
}
