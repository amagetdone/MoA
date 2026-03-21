package com.ssafy.amagetdone.domain.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public final class AccountAdminDto {

	private AccountAdminDto() {
	}

	public record ProductCreateRequest(
		String bankCode,
		String accountName,
		String accountDescription
	) {
	}

	public record ProductResponse(
		String accountTypeUniqueNo,
		String bankCode,
		String bankName,
		String accountTypeCode,
		String accountTypeName,
		String accountName,
		String accountDescription,
		String accountType
	) {
	}

	public record DataInitRequest(
		String apiKey,
		String userApiKey
	) {
	}

	public record AccountCreateRequest(
		Integer userId,
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

	@Schema(name = "AccountTransactionInitRequest")
	public record AccountTransactionInitRequest(
		@Schema(
			description = "Target account number from local Account.accountNo",
			example = "0016174648358792"
		)
		String accountNo
	) {
	}

	@Schema(name = "CardTransactionInitRequest")
	public record CardTransactionInitRequest(
		@Schema(
			description = "Target card number from local Card.cardNo",
			example = "5241111122223333"
		)
		String cardNo
	) {
	}

	@Schema(name = "TransactionInitResponse")
	public record TransactionInitResponse(
		@Schema(description = "Target type", example = "account")
		String targetType,
		@Schema(description = "Target number", example = "0016174648358792")
		String targetNo,
		@Schema(description = "Created transaction count", example = "109")
		int transactionCount
	) {
	}
}
