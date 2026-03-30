package com.ssafy.amagetdone.domain.transaction.exception;

public class TransactionException extends RuntimeException {

	private final TransactionErrorCode errorCode;

	public TransactionException(TransactionErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	public TransactionException(TransactionErrorCode errorCode, Throwable cause) {
		super(errorCode.message(), cause);
		this.errorCode = errorCode;
	}

	public TransactionErrorCode getErrorCode() {
		return errorCode;
	}
}
