package com.ssafy.amagetdone.domain.accountverification.infra.client.dto;

public final class AccountVerificationClientDto {

	private AccountVerificationClientDto() {
	}

	public record RequestVerificationRequest(
		String userKey,
		String accountNo,
		String authText
	) {
	}

	public record RequestVerificationResponse(
		Long transactionUniqueNo,
		String accountNo
	) {
	}

	public record CheckVerificationRequest(
		String userKey,
		String accountNo,
		String authText,
		String authCode
	) {
	}

	public record CheckVerificationResponse(
		String status,
		Long transactionUniqueNo,
		String accountNo
	) {
	}
}
