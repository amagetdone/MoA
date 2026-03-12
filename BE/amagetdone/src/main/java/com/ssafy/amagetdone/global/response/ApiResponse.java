package com.ssafy.amagetdone.global.response;

public record ApiResponse<T>(
	String status,
	String message,
	T data
) {

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>("ok", message, data);
	}
}
