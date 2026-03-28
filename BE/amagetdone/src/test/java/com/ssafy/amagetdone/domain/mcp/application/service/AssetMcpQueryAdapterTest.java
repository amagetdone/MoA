package com.ssafy.amagetdone.domain.mcp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.application.service.AccountService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetMcpQueryAdapterTest {

	@Mock
	private AccountService accountService;

	@InjectMocks
	private AssetMcpQueryAdapter adapter;

	@Test
	void getBankAccountsDelegatesToAccountService() {
		List<AccountDto.BankAccountResponse> response = List.of(
			new AccountDto.BankAccountResponse("001", "SSAFY", "001123", "main", "deposit", 100000L, "KRW")
		);
		when(accountService.getBankAccounts(7)).thenReturn(response);

		assertThat(adapter.getBankAccounts(7)).isSameAs(response);
		verify(accountService).getBankAccounts(7);
	}

	@Test
	void getAccountDetailDelegatesToAccountService() {
		AccountDto.AccountSummaryResponse response = new AccountDto.AccountSummaryResponse(
			"001123",
			"001",
			"SSAFY",
			"main",
			"salary",
			true,
			100000L
		);
		when(accountService.getAccount(7, "001123")).thenReturn(response);

		assertThat(adapter.getAccountDetail(7, "001123")).isSameAs(response);
		verify(accountService).getAccount(7, "001123");
	}

	@Test
	void getLinkedAccountsDelegatesToAccountService() {
		List<AccountDto.AccountSummaryResponse> response = List.of(
			new AccountDto.AccountSummaryResponse("001123", "001", "SSAFY", "main", null, true, 100000L)
		);
		when(accountService.getAccounts(7)).thenReturn(response);

		assertThat(adapter.getLinkedAccounts(7)).isSameAs(response);
		verify(accountService).getAccounts(7);
	}

	@Test
	void getLinkedCardsDelegatesToAccountService() {
		List<AccountDto.CardSummaryResponse> response = List.of(
			new AccountDto.CardSummaryResponse(11L, "1234-****", "more card", "SSAFY Card", "001123")
		);
		when(accountService.getCards(7)).thenReturn(response);

		assertThat(adapter.getLinkedCards(7)).isSameAs(response);
		verify(accountService).getCards(7);
	}
}
