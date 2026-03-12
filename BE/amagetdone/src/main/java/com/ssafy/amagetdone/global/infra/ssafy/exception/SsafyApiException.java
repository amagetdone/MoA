package com.ssafy.amagetdone.global.infra.ssafy.exception;

public class SsafyApiException extends RuntimeException {

	private final Reason reason;

	public SsafyApiException(Reason reason) {
		super(reason.name());
		this.reason = reason;
	}

	public SsafyApiException(Reason reason, Throwable cause) {
		super(reason.name(), cause);
		this.reason = reason;
	}

	public Reason reason() {
		return reason;
	}

	public enum Reason {
		INVALID_CONFIGURATION,
		CALL_FAILED,
		INVALID_HEADER,
		FAILED_RESPONSE_CODE,
		INVALID_PAYLOAD
	}
}
