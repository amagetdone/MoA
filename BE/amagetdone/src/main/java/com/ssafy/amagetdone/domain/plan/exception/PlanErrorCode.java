package com.ssafy.amagetdone.domain.plan.exception;

import com.ssafy.amagetdone.global.exception.ErrorCode;

import org.springframework.http.HttpStatus;

public enum PlanErrorCode implements ErrorCode {
	PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_001", "Plan not found"),
	PLAN_OWNER_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_002", "Plan owner not found"),
	BUDGET_WEEK_NOT_FOUND(HttpStatus.BAD_REQUEST, "PLAN_003", "Budget week not found for plan period"),
	PLAN_VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_004", "Plan video not found"),
	INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "PLAN_005", "Invalid page request");

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
