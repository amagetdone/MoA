package com.ssafy.amagetdone.domain.mypage.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.domain.mypage.api.dto.MypageDto;
import com.ssafy.amagetdone.domain.mypage.application.usecase.GetMypageUseCase;
import com.ssafy.amagetdone.global.config.SecurityConfig;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.BDDMockito.given;

@WebMvcTest(MypageController.class)
@Import(SecurityConfig.class)
class MypageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GetMypageUseCase getMypageUseCase;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@Test
	void getMypageReturnsProfileAndSummary() throws Exception {
		given(getMypageUseCase.execute(1))
			.willReturn(new MypageDto.Response(
				"테스트유저",
				"test@more.com",
				"profile/test.png",
				4L,
				1L,
				5690000L
			));

		mockMvc.perform(apiGet("/v1/mypage")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("내 정보 요약을 조회했습니다."))
			.andExpect(jsonPath("$.data.userName").value("테스트유저"))
			.andExpect(jsonPath("$.data.userEmail").value("test@more.com"))
			.andExpect(jsonPath("$.data.profileImageUrl").value("profile/test.png"))
			.andExpect(jsonPath("$.data.activePlanCount").value(4))
			.andExpect(jsonPath("$.data.completedPlanCount").value(1))
			.andExpect(jsonPath("$.data.totalSavedAmount").value(5690000));
	}

	private MockHttpServletRequestBuilder apiGet(String path) {
		return get("/api" + path).contextPath("/api");
	}
}
