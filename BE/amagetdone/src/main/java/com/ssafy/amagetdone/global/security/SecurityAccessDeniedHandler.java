package com.ssafy.amagetdone.global.security;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.amagetdone.global.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public class SecurityAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public SecurityAccessDeniedHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(
		HttpServletRequest request,
		HttpServletResponse response,
		AccessDeniedException accessDeniedException
	) throws IOException {
		if (response.isCommitted()) {
			return;
		}

		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), ErrorResponse.of("Access denied", "ACCESS_DENIED"));
	}
}
