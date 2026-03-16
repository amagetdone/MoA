package com.ssafy.amagetdone.global.response;

import java.util.Map;

import com.ssafy.amagetdone.global.exception.ErrorCode;

public record ErrorResponse(
	String status,
	String message,
	String errorCode,
	Map<String, Object> detail
) {

	public static ErrorResponse from(ErrorCode errorCode) {
		return new ErrorResponse("error", errorCode.message(), errorCode.code(), null);
	}

	public static ErrorResponse from(ErrorCode errorCode, Map<String, Object> detail) {
		return new ErrorResponse("error", errorCode.message(), errorCode.code(), detail);
	}

	public static ErrorResponse of(String message, String errorCode) {
		return new ErrorResponse("error", message, errorCode, null);
	}
}
