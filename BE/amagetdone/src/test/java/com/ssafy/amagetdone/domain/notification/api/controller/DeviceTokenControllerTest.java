package com.ssafy.amagetdone.domain.notification.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.domain.notification.application.usecase.UpsertDeviceTokenUseCase;
import com.ssafy.amagetdone.global.config.SecurityConfig;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeviceTokenController.class)
@Import(SecurityConfig.class)
class DeviceTokenControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UpsertDeviceTokenUseCase upsertDeviceTokenUseCase;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@Test
	void upsertDeviceTokenReturnsSuccessMessage() throws Exception {
		mockMvc.perform(post("/api/v1/device-tokens")
				.contextPath("/api")
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "installationId": "install-1",
					  "fcmToken": "token-1",
					  "platform": "ANDROID"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("FCM \ud1a0\ud070\uc774 \ub4f1\ub85d\ub418\uc5c8\uc2b5\ub2c8\ub2e4."))
			.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	void upsertDeviceTokenRejectsBlankFields() throws Exception {
		mockMvc.perform(post("/api/v1/device-tokens")
				.contextPath("/api")
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "installationId": "",
					  "fcmToken": "",
					  "platform": ""
					}
					"""))
			.andExpect(status().isBadRequest());
	}
}
