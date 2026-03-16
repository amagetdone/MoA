package com.ssafy.amagetdone.domain.member.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.api.dto.RefreshTokenDto;
import com.ssafy.amagetdone.domain.member.api.dto.LogoutDto;
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
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("소셜 로그인에 성공했습니다."))
			.andExpect(jsonPath("$.data.status").value("TEMP"))
			.andExpect(jsonPath("$.data.profile").doesNotExist())
			.andExpect(jsonPath("$.data.account").value(false))
			.andExpect(jsonPath("$.data.plan").value(false))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
	}

	@Test
	void socialTestAuthReturnsJwtPayload() throws Exception {
		given(memberAuthService.authenticateTest())
			.willReturn(new SocialAuthDto.Response(
				"TEMP",
				false,
				false,
				"access-token",
				"refresh-token"
			));

		mockMvc.perform(apiPost("/v1/auth/social/test"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("테스트 소셜 로그인에 성공했습니다."))
			.andExpect(jsonPath("$.data.status").value("TEMP"))
			.andExpect(jsonPath("$.data.account").value(false))
			.andExpect(jsonPath("$.data.plan").value(false))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
	}

	@Test
	void socialTestResetReturnsSuccess() throws Exception {
		willDoNothing().given(memberAuthService).resetTestUser();

		mockMvc.perform(apiPost("/v1/auth/social/test/reset"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("테스트 소셜 사용자를 초기화했습니다."))
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
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("토큰 재발급에 성공했습니다."))
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
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("로그아웃에 성공했습니다."))
			.andExpect(jsonPath("$.data.success").value(true));
	}

	private MockHttpServletRequestBuilder apiPost(String path) {
		return post("/api" + path).contextPath("/api");
	}
}
