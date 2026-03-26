package com.ssafy.amagetdone.domain.chatbot.exception;

public class ChatbotRelayException extends RuntimeException {

	private final ChatbotErrorCode errorCode;

	public ChatbotRelayException(ChatbotErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	public ChatbotRelayException(ChatbotErrorCode errorCode, Throwable cause) {
		super(errorCode.message(), cause);
		this.errorCode = errorCode;
	}

	public ChatbotErrorCode getErrorCode() {
		return errorCode;
	}
}
