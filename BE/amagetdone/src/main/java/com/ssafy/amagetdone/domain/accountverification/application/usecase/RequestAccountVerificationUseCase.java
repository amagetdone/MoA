package com.ssafy.amagetdone.domain.accountverification.application.usecase;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.account.application.usecase.FindUserAccountUseCase;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
import com.ssafy.amagetdone.domain.accountverification.infra.client.SsafyAccountVerificationApiClient;
import com.ssafy.amagetdone.domain.accountverification.infra.client.dto.AccountVerificationClientDto;
import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;
import com.ssafy.amagetdone.domain.accountverification.model.repository.AccountVerificationRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RequestAccountVerificationUseCase {

	private static final String AUTH_TEXT = "AMAGETDONE";

	private final FindUserAccountUseCase findUserAccountUseCase;
	private final AccountVerificationRepository accountVerificationRepository;
	private final SsafyAccountVerificationApiClient ssafyAccountVerificationApiClient;

	public RequestAccountVerificationUseCase(
		FindUserAccountUseCase findUserAccountUseCase,
		AccountVerificationRepository accountVerificationRepository,
		SsafyAccountVerificationApiClient ssafyAccountVerificationApiClient
	) {
		this.findUserAccountUseCase = findUserAccountUseCase;
		this.accountVerificationRepository = accountVerificationRepository;
		this.ssafyAccountVerificationApiClient = ssafyAccountVerificationApiClient;
	}

	@Transactional
	public AccountVerificationDto.RequestVerificationResponse execute(
		Integer userId,
		AccountVerificationDto.RequestVerificationRequest request
	) {
		Account account = findUserAccountUseCase.execute(userId, request.accountNo());
		AccountVerificationClientDto.RequestVerificationResponse response =
			ssafyAccountVerificationApiClient.requestVerification(
				new AccountVerificationClientDto.RequestVerificationRequest(
					account.getUser().getUserKey(),
					account.getAccountNo(),
					AUTH_TEXT
				)
			);

		AccountVerification savedVerification = accountVerificationRepository.save(
			AccountVerification.createRequested(
				account,
				AUTH_TEXT,
				response.transactionUniqueNo(),
				LocalDateTime.now()
			)
		);

		return new AccountVerificationDto.RequestVerificationResponse(
			savedVerification.getId(),
			response.accountNo(),
			AUTH_TEXT,
			response.transactionUniqueNo(),
			savedVerification.getRequestedAt().toString(),
			savedVerification.getStatus().name()
		);
	}
}
