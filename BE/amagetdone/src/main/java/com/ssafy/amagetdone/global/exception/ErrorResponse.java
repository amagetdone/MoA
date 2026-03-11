package com.ssafy.amagetdone.global.exception;

public record ErrorResponse(
	int status,
	String code,
	String message
) {

	public static ErrorResponse from(ErrorCode errorCode) {
		return new ErrorResponse(
			errorCode.httpStatus().value(),
			errorCode.code(),
			errorCode.message()
		);
	}

	public static ErrorResponse of(int status, String code, String message) {
		return new ErrorResponse(status, code, message);
	}
}
