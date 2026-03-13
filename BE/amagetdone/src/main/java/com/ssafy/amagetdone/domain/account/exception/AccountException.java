package com.ssafy.amagetdone.domain.account.exception;

public class AccountException extends RuntimeException {

	private final AccountErrorCode errorCode;

	public AccountException(AccountErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	public AccountException(AccountErrorCode errorCode, Throwable cause) {
		super(errorCode.message(), cause);
		this.errorCode = errorCode;
	}

	public AccountErrorCode getErrorCode() {
		return errorCode;
	}
}
