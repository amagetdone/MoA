package com.ssafy.amagetdone.domain.accountverification.api.dto;

public final class AccountVerificationAdminDto {

	private AccountVerificationAdminDto() {
	}

	public record CheckVerificationRequest(
		Integer userId,
		String accountNo,
		String authCode
	) {
	}

	public record CheckVerificationResponse(
		Long verificationId,
		String accountNo,
		String status,
		boolean verified,
		Long transactionUniqueNo,
		String verifiedAt
	) {
	}

	public record VerificationStatusResponse(
		Long verificationId,
		Integer userId,
		String accountNo,
		String status,
		boolean verified,
		String authText,
		Long requestTransactionUniqueNo,
		String requestedAt,
		String verifiedAt,
		String failureReason
	) {
	}
}
