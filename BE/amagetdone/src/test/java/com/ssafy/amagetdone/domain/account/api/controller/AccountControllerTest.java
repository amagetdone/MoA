// Responsibility: Verifies account controller authorization-to-service delegation.
package com.ssafy.amagetdone.domain.account.api.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.application.service.AccountService;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.response.ApiResponse;

import org.junit.jupiter.api.Test;

class AccountControllerTest {

	@Test
	void confirmReadsUserIdFromAuthorizationHeader() {
		AccountService accountService = mock(AccountService.class);
		JwtAccessTokenReader jwtAccessTokenReader = mock(JwtAccessTokenReader.class);
		AccountController controller = new AccountController(accountService, jwtAccessTokenReader);
		AccountDto.ConfirmAccountRequest request = new AccountDto.ConfirmAccountRequest("0011234567890123");
		AccountDto.ConfirmAccountResponse response = new AccountDto.ConfirmAccountResponse(true);

		when(jwtAccessTokenReader.readUserId("Bearer access-token")).thenReturn(7);
		when(accountService.confirmAccount(7, request)).thenReturn(response);

		ApiResponse<AccountDto.ConfirmAccountResponse> result = controller.confirmAccount("Bearer access-token", request);

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isEqualTo(response);
		verify(jwtAccessTokenReader).readUserId("Bearer access-token");
		verify(accountService).confirmAccount(7, request);
	}

	@Test
	void deleteAccountReadsUserIdFromAuthorizationHeader() {
		AccountService accountService = mock(AccountService.class);
		JwtAccessTokenReader jwtAccessTokenReader = mock(JwtAccessTokenReader.class);
		AccountController controller = new AccountController(accountService, jwtAccessTokenReader);

		when(jwtAccessTokenReader.readUserId("Bearer access-token")).thenReturn(7);

		ApiResponse<Void> result = controller.deleteAccount("Bearer access-token", "0011234567890123");

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).isNull();
		verify(jwtAccessTokenReader).readUserId("Bearer access-token");
		verify(accountService).deleteAccount(7, "0011234567890123");
	}

	@Test
	void getCardsReadsUserIdFromAuthorizationHeader() {
		AccountService accountService = mock(AccountService.class);
		JwtAccessTokenReader jwtAccessTokenReader = mock(JwtAccessTokenReader.class);
		AccountController controller = new AccountController(accountService, jwtAccessTokenReader);
		AccountDto.CardSummaryResponse response = new AccountDto.CardSummaryResponse(
			2050305894L,
			"1001967923277056",
			"SSAFY 스마일카드",
			"KB국민카드",
			"0013549893721848"
		);

		when(jwtAccessTokenReader.readUserId("Bearer access-token")).thenReturn(7);
		when(accountService.getCards(7)).thenReturn(List.of(response));

		ApiResponse<List<AccountDto.CardSummaryResponse>> result = controller.getCards("Bearer access-token");

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).containsExactly(response);
		verify(jwtAccessTokenReader).readUserId("Bearer access-token");
		verify(accountService).getCards(7);
	}
}
