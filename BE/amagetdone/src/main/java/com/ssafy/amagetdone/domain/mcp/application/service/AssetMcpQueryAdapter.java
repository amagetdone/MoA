package com.ssafy.amagetdone.domain.mcp.application.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.amagetdone.domain.account.api.dto.AccountDto;
import com.ssafy.amagetdone.domain.account.application.service.AccountService;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetMcpQueryAdapter {

	private final AccountService accountService;

	@Tool(name = "get_bank_accounts", description = "Returns external bank account candidates for the given user.")
	public List<AccountDto.BankAccountResponse> getBankAccounts(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return accountService.getBankAccounts(user_id);
	}

	@Tool(name = "get_account_detail", description = "Returns linked account detail with current balance for the given user.")
	public AccountDto.AccountSummaryResponse getAccountDetail(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "Linked account number") String accountNo
	) {
		return accountService.getAccount(user_id, accountNo);
	}

	@Tool(name = "get_linked_accounts", description = "Returns linked accounts for the given user.")
	public List<AccountDto.AccountSummaryResponse> getLinkedAccounts(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return accountService.getAccounts(user_id);
	}

	@Tool(name = "get_linked_cards", description = "Returns linked cards for the given user.")
	public List<AccountDto.CardSummaryResponse> getLinkedCards(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return accountService.getCards(user_id);
	}
}
