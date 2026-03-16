package com.ssafy.amagetdone.domain.plan.api.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.amagetdone.config.SecurityConfig;
import com.ssafy.amagetdone.domain.plan.application.service.PlanImageService;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlanImageController.class)
@Import(SecurityConfig.class)
class PlanImageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PlanImageService planImageService;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@Test
	void uploadPlanImageReturnsObjectKey() throws Exception {
		MockMultipartFile file = new MockMultipartFile("image", "plan.png", "image/png", "abc".getBytes());
		given(planImageService.replacePlanImage(1, 2, file))
			.willReturn("plan/new.png");

		mockMvc.perform(multipart("/api/v1/plans/2/image")
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
			.andExpect(jsonPath("$.data").value("plan/new.png"));
	}
}
