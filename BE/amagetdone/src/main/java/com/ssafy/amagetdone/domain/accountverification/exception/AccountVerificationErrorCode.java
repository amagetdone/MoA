package com.ssafy.amagetdone.domain.accountverification.exception;

import com.ssafy.amagetdone.global.exception.ErrorCode;

import org.springframework.http.HttpStatus;

public enum AccountVerificationErrorCode implements ErrorCode {
	VERIFICATION_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT_VERIFICATION_001", "Verification request not found"),
	VERIFICATION_API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "ACCOUNT_VERIFICATION_002", "Verification API call failed"),
	INVALID_VERIFICATION_API_RESPONSE(
		HttpStatus.BAD_GATEWAY,
		"ACCOUNT_VERIFICATION_003",
		"Invalid verification API response"
	),
	AUTH_HISTORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "ACCOUNT_VERIFICATION_004", "Auth history not found"),
	AUTH_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "ACCOUNT_VERIFICATION_005", "Auth code expired"),
	AUTH_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "ACCOUNT_VERIFICATION_006", "Auth code mismatch"),
	INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST, "ACCOUNT_VERIFICATION_007", "Invalid auth code"),
	INVALID_AUTH_TEXT(HttpStatus.BAD_REQUEST, "ACCOUNT_VERIFICATION_008", "Invalid auth text");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	AccountVerificationErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	@Override
	public String code() {
		return code;
	}

	@Override
	public String message() {
		return message;
	}

	@Override
	public HttpStatus httpStatus() {
		return httpStatus;
	}
}
