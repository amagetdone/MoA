package com.ssafy.amagetdone.domain.accountverification.api.dto;

public final class AccountVerificationDto {

	private AccountVerificationDto() {
	}

	public record RequestVerificationRequest(
		String accountNo
	) {
	}

	public record RequestVerificationResponse(
		Long verificationId,
		String accountNo,
		String authText,
		Long transactionUniqueNo,
		String requestedAt,
		String status
	) {
	}
}
