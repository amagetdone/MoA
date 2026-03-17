package com.ssafy.amagetdone.domain.accountverification.application.usecase;

import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationErrorCode;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationException;
import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;
import com.ssafy.amagetdone.domain.accountverification.model.repository.AccountVerificationRepository;

import org.springframework.stereotype.Component;

@Component
public class GetAccountVerificationStatusUseCase {

	private final AccountVerificationRepository accountVerificationRepository;
	private final AccountRepository accountRepository;

	public GetAccountVerificationStatusUseCase(
		AccountVerificationRepository accountVerificationRepository,
		AccountRepository accountRepository
	) {
		this.accountVerificationRepository = accountVerificationRepository;
		this.accountRepository = accountRepository;
	}

	public AccountVerificationDto.VerificationStatusResponse execute(Integer userId, String accountNo) {
		AccountVerification verification = accountVerificationRepository
			.findTopByUser_IdAndAccountNoOrderByCreatedAtDesc(userId, accountNo)
			.orElseThrow(() -> new AccountVerificationException(AccountVerificationErrorCode.VERIFICATION_REQUEST_NOT_FOUND));

		boolean verified = accountRepository.findByUserIdAndAccountNo(userId, accountNo)
			.map(account -> account.isVerified())
			.orElse(false);

		return new AccountVerificationDto.VerificationStatusResponse(
			verification.getId(),
			verification.getAccountNo(),
			verification.getStatus().name(),
			verified,
			verification.getAuthText(),
			verification.getRequestTransactionUniqueNo(),
			verification.getRequestedAt().toString(),
			verification.getVerifiedAt() == null ? null : verification.getVerifiedAt().toString(),
			verification.getFailureReason()
		);
	}
}
