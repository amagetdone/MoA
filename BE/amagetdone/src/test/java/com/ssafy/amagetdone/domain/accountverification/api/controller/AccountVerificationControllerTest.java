package com.ssafy.amagetdone.domain.accountverification.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationDto;
import com.ssafy.amagetdone.domain.accountverification.application.service.AccountVerificationService;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.response.ApiResponse;

import org.junit.jupiter.api.Test;

class AccountVerificationControllerTest {

	@Test
	void requestVerificationReadsUserIdFromAuthorizationHeader() {
		AccountVerificationService accountVerificationService = mock(AccountVerificationService.class);
		JwtAccessTokenReader jwtAccessTokenReader = mock(JwtAccessTokenReader.class);
		AccountVerificationController controller = new AccountVerificationController(
			accountVerificationService,
			jwtAccessTokenReader
		);
		AccountVerificationDto.RequestVerificationRequest request =
			new AccountVerificationDto.RequestVerificationRequest("0011234567890123");
		AccountVerificationDto.RequestVerificationResponse response =
			new AccountVerificationDto.RequestVerificationResponse(
				12L,
				"0011234567890123",
				"AMAGETDONE",
				88L,
				"2026-03-13T09:00:00Z",
				"REQUESTED"
			);

		when(jwtAccessTokenReader.readUserId("Bearer access-token")).thenReturn(7);
		when(accountVerificationService.requestVerification(7, request)).thenReturn(response);

		ApiResponse<AccountVerificationDto.RequestVerificationResponse> result =
			controller.requestVerification("Bearer access-token", request);

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isEqualTo(response);
		verify(jwtAccessTokenReader).readUserId("Bearer access-token");
		verify(accountVerificationService).requestVerification(7, request);
	}
}
