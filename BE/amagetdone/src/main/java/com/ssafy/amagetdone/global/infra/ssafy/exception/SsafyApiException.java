package com.ssafy.amagetdone.global.infra.ssafy.exception;

public class SsafyApiException extends RuntimeException {

	private final Reason reason;
	private final String responseCode;
	private final String responseMessage;

	public SsafyApiException(Reason reason) {
		super(reason.name());
		this.reason = reason;
		this.responseCode = null;
		this.responseMessage = null;
	}

	public SsafyApiException(Reason reason, Throwable cause) {
		super(reason.name(), cause);
		this.reason = reason;
		this.responseCode = null;
		this.responseMessage = null;
	}

	public SsafyApiException(Reason reason, String responseCode, String responseMessage) {
		super(reason.name());
		this.reason = reason;
		this.responseCode = responseCode;
		this.responseMessage = responseMessage;
	}

	public Reason reason() {
		return reason;
	}

	public String responseCode() {
		return responseCode;
	}

	public String responseMessage() {
		return responseMessage;
	}

	public enum Reason {
		INVALID_CONFIGURATION,
		CALL_FAILED,
		INVALID_HEADER,
		FAILED_RESPONSE_CODE,
		INVALID_PAYLOAD
	}
}
