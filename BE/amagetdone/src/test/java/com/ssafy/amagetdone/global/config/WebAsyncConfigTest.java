package com.ssafy.amagetdone.global.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AsyncStreamingTestController.class)
@Import({SecurityConfig.class, WebAsyncConfig.class})
class WebAsyncConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@Test
	void streamingResponseBodyUsesDedicatedAsyncExecutorWithSecurityContext() throws Exception {
		MvcResult mvcResult = mockMvc.perform(get("/api/v1/test/stream")
				.contextPath("/api")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(7), null, List.of())
				)))
			.andExpect(request().asyncStarted())
			.andReturn();

		mockMvc.perform(asyncDispatch(mvcResult))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.allOf(
				org.hamcrest.Matchers.containsString("user=7"),
				org.hamcrest.Matchers.containsString("thread=mvc-async-")
			)));
	}
}
