package com.ssafy.amagetdone.domain.accountverification.application.usecase;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.account.infra.client.SsafyDemandDepositAccountApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.DemandDepositAccountDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationErrorCode;
import com.ssafy.amagetdone.domain.accountverification.exception.AccountVerificationException;
import com.ssafy.amagetdone.domain.accountverification.infra.client.SsafyAccountVerificationApiClient;
import com.ssafy.amagetdone.domain.accountverification.infra.client.dto.AccountVerificationClientDto;
import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;
import com.ssafy.amagetdone.domain.accountverification.model.repository.AccountVerificationRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Validates auth code via Ssafy API and registers the account in local DB on success.
@Component
public class CheckAccountVerificationUseCase {

	private final AccountVerificationRepository accountVerificationRepository;
	private final AccountRepository accountRepository;
	private final SsafyAccountVerificationApiClient ssafyAccountVerificationApiClient;
	private final SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient;

	public CheckAccountVerificationUseCase(
		AccountVerificationRepository accountVerificationRepository,
		AccountRepository accountRepository,
		SsafyAccountVerificationApiClient ssafyAccountVerificationApiClient,
		SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient
	) {
		this.accountVerificationRepository = accountVerificationRepository;
		this.accountRepository = accountRepository;
		this.ssafyAccountVerificationApiClient = ssafyAccountVerificationApiClient;
		this.ssafyDemandDepositAccountApiClient = ssafyDemandDepositAccountApiClient;
	}

	@Transactional
	public AccountVerificationDto.CheckVerificationResponse execute(
		Integer userId,
		AccountVerificationDto.CheckVerificationRequest request
	) {
		AccountVerification verification = accountVerificationRepository
			.findTopByUser_IdAndAccountNoOrderByCreatedAtDesc(userId, request.accountNo())
			.orElseThrow(() -> new AccountVerificationException(AccountVerificationErrorCode.VERIFICATION_REQUEST_NOT_FOUND));
		User user = verification.getUser();

		try {
			AccountVerificationClientDto.CheckVerificationResponse response =
				ssafyAccountVerificationApiClient.checkVerification(
					new AccountVerificationClientDto.CheckVerificationRequest(
						user.getUserKey(),
						verification.getAccountNo(),
						verification.getAuthText(),
						request.authCode()
					)
				);
			verification.markSuccess(LocalDateTime.now());
			accountVerificationRepository.save(verification);

			Account account = registerVerifiedAccount(user, verification.getAccountNo());

			return new AccountVerificationDto.CheckVerificationResponse(
				verification.getId(),
				response.accountNo(),
				response.status(),
				account.isVerified(),
				response.transactionUniqueNo(),
				verification.getVerifiedAt().toString()
			);
		} catch (AccountVerificationException exception) {
			verification.markFailed(exception.getErrorCode().message());
			accountVerificationRepository.save(verification);
			throw exception;
		}
	}

	// Fetches account details from Ssafy API and saves to local DB as verified.
	private Account registerVerifiedAccount(User user, String accountNo) {
		Account account = accountRepository.findByUserIdAndAccountNo(user.getId(), accountNo)
			.orElseGet(() -> {
				DemandDepositAccountDto.AccountListResponse accountList =
					ssafyDemandDepositAccountApiClient.inquireAccounts(
						new DemandDepositAccountDto.AccountListRequest(user.getUserKey())
					);
				DemandDepositAccountDto.AccountItem matched = accountList.accounts().stream()
					.filter(item -> item.accountNo().equals(accountNo))
					.findFirst()
					.orElseThrow(() -> new AccountVerificationException(
						AccountVerificationErrorCode.VERIFICATION_REQUEST_NOT_FOUND));

				return Account.create(
					user,
					matched.accountNo(),
					null,
					matched.accountName(),
					matched.accountTypeName(),
					matched.bankCode(),
					matched.bankName(),
					matched.currency()
				);
			});
		account.markVerified();
		return accountRepository.save(account);
	}
}
