package com.ssafy.amagetdone.global.security;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.global.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class SecurityAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public SecurityAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {
		MemberErrorCode errorCode = resolveErrorCode(authException);
		response.setStatus(errorCode.httpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), ErrorResponse.from(errorCode));
	}

	private MemberErrorCode resolveErrorCode(AuthenticationException authException) {
		if (authException instanceof MemberAuthenticationException memberAuthenticationException) {
			return memberAuthenticationException.getErrorCode();
		}
		return MemberErrorCode.AUTHORIZATION_HEADER_REQUIRED;
	}
}
