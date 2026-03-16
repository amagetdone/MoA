package com.ssafy.amagetdone.domain.member.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.amagetdone.domain.member.api.dto.ProfileCompletionDto;
import com.ssafy.amagetdone.domain.member.application.service.MemberProfileImageService;
import com.ssafy.amagetdone.domain.member.application.service.MemberProfileService;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.config.SecurityConfig;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(MemberProfileController.class)
@Import(SecurityConfig.class)
class MemberProfileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemberProfileService memberProfileService;

	@MockitoBean
	private MemberProfileImageService memberProfileImageService;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@Test
	void completeProfileWithAuthenticatedPrincipalReturnsCompletedPayload() throws Exception {
		given(jwtAccessTokenReader.readUserId(null))
			.willThrow(new MemberException(MemberErrorCode.AUTHORIZATION_HEADER_REQUIRED));
		given(memberProfileService.completeProfile(eq(1), any(ProfileCompletionDto.Request.class)))
			.willReturn(new ProfileCompletionDto.Response("ACTIVE"));

		mockMvc.perform(apiPatch("/v1/members/me/profile")
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
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
			.andExpect(jsonPath("$.message").value("프로필 입력을 완료했습니다."))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"));
	}

	@Test
	void completeProfileReturnsCompletedPayloadForAccessToken() throws Exception {
		given(jwtAccessTokenReader.readUserId("Bearer access-token")).willReturn(1);
		given(memberProfileService.completeProfile(eq(1), any(ProfileCompletionDto.Request.class)))
			.willReturn(new ProfileCompletionDto.Response("ACTIVE"));

		mockMvc.perform(apiPatch("/v1/members/me/profile")
				.with(csrf())
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
			.andExpect(jsonPath("$.message").value("프로필 입력을 완료했습니다."))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.userId").doesNotExist())
			.andExpect(jsonPath("$.data.profileCompleted").doesNotExist())
			.andExpect(jsonPath("$.data.nextAction").doesNotExist());
	}

	@Test
	void completeProfileWithoutBearerTokenReturnsUnauthorized() throws Exception {
		given(jwtAccessTokenReader.readUserId(null))
			.willThrow(new MemberException(MemberErrorCode.AUTHORIZATION_HEADER_REQUIRED));

		mockMvc.perform(apiPatch("/v1/members/me/profile")
				.with(csrf())
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

	@Test
	void uploadProfileImageWithAuthenticatedPrincipalReturnsObjectKey() throws Exception {
		MockMultipartFile file = new MockMultipartFile("image", "profile.png", "image/png", "abc".getBytes());
		given(memberProfileImageService.replaceProfileImage(1, file))
			.willReturn("profile/new.png");

		mockMvc.perform(multipart("/api/v1/members/me/profile-image")
				.file(file)
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.with(request -> {
					request.setMethod("PATCH");
					request.setContextPath("/api");
					return request;
				})
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data").value("profile/new.png"));
	}

	private MockHttpServletRequestBuilder apiPatch(String path) {
		return patch("/api" + path).contextPath("/api");
	}
}
