package com.ssafy.amagetdone.domain.member.exception;

public class MemberException extends RuntimeException {

	private final MemberErrorCode errorCode;

	public MemberException(MemberErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	public MemberException(MemberErrorCode errorCode, Throwable cause) {
		super(errorCode.message(), cause);
		this.errorCode = errorCode;
	}

	public MemberErrorCode getErrorCode() {
		return errorCode;
	}
}
