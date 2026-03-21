package com.ssafy.amagetdone.domain.transaction.application.usecase;

import java.time.YearMonth;

import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionQueryService;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;

import org.springframework.stereotype.Component;

@Component
public class GetMonthlyExecutionAnalysisResultUseCase {

	private final TransactionQueryService transactionQueryService;

	public GetMonthlyExecutionAnalysisResultUseCase(TransactionQueryService transactionQueryService) {
		this.transactionQueryService = transactionQueryService;
	}

	public TransactionAnalysisDto.OnboardingResult execute(
		Integer userId,
		TransactionAnalysisMetadata metadata,
		YearMonth month
	) {
		return transactionQueryService.getMonthlyExecutionAnalysisResult(userId, metadata, month);
	}
}
