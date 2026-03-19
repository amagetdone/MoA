package com.ssafy.amagetdone.domain.plan.exception;

public class PlanException extends RuntimeException {

	private final PlanErrorCode errorCode;

	public PlanException(PlanErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	public PlanErrorCode getErrorCode() {
		return errorCode;
	}
}
