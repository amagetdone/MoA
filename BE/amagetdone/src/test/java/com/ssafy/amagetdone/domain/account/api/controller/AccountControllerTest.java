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
	void syncAccountsReadsUserIdFromAuthorizationHeader() {
		AccountService accountService = mock(AccountService.class);
		JwtAccessTokenReader jwtAccessTokenReader = mock(JwtAccessTokenReader.class);
		AccountController controller = new AccountController(accountService, jwtAccessTokenReader);
		List<AccountDto.AccountSummaryResponse> responses = List.of(
			new AccountDto.AccountSummaryResponse("0011234567890123", "001", "한국은행", "데모 수시입출금", null, true)
		);

		when(jwtAccessTokenReader.readUserId("Bearer access-token")).thenReturn(7);
		when(accountService.syncAccounts(7)).thenReturn(responses);

		ApiResponse<List<AccountDto.AccountSummaryResponse>> result = controller.syncAccounts("Bearer access-token");

		assertThat(result.status()).isEqualTo("ok");
		assertThat(result.data()).containsExactlyElementsOf(responses);
		verify(jwtAccessTokenReader).readUserId("Bearer access-token");
		verify(accountService).syncAccounts(7);
	}
}
