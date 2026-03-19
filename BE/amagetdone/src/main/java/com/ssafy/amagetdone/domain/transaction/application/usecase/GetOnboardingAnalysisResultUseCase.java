package com.ssafy.amagetdone.domain.transaction.application.usecase;

import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetOnboardingAnalysisResultUseCase {

	private final TransactionQueryService transactionQueryService;

	public GetOnboardingAnalysisResultUseCase(TransactionQueryService transactionQueryService) {
		this.transactionQueryService = transactionQueryService;
	}

	public TransactionAnalysisDto.OnboardingResult execute(Integer userId) {
		return transactionQueryService.getOnboardingAnalysisResult(userId);
	}
}
