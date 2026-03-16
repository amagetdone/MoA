package com.ssafy.amagetdone.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.amagetdone.domain.member.api.controller.AuthController;
import com.ssafy.amagetdone.domain.member.api.controller.MemberProfileController;
import com.ssafy.amagetdone.domain.member.api.dto.LogoutDto;
import com.ssafy.amagetdone.domain.member.api.dto.ProfileCompletionDto;
import com.ssafy.amagetdone.domain.member.api.dto.RefreshTokenDto;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.application.service.MemberAuthService;
import com.ssafy.amagetdone.domain.member.application.service.MemberProfileImageService;
import com.ssafy.amagetdone.domain.member.application.service.MemberProfileService;
import com.ssafy.amagetdone.domain.member.application.service.MemberTokenService;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.config.SecurityConfig;
import com.ssafy.amagetdone.global.controller.GlobalHealthCheckController;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest({
	AuthController.class,
	MemberProfileController.class,
	GlobalHealthCheckController.class
})
@Import(SecurityConfig.class)
class SecurityConfigIntegrationTest {

	private static final String INVALID_BEARER = "Bearer invalid-token";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemberAuthService memberAuthService;

	@MockitoBean
	private MemberTokenService memberTokenService;

	@MockitoBean
	private MemberProfileService memberProfileService;

	@MockitoBean
	private MemberProfileImageService memberProfileImageService;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@Test
	void socialTestIgnoresInvalidAuthorizationHeader() throws Exception {
		given(jwtAccessTokenReader.readUserId(INVALID_BEARER))
			.willThrow(new MemberException(MemberErrorCode.INVALID_ACCESS_TOKEN));
		given(memberAuthService.authenticateTest())
			.willReturn(new SocialAuthDto.Response("TEMP", false, false, "access-token", "refresh-token"));

		mockMvc.perform(apiPost("/v1/auth/social/test")
				.header(AUTHORIZATION, INVALID_BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").value("access-token"));

		then(jwtAccessTokenReader).should(never()).readUserId(INVALID_BEARER);
	}

	@Test
	void refreshIgnoresInvalidAuthorizationHeader() throws Exception {
		given(jwtAccessTokenReader.readUserId(INVALID_BEARER))
			.willThrow(new MemberException(MemberErrorCode.INVALID_ACCESS_TOKEN));
		given(memberTokenService.reissue("refresh-token"))
			.willReturn(new RefreshTokenDto.Response("ACTIVE", true, false, "new-access-token", "new-refresh-token"));

		mockMvc.perform(apiPost("/v1/auth/refresh")
				.header(AUTHORIZATION, INVALID_BEARER)
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "refreshToken": "refresh-token"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").value("new-access-token"));

		then(jwtAccessTokenReader).should(never()).readUserId(INVALID_BEARER);
	}

	@Test
	void logoutIgnoresInvalidAuthorizationHeader() throws Exception {
		given(jwtAccessTokenReader.readUserId(INVALID_BEARER))
			.willThrow(new MemberException(MemberErrorCode.INVALID_ACCESS_TOKEN));
		given(memberTokenService.logout("refresh-token"))
			.willReturn(new LogoutDto.Response(true));

		mockMvc.perform(apiPost("/v1/auth/logout")
				.header(AUTHORIZATION, INVALID_BEARER)
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "refreshToken": "refresh-token"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.success").value(true));

		then(jwtAccessTokenReader).should(never()).readUserId(INVALID_BEARER);
	}

	@Test
	void protectedEndpointWithoutAuthReturnsUnauthorized() throws Exception {
		mockMvc.perform(apiPatch("/v1/members/me/profile")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "nickname": "moa",
					  "email": "moa@example.com",
					  "birthDate": "1997-03-01",
					  "hasFixedIncome": true
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value(MemberErrorCode.AUTHORIZATION_HEADER_REQUIRED.code()));
	}

	@Test
	void protectedEndpointWithInvalidBearerReturnsUnauthorized() throws Exception {
		given(jwtAccessTokenReader.readUserId(INVALID_BEARER))
			.willThrow(new MemberException(MemberErrorCode.INVALID_ACCESS_TOKEN));

		mockMvc.perform(apiPatch("/v1/members/me/profile")
				.header(AUTHORIZATION, INVALID_BEARER)
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "nickname": "moa",
					  "email": "moa@example.com",
					  "birthDate": "1997-03-01",
					  "hasFixedIncome": true
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value(MemberErrorCode.INVALID_ACCESS_TOKEN.code()));

		then(jwtAccessTokenReader).should().readUserId(INVALID_BEARER);
		then(memberProfileService).should(never()).completeProfile(eq(0), any(ProfileCompletionDto.Request.class));
	}

	private MockHttpServletRequestBuilder apiPost(String path) {
		return post("/api" + path).contextPath("/api");
	}

	private MockHttpServletRequestBuilder apiPatch(String path) {
		return patch("/api" + path).contextPath("/api");
	}
}
