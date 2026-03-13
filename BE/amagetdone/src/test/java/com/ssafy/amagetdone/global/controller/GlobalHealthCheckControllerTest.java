package com.ssafy.amagetdone.global.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GlobalHealthCheckController.class)
class GlobalHealthCheckControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiHealthEndpointReturnsHelloWorld() throws Exception {
		assertHealthResponse(apiGet("/health"));
	}

	private void assertHealthResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
		mockMvc.perform(requestBuilder)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("헬스 체크에 성공했습니다."))
			.andExpect(jsonPath("$.data").value("Hello World!"));
	}

	private MockHttpServletRequestBuilder apiGet(String path) {
		return get("/api" + path).contextPath("/api");
	}
}
