package com.ssafy.amagetdone.global.security;

import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;

import org.springframework.security.core.AuthenticationException;

public class MemberAuthenticationException extends AuthenticationException {

	private final MemberErrorCode errorCode;

	public MemberAuthenticationException(MemberErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	public MemberAuthenticationException(MemberErrorCode errorCode, Throwable cause) {
		super(errorCode.message(), cause);
		this.errorCode = errorCode;
	}

	public MemberErrorCode getErrorCode() {
		return errorCode;
	}
}
