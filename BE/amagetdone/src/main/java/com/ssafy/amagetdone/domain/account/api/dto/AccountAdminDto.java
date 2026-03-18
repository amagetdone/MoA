package com.ssafy.amagetdone.domain.account.api.dto;

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

	public record DataInitResponse(
		String accountNo,
		String cardNo,
		String cardUniqueNo,
		int accountTransactionCount,
		int cardTransactionCount
	) {
	}
}
