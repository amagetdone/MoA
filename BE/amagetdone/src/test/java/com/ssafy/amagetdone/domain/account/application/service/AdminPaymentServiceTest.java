package com.ssafy.amagetdone.domain.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.application.usecase.CancelAdminCardPaymentUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ExecuteAdminAccountPaymentUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ExecuteAdminCardPaymentUseCase;
import com.ssafy.amagetdone.domain.account.application.usecase.ReconcileLinkedAccountsUseCase;

import org.junit.jupiter.api.Test;

class AdminPaymentServiceTest {

	@Test
	void executeAccountPaymentDelegatesToUseCaseAndReconcilesAccounts() {
		ExecuteAdminAccountPaymentUseCase executeAdminAccountPaymentUseCase = mock(ExecuteAdminAccountPaymentUseCase.class);
		ExecuteAdminCardPaymentUseCase executeAdminCardPaymentUseCase = mock(ExecuteAdminCardPaymentUseCase.class);
		CancelAdminCardPaymentUseCase cancelAdminCardPaymentUseCase = mock(CancelAdminCardPaymentUseCase.class);
		ReconcileLinkedAccountsUseCase reconcileLinkedAccountsUseCase = mock(ReconcileLinkedAccountsUseCase.class);
		AdminPaymentService service = new AdminPaymentService(
			executeAdminAccountPaymentUseCase,
			executeAdminCardPaymentUseCase,
			cancelAdminCardPaymentUseCase,
			reconcileLinkedAccountsUseCase
		);
		AccountAdminDto.AccountPaymentRequest request = new AccountAdminDto.AccountPaymentRequest(
			"0011234567890123",
			"0010000000000001",
			10000L,
			"withdrawal",
			"deposit"
		);
		AccountAdminDto.PaymentResponse response = new AccountAdminDto.PaymentResponse(
			"account",
			"0011234567890123",
			91L,
			"20260327",
			"101500",
			10000L,
			"COMPLETED"
		);
		when(executeAdminAccountPaymentUseCase.execute(7, request)).thenReturn(response);

		assertThat(service.executeAccountPayment(7, request)).isEqualTo(response);
		verify(executeAdminAccountPaymentUseCase).execute(7, request);
		verify(reconcileLinkedAccountsUseCase).execute(7);
	}

	@Test
	void executeCardPaymentDelegatesToUseCaseAndReconcilesAccounts() {
		ExecuteAdminAccountPaymentUseCase executeAdminAccountPaymentUseCase = mock(ExecuteAdminAccountPaymentUseCase.class);
		ExecuteAdminCardPaymentUseCase executeAdminCardPaymentUseCase = mock(ExecuteAdminCardPaymentUseCase.class);
		CancelAdminCardPaymentUseCase cancelAdminCardPaymentUseCase = mock(CancelAdminCardPaymentUseCase.class);
		ReconcileLinkedAccountsUseCase reconcileLinkedAccountsUseCase = mock(ReconcileLinkedAccountsUseCase.class);
		AdminPaymentService service = new AdminPaymentService(
			executeAdminAccountPaymentUseCase,
			executeAdminCardPaymentUseCase,
			cancelAdminCardPaymentUseCase,
			reconcileLinkedAccountsUseCase
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
		verify(reconcileLinkedAccountsUseCase).execute(7);
	}

	@Test
	void cancelCardPaymentDelegatesToUseCaseAndReconcilesAccounts() {
		ExecuteAdminAccountPaymentUseCase executeAdminAccountPaymentUseCase = mock(ExecuteAdminAccountPaymentUseCase.class);
		ExecuteAdminCardPaymentUseCase executeAdminCardPaymentUseCase = mock(ExecuteAdminCardPaymentUseCase.class);
		CancelAdminCardPaymentUseCase cancelAdminCardPaymentUseCase = mock(CancelAdminCardPaymentUseCase.class);
		ReconcileLinkedAccountsUseCase reconcileLinkedAccountsUseCase = mock(ReconcileLinkedAccountsUseCase.class);
		AdminPaymentService service = new AdminPaymentService(
			executeAdminAccountPaymentUseCase,
			executeAdminCardPaymentUseCase,
			cancelAdminCardPaymentUseCase,
			reconcileLinkedAccountsUseCase
		);
		AccountAdminDto.CardPaymentCancelRequest request = new AccountAdminDto.CardPaymentCancelRequest(
			"5241111122223333",
			88L
		);
		AccountAdminDto.PaymentResponse response = new AccountAdminDto.PaymentResponse(
			"card",
			"5241111122223333",
			89L,
			"20260327",
			"102100",
			28900L,
			"CANCEL"
		);
		when(cancelAdminCardPaymentUseCase.execute(7, request)).thenReturn(response);

		assertThat(service.cancelCardPayment(7, request)).isEqualTo(response);
		verify(cancelAdminCardPaymentUseCase).execute(7, request);
		verify(reconcileLinkedAccountsUseCase).execute(7);
	}
}
