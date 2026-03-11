package com.ssafy.amagetdone.domain.member.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.amagetdone.domain.member.api.dto.ProfileCompletionDto;
import com.ssafy.amagetdone.domain.member.application.service.MemberProfileService;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
@WebMvcTest(MemberProfileController.class)
class MemberProfileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemberProfileService memberProfileService;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@Test
	void completeProfileReturnsCompletedPayloadForAccessToken() throws Exception {
		given(jwtAccessTokenReader.readUserId("Bearer access-token")).willReturn(1);
		given(memberProfileService.completeProfile(eq(1), any(ProfileCompletionDto.Request.class)))
			.willReturn(new ProfileCompletionDto.Response(1, true, "ACTIVE", "GO_HOME"));

		mockMvc.perform(patch("/api/v1/members/me/profile")
				.header(AUTHORIZATION, "Bearer access-token")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "nickname": "moa",
					  "email": "moa@example.com",
					  "birthDate": "1997-03-01",
					  "hasFixedIncome": true
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("프로필 입력이 완료되었습니다."))
			.andExpect(jsonPath("$.data.userId").value(1))
			.andExpect(jsonPath("$.data.profileCompleted").value(true))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.nextAction").value("GO_HOME"));
	}

	@Test
	void completeProfileWithoutBearerTokenReturnsUnauthorized() throws Exception {
		given(jwtAccessTokenReader.readUserId(null))
			.willThrow(new MemberException(MemberErrorCode.AUTHORIZATION_HEADER_REQUIRED));

		mockMvc.perform(patch("/api/v1/members/me/profile")
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
			.andExpect(jsonPath("$.status").value("error"))
			.andExpect(jsonPath("$.errorCode").value(MemberErrorCode.AUTHORIZATION_HEADER_REQUIRED.code()))
			.andExpect(jsonPath("$.message").value(MemberErrorCode.AUTHORIZATION_HEADER_REQUIRED.message()));
	}
}
