package com.ssafy.amagetdone.domain.accountverification.application.service;

import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
import com.ssafy.amagetdone.domain.accountverification.application.usecase.CheckAccountVerificationUseCase;
import com.ssafy.amagetdone.domain.accountverification.application.usecase.RequestAccountVerificationUseCase;

import org.springframework.stereotype.Service;

@Service
public class AccountVerificationService {

	private final RequestAccountVerificationUseCase requestAccountVerificationUseCase;
	private final CheckAccountVerificationUseCase checkAccountVerificationUseCase;

	public AccountVerificationService(
		RequestAccountVerificationUseCase requestAccountVerificationUseCase,
		CheckAccountVerificationUseCase checkAccountVerificationUseCase
	) {
		this.requestAccountVerificationUseCase = requestAccountVerificationUseCase;
		this.checkAccountVerificationUseCase = checkAccountVerificationUseCase;
	}

	public AccountVerificationDto.RequestVerificationResponse requestVerification(
		Integer userId,
		AccountVerificationDto.RequestVerificationRequest request
	) {
		return requestAccountVerificationUseCase.execute(userId, request);
	}

	public AccountVerificationDto.CheckVerificationResponse checkVerification(
		Integer userId,
		AccountVerificationDto.CheckVerificationRequest request
	) {
		return checkAccountVerificationUseCase.execute(userId, request);
	}
}
