package com.ssafy.amagetdone.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.config.SecurityConfig;

import jakarta.servlet.DispatcherType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DispatcherTypeTestController.class)
@Import(SecurityConfig.class)
class SecurityDispatcherTypeIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@Test
	void asyncDispatcherBypassesAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/test/dispatcher")
				.contextPath("/api")
				.with(request -> {
					request.setDispatcherType(DispatcherType.ASYNC);
					return request;
				}))
			.andExpect(status().isOk())
			.andExpect(content().string("ok"));
	}

	@Test
	void errorDispatcherBypassesAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/test/dispatcher")
				.contextPath("/api")
				.with(request -> {
					request.setDispatcherType(DispatcherType.ERROR);
					return request;
				}))
			.andExpect(status().isOk())
			.andExpect(content().string("ok"));
	}
}
