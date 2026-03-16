package com.ssafy.amagetdone.domain.accountverification.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.amagetdone.domain.accountverification.api.dto.AccountVerificationAdminDto;
import com.ssafy.amagetdone.domain.accountverification.application.service.AccountVerificationService;
import com.ssafy.amagetdone.global.response.ApiResponse;

import org.junit.jupiter.api.Test;

class AccountVerificationAdminControllerTest {

	@Test
	void checkVerificationReturnsWrappedResponse() {
		AccountVerificationService accountVerificationService = mock(AccountVerificationService.class);
		AccountVerificationAdminController controller = new AccountVerificationAdminController(accountVerificationService);
		AccountVerificationAdminDto.CheckVerificationRequest request =
			new AccountVerificationAdminDto.CheckVerificationRequest(7, "0011234567890123", "1234");
		AccountVerificationAdminDto.CheckVerificationResponse response =
			new AccountVerificationAdminDto.CheckVerificationResponse(
				12L,
				"0011234567890123",
				"SUCCESS",
				true,
				88L,
				"2026-03-13T09:03:00Z"
			);

		when(accountVerificationService.checkVerification(request)).thenReturn(response);

		ApiResponse<AccountVerificationAdminDto.CheckVerificationResponse> result = controller.checkVerification(request);

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isEqualTo(response);
		verify(accountVerificationService).checkVerification(request);
	}
}
