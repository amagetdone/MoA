package com.ssafy.amagetdone.domain.account.exception;

import java.util.Map;

import com.ssafy.amagetdone.global.infra.ssafy.exception.SsafyApiException;

public class AccountException extends RuntimeException {

	private final AccountErrorCode errorCode;
	private final Map<String, Object> detail;

	public AccountException(AccountErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
		this.detail = null;
	}

	public AccountException(AccountErrorCode errorCode, Throwable cause) {
		super(errorCode.message(), cause);
		this.errorCode = errorCode;
		this.detail = extractDetail(cause);
	}

	public AccountErrorCode getErrorCode() {
		return errorCode;
	}

	public Map<String, Object> getDetail() {
		return detail;
	}

	private Map<String, Object> extractDetail(Throwable cause) {
		if (!(cause instanceof SsafyApiException ssafyApiException)) {
			return null;
		}
		if (ssafyApiException.responseCode() == null && ssafyApiException.responseMessage() == null) {
			return null;
		}
		return Map.of(
			"source", "ssafy",
			"reason", ssafyApiException.reason().name(),
			"responseCode", ssafyApiException.responseCode(),
			"responseMessage", ssafyApiException.responseMessage()
		);
	}
}
