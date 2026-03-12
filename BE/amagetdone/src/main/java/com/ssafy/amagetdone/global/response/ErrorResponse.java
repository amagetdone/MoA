package com.ssafy.amagetdone.global.response;

import com.ssafy.amagetdone.global.exception.ErrorCode;

public record ErrorResponse(
	String status,
	String message,
	String errorCode
) {

	public static ErrorResponse from(ErrorCode errorCode) {
		return new ErrorResponse("error", errorCode.message(), errorCode.code());
	}

	public static ErrorResponse of(String message, String errorCode) {
		return new ErrorResponse("error", message, errorCode);
	}
}
