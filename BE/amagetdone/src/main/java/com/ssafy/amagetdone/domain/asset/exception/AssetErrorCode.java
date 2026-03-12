package com.ssafy.amagetdone.domain.asset.exception;

import com.ssafy.amagetdone.global.exception.ErrorCode;

import org.springframework.http.HttpStatus;

public enum AssetErrorCode implements ErrorCode {
	ASSET_ERROR(HttpStatus.BAD_REQUEST, "ASSET_001", "Asset error"),
	MERCHANT_API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "ASSET_002", "Merchant API call failed"),
	INVALID_MERCHANT_API_RESPONSE(HttpStatus.BAD_GATEWAY, "ASSET_003", "Invalid merchant API response");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	AssetErrorCode(HttpStatus httpStatus, String code, String message) {
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
