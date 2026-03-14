package com.ssafy.amagetdone.domain.plan.exception;

import com.ssafy.amagetdone.global.exception.ErrorCode;

import org.springframework.http.HttpStatus;

public enum PlanErrorCode implements ErrorCode {
	PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_001", "Plan not found");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	PlanErrorCode(HttpStatus httpStatus, String code, String message) {
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
