package com.ssafy.amagetdone.domain.transaction.application.usecase;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisJobDto;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionQueryService;

import org.springframework.stereotype.Component;

@Component
public class GetOnboardingAnalysisResultUseCase {

	private final TransactionQueryService transactionQueryService;

	public GetOnboardingAnalysisResultUseCase(TransactionQueryService transactionQueryService) {
		this.transactionQueryService = transactionQueryService;
	}

	public TransactionAnalysisJobDto.OnboardingResult execute(Integer userId, LocalDateTime referenceDateTime) {
		return transactionQueryService.getOnboardingAnalysisResult(userId, referenceDateTime);
	}
}
