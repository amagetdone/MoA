package com.ssafy.amagetdone.domain.accountverification.application.usecase;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationAdminDto;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationErrorCode;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationException;
import com.ssafy.amagetdone.domain.accountverification.infra.client.SsafyAccountVerificationApiClient;
import com.ssafy.amagetdone.domain.accountverification.infra.client.dto.AccountVerificationClientDto;
import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;
import com.ssafy.amagetdone.domain.accountverification.model.repository.AccountVerificationRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CheckAccountVerificationUseCase {

	private final AccountVerificationRepository accountVerificationRepository;
	private final SsafyAccountVerificationApiClient ssafyAccountVerificationApiClient;

	public CheckAccountVerificationUseCase(
		AccountVerificationRepository accountVerificationRepository,
		SsafyAccountVerificationApiClient ssafyAccountVerificationApiClient
	) {
		this.accountVerificationRepository = accountVerificationRepository;
		this.ssafyAccountVerificationApiClient = ssafyAccountVerificationApiClient;
	}

	@Transactional
	public AccountVerificationAdminDto.CheckVerificationResponse execute(
		AccountVerificationAdminDto.CheckVerificationRequest request
	) {
		AccountVerification verification = accountVerificationRepository
			.findTopByAccount_User_IdAndAccount_AccountNoOrderByCreatedAtDesc(request.userId(), request.accountNo())
			.orElseThrow(() -> new AccountVerificationException(AccountVerificationErrorCode.VERIFICATION_REQUEST_NOT_FOUND));
		Account account = verification.getAccount();

		try {
			AccountVerificationClientDto.CheckVerificationResponse response =
				ssafyAccountVerificationApiClient.checkVerification(
					new AccountVerificationClientDto.CheckVerificationRequest(
						account.getUser().getUserKey(),
						account.getAccountNo(),
						verification.getAuthText(),
						request.authCode()
					)
				);
			verification.markSuccess(LocalDateTime.now());
			account.markVerified();
			accountVerificationRepository.save(verification);
			return new AccountVerificationAdminDto.CheckVerificationResponse(
				verification.getId(),
				response.accountNo(),
				response.status(),
				true,
				response.transactionUniqueNo(),
				verification.getVerifiedAt().toString()
			);
		} catch (AccountVerificationException exception) {
			verification.markFailed(exception.getErrorCode().message());
			accountVerificationRepository.save(verification);
			throw exception;
		}
	}
}
