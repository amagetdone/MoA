package com.ssafy.amagetdone.domain.member.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemberAuthService memberAuthService;

	@MockitoBean
	private MemberTokenService memberTokenService;

	@Test
	void socialAuthReturnsJwtPayloadForKakao() throws Exception {
		given(memberAuthService.authenticate(eq(SocialProvider.KAKAO), any()))
			.willReturn(new SocialAuthDto.Response(
				1,
				"kakao",
				true,
				false,
				"COMPLETE_PROFILE",
				"access-token",
				"refresh-token"
			));

		mockMvc.perform(post("/api/v1/auth/social/kakao")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "accessToken": "kakao-access-token"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(1))
			.andExpect(jsonPath("$.provider").value("kakao"))
			.andExpect(jsonPath("$.isNewUser").value(true))
			.andExpect(jsonPath("$.profileCompleted").value(false))
			.andExpect(jsonPath("$.nextAction").value("COMPLETE_PROFILE"))
			.andExpect(jsonPath("$.accessToken").value("access-token"))
			.andExpect(jsonPath("$.refreshToken").value("refresh-token"));
	}

	@Test
	void unsupportedProviderReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/auth/social/naver")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "accessToken": "naver-access-token"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value(MemberErrorCode.UNSUPPORTED_SOCIAL_PROVIDER.code()))
			.andExpect(jsonPath("$.message").value(MemberErrorCode.UNSUPPORTED_SOCIAL_PROVIDER.message()));
	}

	@Test
	void refreshReturnsRotatedTokens() throws Exception {
		given(memberTokenService.reissue("refresh-token"))
			.willReturn(new RefreshTokenDto.Response("new-access-token", "new-refresh-token"));

		mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "refreshToken": "refresh-token"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("new-access-token"))
			.andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
	}

	@Test
	void logoutReturnsSuccess() throws Exception {
		given(memberTokenService.logout("refresh-token"))
			.willReturn(new LogoutDto.Response(true));

		mockMvc.perform(post("/api/v1/auth/logout")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "refreshToken": "refresh-token"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));
	}
}
