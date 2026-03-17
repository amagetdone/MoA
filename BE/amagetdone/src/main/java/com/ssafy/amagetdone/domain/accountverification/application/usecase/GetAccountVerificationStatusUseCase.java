package com.ssafy.amagetdone.domain.accountverification.application.usecase;

import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationErrorCode;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationException;
import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;
import com.ssafy.amagetdone.domain.accountverification.model.repository.AccountVerificationRepository;

import org.springframework.stereotype.Component;

@Component
public class GetAccountVerificationStatusUseCase {

	private final AccountVerificationRepository accountVerificationRepository;

	public GetAccountVerificationStatusUseCase(AccountVerificationRepository accountVerificationRepository) {
		this.accountVerificationRepository = accountVerificationRepository;
	}

	public AccountVerificationDto.VerificationStatusResponse execute(Integer userId, String accountNo) {
		AccountVerification verification = accountVerificationRepository
			.findTopByAccount_User_IdAndAccount_AccountNoOrderByCreatedAtDesc(userId, accountNo)
			.orElseThrow(() -> new AccountVerificationException(AccountVerificationErrorCode.VERIFICATION_REQUEST_NOT_FOUND));

		return new AccountVerificationDto.VerificationStatusResponse(
			verification.getId(),
			verification.getAccount().getAccountNo(),
			verification.getStatus().name(),
			verification.getAccount().isVerified(),
			verification.getAuthText(),
			verification.getRequestTransactionUniqueNo(),
			verification.getRequestedAt().toString(),
			verification.getVerifiedAt() == null ? null : verification.getVerifiedAt().toString(),
			verification.getFailureReason()
		);
	}
}
