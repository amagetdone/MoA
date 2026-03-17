package com.ssafy.amagetdone.domain.accountverification.application.service;

import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
import com.ssafy.amagetdone.domain.accountverification.application.usecase.CheckAccountVerificationUseCase;
import com.ssafy.amagetdone.domain.accountverification.application.usecase.GetAccountVerificationStatusUseCase;
import com.ssafy.amagetdone.domain.accountverification.application.usecase.RequestAccountVerificationUseCase;

import org.springframework.stereotype.Service;

@Service
public class AccountVerificationService {

	private final RequestAccountVerificationUseCase requestAccountVerificationUseCase;
	private final CheckAccountVerificationUseCase checkAccountVerificationUseCase;
	private final GetAccountVerificationStatusUseCase getAccountVerificationStatusUseCase;

	public AccountVerificationService(
		RequestAccountVerificationUseCase requestAccountVerificationUseCase,
		CheckAccountVerificationUseCase checkAccountVerificationUseCase,
		GetAccountVerificationStatusUseCase getAccountVerificationStatusUseCase
	) {
		this.requestAccountVerificationUseCase = requestAccountVerificationUseCase;
		this.checkAccountVerificationUseCase = checkAccountVerificationUseCase;
		this.getAccountVerificationStatusUseCase = getAccountVerificationStatusUseCase;
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

	public AccountVerificationDto.VerificationStatusResponse getVerificationStatus(Integer userId, String accountNo) {
		return getAccountVerificationStatusUseCase.execute(userId, accountNo);
	}
}
