package com.ssafy.amagetdone.domain.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.application.usecase.CancelAdminCardPaymentUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ExecuteAdminAccountPaymentUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ExecuteAdminCardPaymentUseCase;

import org.junit.jupiter.api.Test;

class AdminPaymentServiceTest {

	@Test
	void executeCardPaymentDelegatesToUseCase() {
		ExecuteAdminAccountPaymentUseCase executeAdminAccountPaymentUseCase = mock(ExecuteAdminAccountPaymentUseCase.class);
		ExecuteAdminCardPaymentUseCase executeAdminCardPaymentUseCase = mock(ExecuteAdminCardPaymentUseCase.class);
		CancelAdminCardPaymentUseCase cancelAdminCardPaymentUseCase = mock(CancelAdminCardPaymentUseCase.class);
		AdminPaymentService service = new AdminPaymentService(
			executeAdminAccountPaymentUseCase,
			executeAdminCardPaymentUseCase,
			cancelAdminCardPaymentUseCase
		);
		AccountAdminDto.CardPaymentRequest request = new AccountAdminDto.CardPaymentRequest("5241111122223333", 101001L, 28900L);
		AccountAdminDto.PaymentResponse response = new AccountAdminDto.PaymentResponse(
			"card",
			"5241111122223333",
			88L,
			"20260324",
			"092700",
			28900L,
			"BILLED"
		);
		when(executeAdminCardPaymentUseCase.execute(7, request)).thenReturn(response);

		assertThat(service.executeCardPayment(7, request)).isEqualTo(response);
		verify(executeAdminCardPaymentUseCase).execute(7, request);
	}
}
