package com.ssafy.amagetdone.global.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

class SecurityAuthenticationEntryPointTest {

	@Test
	void committedResponseSkipsAuthenticationEntryPointHandling() throws Exception {
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		SecurityAuthenticationEntryPoint entryPoint = new SecurityAuthenticationEntryPoint(objectMapper);
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		when(response.isCommitted()).thenReturn(true);

		entryPoint.commence(request, response, new MemberAuthenticationException(
			com.ssafy.amagetdone.domain.member.exception.MemberErrorCode.INVALID_ACCESS_TOKEN
		));

		verifyNoInteractions(objectMapper);
	}
}
