package com.ssafy.amagetdone.domain.account.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.application.service.AccountService;
import com.ssafy.amagetdone.global.response.ApiResponse;

import org.junit.jupiter.api.Test;

class AdminDataInitControllerTest {

	@Test
	void initializeAccountTransactionsReturnsWrappedResponse() {
		AccountService accountService = mock(AccountService.class);
		AdminDataInitController controller = new AdminDataInitController(accountService);
		AccountAdminDto.AccountTransactionInitRequest request =
			new AccountAdminDto.AccountTransactionInitRequest("0016174648358792");
		AccountAdminDto.TransactionInitResponse response = new AccountAdminDto.TransactionInitResponse(
			"account",
			"0016174648358792",
			109
		);
		when(accountService.initializeAccountTransactions(request)).thenReturn(response);

		ApiResponse<AccountAdminDto.TransactionInitResponse> result =
			controller.initializeAccountTransactions(request);

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isEqualTo(response);
		verify(accountService).initializeAccountTransactions(request);
	}

	@Test
	void initializeCardTransactionsReturnsWrappedResponse() {
		AccountService accountService = mock(AccountService.class);
		AdminDataInitController controller = new AdminDataInitController(accountService);
		AccountAdminDto.CardTransactionInitRequest request =
			new AccountAdminDto.CardTransactionInitRequest("5241111122223333");
		AccountAdminDto.TransactionInitResponse response = new AccountAdminDto.TransactionInitResponse(
			"card",
			"5241111122223333",
			36
		);
		when(accountService.initializeCardTransactions(request)).thenReturn(response);

		ApiResponse<AccountAdminDto.TransactionInitResponse> result =
			controller.initializeCardTransactions(request);

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isEqualTo(response);
		verify(accountService).initializeCardTransactions(request);
	}
}
