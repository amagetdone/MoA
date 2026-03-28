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

	@Tool(
		name = "get_bank_accounts",
		description = "Returns the external SSAFY bank account candidates available to the user, including balances. Use when: the assistant needs to understand what bank accounts exist before or around account-linking conversations. Best for: account-linking guidance and checking candidate liquidity sources outside the already linked set. Prefer get_linked_accounts when the user asks about currently linked usable accounts."
	)
	public List<AccountDto.BankAccountResponse> getBankAccounts(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return accountService.getBankAccounts(user_id);
	}

	@Tool(
		name = "get_account_detail",
		description = "Returns one linked account with its current balance and verification status. Use when: the assistant needs exact cash availability in a specific account before giving spending or transfer advice. Best for: single-account liquidity checks and explaining payment-source capacity. Prefer get_linked_accounts when you need all linked account balances together."
	)
	public AccountDto.AccountSummaryResponse getAccountDetail(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "Linked account number") String accountNo
	) {
		return accountService.getAccount(user_id, accountNo);
	}

	@Tool(
		name = "get_linked_accounts",
		description = "Returns all currently linked accounts with balances and verification state. Use when: the assistant needs an overview of the user's liquid assets before coaching on spending safety, buffer size, or payment planning. Best for: cash-buffer checks and deciding whether the user can absorb current spending. Prefer get_account_detail for one specific account."
	)
	public List<AccountDto.AccountSummaryResponse> getLinkedAccounts(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return accountService.getAccounts(user_id);
	}

	@Tool(
		name = "get_linked_cards",
		description = "Returns the user's linked cards and their withdrawal accounts. Use when: the assistant must explain which payment instruments exist and what accounts card spending pulls from. Best for: card-spending context, payment-source mapping, and explaining how card activity connects to cash accounts. Prefer transaction tools when the user asks about actual spend history."
	)
	public List<AccountDto.CardSummaryResponse> getLinkedCards(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return accountService.getCards(user_id);
	}
}
