package com.ssafy.amagetdone.domain.account.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.application.service.AccountAdminService;
import com.ssafy.amagetdone.domain.account.application.service.AdminPaymentService;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import org.junit.jupiter.api.Test;

class AdminControllerTest {

	@Test
	void registerProductReturnsWrappedResponse() {
		AccountAdminService accountAdminService = mock(AccountAdminService.class);
		AdminPaymentService adminPaymentService = mock(AdminPaymentService.class);
		AdminController controller = new AdminController(accountAdminService, adminPaymentService);
		AccountAdminDto.ProductCreateRequest request = new AccountAdminDto.ProductCreateRequest("001", "모아 상품", "설명");
		AccountAdminDto.ProductResponse response = new AccountAdminDto.ProductResponse(
			"001-1-abc",
			"001",
			"테스트은행",
			"1",
			"수시입출금",
			"모아 상품",
			"설명",
			"DOMESTIC"
		);
		when(accountAdminService.registerProduct(request)).thenReturn(response);

		ApiResponse<AccountAdminDto.ProductResponse> result = controller.registerProduct(request);

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isEqualTo(response);
		verify(accountAdminService).registerProduct(request);
	}

	@Test
	void initializeAccountTransactionsReturnsWrappedResponse() {
		AccountAdminService accountAdminService = mock(AccountAdminService.class);
		AdminPaymentService adminPaymentService = mock(AdminPaymentService.class);
		AdminController controller = new AdminController(accountAdminService, adminPaymentService);
		AccountAdminDto.AccountTransactionInitRequest request =
			new AccountAdminDto.AccountTransactionInitRequest("0016174648358792");
		AccountAdminDto.TransactionInitResponse response = new AccountAdminDto.TransactionInitResponse(
			"account",
			"0016174648358792",
			109
		);
		when(accountAdminService.initializeAccountTransactions(request)).thenReturn(response);

		ApiResponse<AccountAdminDto.TransactionInitResponse> result =
			controller.initializeAccountTransactions(request);

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isEqualTo(response);
		verify(accountAdminService).initializeAccountTransactions(request);
	}

	@Test
	void initializeCardTransactionsReturnsWrappedResponse() {
		AccountAdminService accountAdminService = mock(AccountAdminService.class);
		AdminPaymentService adminPaymentService = mock(AdminPaymentService.class);
		AdminController controller = new AdminController(accountAdminService, adminPaymentService);
		AccountAdminDto.CardTransactionInitRequest request =
			new AccountAdminDto.CardTransactionInitRequest("5241111122223333");
		AccountAdminDto.TransactionInitResponse response = new AccountAdminDto.TransactionInitResponse(
			"card",
			"5241111122223333",
			36
		);
		when(accountAdminService.initializeCardTransactions(request)).thenReturn(response);

		ApiResponse<AccountAdminDto.TransactionInitResponse> result =
			controller.initializeCardTransactions(request);

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isEqualTo(response);
		verify(accountAdminService).initializeCardTransactions(request);
	}

	@Test
	void executeAccountPaymentReturnsWrappedResponse() {
		AccountAdminService accountAdminService = mock(AccountAdminService.class);
		AdminPaymentService adminPaymentService = mock(AdminPaymentService.class);
		AdminController controller = new AdminController(accountAdminService, adminPaymentService);
		AccountAdminDto.AccountPaymentRequest request = new AccountAdminDto.AccountPaymentRequest(
			"0011234567890123",
			"0010000000000001",
			10000L,
			"테스트 출금",
			"테스트 입금"
		);
		AccountAdminDto.PaymentResponse response = new AccountAdminDto.PaymentResponse(
			"account",
			"0011234567890123",
			77L,
			"20260324",
			"092500",
			10000L,
			"COMPLETED"
		);
		when(adminPaymentService.executeAccountPayment(7, request)).thenReturn(response);

		ApiResponse<AccountAdminDto.PaymentResponse> result =
			controller.executeAccountPayment(new AuthenticatedMember(7), request);

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isEqualTo(response);
		verify(adminPaymentService).executeAccountPayment(7, request);
	}

	@Test
	void executeCardPaymentReturnsWrappedResponse() {
		AccountAdminService accountAdminService = mock(AccountAdminService.class);
		AdminPaymentService adminPaymentService = mock(AdminPaymentService.class);
		AdminController controller = new AdminController(accountAdminService, adminPaymentService);
		AccountAdminDto.CardPaymentRequest request = new AccountAdminDto.CardPaymentRequest(
			"5241111122223333",
			101001L,
			28900L
		);
		AccountAdminDto.PaymentResponse response = new AccountAdminDto.PaymentResponse(
			"card",
			"5241111122223333",
			88L,
			"20260324",
			"092700",
			28900L,
			"BILLED"
		);
		when(adminPaymentService.executeCardPayment(7, request)).thenReturn(response);

		ApiResponse<AccountAdminDto.PaymentResponse> result =
			controller.executeCardPayment(new AuthenticatedMember(7), request);

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isEqualTo(response);
		verify(adminPaymentService).executeCardPayment(7, request);
	}

	@Test
	void cancelCardPaymentReturnsWrappedResponse() {
		AccountAdminService accountAdminService = mock(AccountAdminService.class);
		AdminPaymentService adminPaymentService = mock(AdminPaymentService.class);
		AdminController controller = new AdminController(accountAdminService, adminPaymentService);
		AccountAdminDto.CardPaymentCancelRequest request = new AccountAdminDto.CardPaymentCancelRequest(
			"5241111122223333",
			88L
		);
		AccountAdminDto.PaymentResponse response = new AccountAdminDto.PaymentResponse(
			"card",
			"5241111122223333",
			89L,
			"20260324",
			"093000",
			28900L,
			"CANCEL"
		);
		when(adminPaymentService.cancelCardPayment(7, request)).thenReturn(response);

		ApiResponse<AccountAdminDto.PaymentResponse> result =
			controller.cancelCardPayment(new AuthenticatedMember(7), request);

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isEqualTo(response);
		verify(adminPaymentService).cancelCardPayment(7, request);
	}
}
