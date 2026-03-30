package com.ssafy.amagetdone.domain.accountverification.exception;

public class AccountVerificationException extends RuntimeException {

	private final AccountVerificationErrorCode errorCode;

	public AccountVerificationException(AccountVerificationErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	public AccountVerificationException(AccountVerificationErrorCode errorCode, Throwable cause) {
		super(errorCode.message(), cause);
		this.errorCode = errorCode;
	}

	public AccountVerificationErrorCode getErrorCode() {
		return errorCode;
	}
}
