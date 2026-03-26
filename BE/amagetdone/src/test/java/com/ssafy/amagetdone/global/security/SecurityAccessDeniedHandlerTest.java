package com.ssafy.amagetdone.global.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class SecurityAccessDeniedHandlerTest {

	@Test
	void committedResponseSkipsAccessDeniedHandling() throws Exception {
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		SecurityAccessDeniedHandler handler = new SecurityAccessDeniedHandler(objectMapper);
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		when(response.isCommitted()).thenReturn(true);

		handler.handle(request, response, new AccessDeniedException("stream already failed"));

		verifyNoInteractions(objectMapper);
	}
}
