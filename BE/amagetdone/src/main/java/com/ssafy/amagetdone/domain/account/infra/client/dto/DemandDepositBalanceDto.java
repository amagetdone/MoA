package com.ssafy.amagetdone.domain.account.infra.client.dto;

public final class DemandDepositBalanceDto {

	private DemandDepositBalanceDto() {
	}

	public record Request(
		String userKey,
		String accountNo
	) {
	}

	public record Response(
		String bankCode,
		String accountNo,
		Long accountBalance,
		String accountCreatedDate,
		String accountExpiryDate,
		String lastTransactionDate,
		String currency
	) {
	}
}
