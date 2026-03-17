package com.ssafy.amagetdone.domain.account.infra.client.dto;

import java.util.List;

public final class DemandDepositAccountDto {

	private DemandDepositAccountDto() {
	}

	public record AccountCreateRequest(
		String userKey,
		String accountTypeUniqueNo
	) {
	}

	public record AccountCreateResponse(
		String bankCode,
		String accountNo,
		String currency,
		String currencyName
	) {
	}

	public record ApiAccountCreateResponse(
		String bankCode,
		String accountNo,
		ApiCurrency currency
	) {
	}

	public record ApiCurrency(
		String currency,
		String currencyName
	) {
	}

	public record AccountListRequest(
		String userKey
	) {
	}

	public record AccountDetailRequest(
		String userKey,
		String accountNo
	) {
	}

	public record AccountListResponse(
		List<AccountItem> accounts
	) {
	}

	public record AccountDetailResponse(
		String bankCode,
		String bankName,
		String userName,
		String accountNo,
		String accountName,
		String accountTypeCode,
		String accountTypeName,
		String accountCreatedDate,
		String accountExpiryDate,
		Long dailyTransferLimit,
		Long oneTimeTransferLimit,
		Long accountBalance,
		String currency
	) {
	}

	public record AccountItem(
		String bankCode,
		String bankName,
		String userName,
		String accountNo,
		String accountName,
		String accountTypeCode,
		String accountTypeName,
		String accountCreatedDate,
		String accountExpiryDate,
		Long dailyTransferLimit,
		Long oneTimeTransferLimit,
		Long accountBalance,
		String currency
	) {
	}
}
