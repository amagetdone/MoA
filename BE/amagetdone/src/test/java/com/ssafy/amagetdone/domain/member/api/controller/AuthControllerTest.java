package com.ssafy.amagetdone.domain.member.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.amagetdone.domain.member.api.dto.LogoutDto;
import com.ssafy.amagetdone.domain.member.api.dto.RefreshTokenDto;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.api.dto.TestSocialAuthDto;
import com.ssafy.amagetdone.domain.member.application.service.MemberAuthService;
import com.ssafy.amagetdone.domain.member.application.service.MemberTokenService;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.config.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemberAuthService memberAuthService;

	@MockitoBean
	private MemberTokenService memberTokenService;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@Test
	void socialAuthReturnsJwtPayloadForKakao() throws Exception {
		given(memberAuthService.authenticate(eq(SocialProvider.KAKAO), any()))
			.willReturn(new SocialAuthDto.Response(
				"TEMP",
				false,
				false,
				"access-token",
				"refresh-token"
			));

		mockMvc.perform(apiPost("/v1/auth/social/kakao")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "accessToken": "kakao-access-token"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().encoding("UTF-8"))
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("\uC18C\uC15C \uB85C\uADF8\uC778\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4."))
			.andExpect(jsonPath("$.data.status").value("TEMP"))
			.andExpect(jsonPath("$.data.profile").doesNotExist())
			.andExpect(jsonPath("$.data.account").value(false))
			.andExpect(jsonPath("$.data.plan").value(false))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
	}

	@Test
	void socialTestAuthReturnsJwtPayload() throws Exception {
		given(memberAuthService.authenticateTest(any(TestSocialAuthDto.Request.class)))
			.willReturn(new SocialAuthDto.Response(
				"TEMP",
				false,
				false,
				"access-token",
				"refresh-token"
			));

		mockMvc.perform(apiPost("/v1/auth/social/test")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "id": 2
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().encoding("UTF-8"))
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("\uD14C\uC2A4\uD2B8 \uC18C\uC15C \uB85C\uADF8\uC778\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4."))
			.andExpect(jsonPath("$.data.status").value("TEMP"))
			.andExpect(jsonPath("$.data.account").value(false))
			.andExpect(jsonPath("$.data.plan").value(false))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));

		verify(memberAuthService).authenticateTest(new TestSocialAuthDto.Request(2));
	}

	@Test
	void socialTestAuthDefaultsIdToOneWhenBodyIsMissing() throws Exception {
		given(memberAuthService.authenticateTest(new TestSocialAuthDto.Request(1)))
			.willReturn(new SocialAuthDto.Response(
				"TEMP",
				false,
				false,
				"access-token",
				"refresh-token"
			));

		mockMvc.perform(apiPost("/v1/auth/social/test"))
			.andExpect(status().isOk())
			.andExpect(content().encoding("UTF-8"))
			.andExpect(jsonPath("$.status").value("ok"));

		verify(memberAuthService).authenticateTest(new TestSocialAuthDto.Request(1));
	}

	@Test
	void socialTestResetReturnsSuccess() throws Exception {
		willDoNothing().given(memberAuthService).resetTestUser();

		mockMvc.perform(apiPost("/v1/auth/social/test/reset"))
			.andExpect(status().isOk())
			.andExpect(content().encoding("UTF-8"))
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("\uD14C\uC2A4\uD2B8 \uC18C\uC15C \uC0AC\uC6A9\uC790\uB97C \uCD08\uAE30\uD654\uD588\uC2B5\uB2C8\uB2E4."))
			.andExpect(jsonPath("$.data.success").value(true));
	}

	@Test
	void unsupportedProviderReturnsBadRequest() throws Exception {
		mockMvc.perform(apiPost("/v1/auth/social/naver")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "accessToken": "naver-access-token"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(content().encoding("UTF-8"))
			.andExpect(jsonPath("$.status").value("error"))
			.andExpect(jsonPath("$.errorCode").value(MemberErrorCode.UNSUPPORTED_SOCIAL_PROVIDER.code()))
			.andExpect(jsonPath("$.message").value(MemberErrorCode.UNSUPPORTED_SOCIAL_PROVIDER.message()));
	}

	@Test
	void refreshReturnsRotatedTokens() throws Exception {
		given(memberTokenService.reissue("refresh-token"))
			.willReturn(new RefreshTokenDto.Response("ACTIVE", true, false, "new-access-token", "new-refresh-token"));

		mockMvc.perform(apiPost("/v1/auth/refresh")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "refreshToken": "refresh-token"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().encoding("UTF-8"))
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("\uD1A0\uD070 \uC7AC\uBC1C\uAE09\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4."))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.account").value(true))
			.andExpect(jsonPath("$.data.plan").value(false))
			.andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
			.andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
	}

	@Test
	void logoutReturnsSuccess() throws Exception {
		given(memberTokenService.logout("refresh-token"))
			.willReturn(new LogoutDto.Response(true));

		mockMvc.perform(apiPost("/v1/auth/logout")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "refreshToken": "refresh-token"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().encoding("UTF-8"))
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("\uB85C\uADF8\uC544\uC6C3\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4."))
			.andExpect(jsonPath("$.data.success").value(true));
	}

	private MockHttpServletRequestBuilder apiPost(String path) {
		return post("/api" + path).contextPath("/api");
	}
}
