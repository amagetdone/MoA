package com.ssafy.amagetdone.domain.chatbot.exception;

import com.ssafy.amagetdone.global.exception.ErrorCode;

import org.springframework.http.HttpStatus;

public enum ChatbotErrorCode implements ErrorCode {
	CHATBOT_UPSTREAM_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "CHATBOT_001", "Chatbot upstream is unavailable");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	ChatbotErrorCode(HttpStatus httpStatus, String code, String message) {
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
