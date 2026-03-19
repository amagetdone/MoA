// Responsibility: Completes 1-won verification and persists the verified linked-account state.
package com.ssafy.amagetdone.domain.accountverification.application.usecase;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.account.application.usecase.SyncLinkedCardsUseCase;
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
	private final SyncLinkedCardsUseCase syncLinkedCardsUseCase;

	public CheckAccountVerificationUseCase(
		AccountVerificationRepository accountVerificationRepository,
		AccountRepository accountRepository,
		SsafyAccountVerificationApiClient ssafyAccountVerificationApiClient,
		SsafyDemandDepositAccountApiClient ssafyDemandDepositAccountApiClient,
		SyncLinkedCardsUseCase syncLinkedCardsUseCase
	) {
		this.accountVerificationRepository = accountVerificationRepository;
		this.accountRepository = accountRepository;
		this.ssafyAccountVerificationApiClient = ssafyAccountVerificationApiClient;
		this.ssafyDemandDepositAccountApiClient = ssafyDemandDepositAccountApiClient;
		this.syncLinkedCardsUseCase = syncLinkedCardsUseCase;
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
			syncLinkedCardsUseCase.execute(user.getId(), user.getUserKey(), account.getAccountNo());

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
		DemandDepositAccountDto.AccountDetailResponse matched = ssafyDemandDepositAccountApiClient.inquireAccount(
			new DemandDepositAccountDto.AccountDetailRequest(user.getUserKey(), accountNo)
		);
		Account account = accountRepository.findByUserIdAndAccountNo(user.getId(), accountNo)
			.orElseGet(() -> Account.create(
				user,
				matched.accountNo(),
				null,
				matched.accountName(),
				matched.accountTypeName(),
				matched.bankCode(),
				matched.bankName(),
				matched.currency()
			));
		account.sync(
			null,
			matched.accountName(),
			matched.accountTypeName(),
			matched.bankCode(),
			matched.bankName(),
			matched.currency()
		);
		account.markVerifiedLinked();
		return accountRepository.save(account);
	}
}
