package com.ssafy.amagetdone.domain.accountverification.api.dto;

public final class AccountVerificationDto {

	private AccountVerificationDto() {
	}

	public record CheckVerificationRequest(
		String accountNo,
		String authCode
	) {
	}

	public record CheckVerificationResponse(
		Long verificationId,
		String accountNo,
		String status,
		boolean verified,
		String verifiedAt
	) {
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
