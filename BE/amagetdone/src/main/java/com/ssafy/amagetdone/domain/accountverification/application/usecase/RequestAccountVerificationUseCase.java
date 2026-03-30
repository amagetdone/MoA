package com.ssafy.amagetdone.domain.accountverification.application.usecase;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.account.application.usecase.FindAccountUserUseCase;
import com.ssafy.amagetdone.domain.accountverification.application.event.VerificationRequestedEvent;
import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
import com.ssafy.amagetdone.domain.accountverification.infra.client.SsafyAccountVerificationApiClient;
import com.ssafy.amagetdone.domain.accountverification.infra.client.dto.AccountVerificationClientDto;
import com.ssafy.amagetdone.domain.accountverification.model.entity.AccountVerification;
import com.ssafy.amagetdone.domain.accountverification.model.repository.AccountVerificationRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Requests 1-won verification via Ssafy API without requiring Account to exist in local DB.
@Component
public class RequestAccountVerificationUseCase {

	private static final String AUTH_TEXT = "AMAGETDONE";

	private final FindAccountUserUseCase findAccountUserUseCase;
	private final AccountVerificationRepository accountVerificationRepository;
	private final SsafyAccountVerificationApiClient ssafyAccountVerificationApiClient;
	private final ApplicationEventPublisher applicationEventPublisher;

	public RequestAccountVerificationUseCase(
		FindAccountUserUseCase findAccountUserUseCase,
		AccountVerificationRepository accountVerificationRepository,
		SsafyAccountVerificationApiClient ssafyAccountVerificationApiClient,
		ApplicationEventPublisher applicationEventPublisher
	) {
		this.findAccountUserUseCase = findAccountUserUseCase;
		this.accountVerificationRepository = accountVerificationRepository;
		this.ssafyAccountVerificationApiClient = ssafyAccountVerificationApiClient;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Transactional
	public AccountVerificationDto.RequestVerificationResponse execute(
		Integer userId,
		AccountVerificationDto.RequestVerificationRequest request
	) {
		User user = findAccountUserUseCase.execute(userId);
		AccountVerificationClientDto.RequestVerificationResponse response =
			ssafyAccountVerificationApiClient.requestVerification(
				new AccountVerificationClientDto.RequestVerificationRequest(
					user.getUserKey(),
					request.accountNo(),
					AUTH_TEXT
				)
			);

		AccountVerification savedVerification = accountVerificationRepository.save(
			AccountVerification.createRequested(
				user,
				request.accountNo(),
				AUTH_TEXT,
				response.transactionUniqueNo(),
				LocalDateTime.now()
			)
		);

		applicationEventPublisher.publishEvent(new VerificationRequestedEvent(
			user.getId(),
			user.getUserKey(),
			request.accountNo(),
			AUTH_TEXT,
			savedVerification.getRequestedAt()
		));

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
