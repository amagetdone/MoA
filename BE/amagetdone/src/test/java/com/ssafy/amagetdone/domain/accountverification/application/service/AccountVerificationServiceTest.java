package com.ssafy.amagetdone.domain.accountverification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
import com.ssafy.amagetdone.domain.accountverification.application.usecase.CheckAccountVerificationUseCase;
import com.ssafy.amagetdone.domain.accountverification.application.usecase.RequestAccountVerificationUseCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountVerificationServiceTest {

	@Mock
	private RequestAccountVerificationUseCase requestAccountVerificationUseCase;

	@Mock
	private CheckAccountVerificationUseCase checkAccountVerificationUseCase;

	@InjectMocks
	private AccountVerificationService accountVerificationService;

	@Test
	void delegatesVerificationMethodsToUseCases() {
		AccountVerificationDto.RequestVerificationRequest request =
			new AccountVerificationDto.RequestVerificationRequest("0011234567890123");
		AccountVerificationDto.RequestVerificationResponse requestResponse =
			new AccountVerificationDto.RequestVerificationResponse(
				12L,
				"0011234567890123",
				"AMAGETDONE",
				88L,
				"2026-03-13T09:00:00Z",
				"REQUESTED"
			);
		AccountVerificationDto.CheckVerificationRequest checkRequest =
			new AccountVerificationDto.CheckVerificationRequest("0011234567890123", "1234");
		AccountVerificationDto.CheckVerificationResponse checkResponse =
			new AccountVerificationDto.CheckVerificationResponse(
				12L,
				"0011234567890123",
				"SUCCESS",
				true,
				88L,
				"2026-03-13T09:03:00Z"
			);

		when(requestAccountVerificationUseCase.execute(7, request)).thenReturn(requestResponse);
		when(checkAccountVerificationUseCase.execute(7, checkRequest)).thenReturn(checkResponse);

		assertThat(accountVerificationService.requestVerification(7, request)).isEqualTo(requestResponse);
		assertThat(accountVerificationService.checkVerification(7, checkRequest)).isEqualTo(checkResponse);
		verify(requestAccountVerificationUseCase).execute(7, request);
		verify(checkAccountVerificationUseCase).execute(7, checkRequest);
	}
}
