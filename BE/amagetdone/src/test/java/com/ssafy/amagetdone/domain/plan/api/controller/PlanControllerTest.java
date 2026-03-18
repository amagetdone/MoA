package com.ssafy.amagetdone.domain.plan.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateResponse;
import com.ssafy.amagetdone.domain.plan.application.usecase.CreatePlanUseCase;
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

@WebMvcTest(PlanController.class)
@Import(SecurityConfig.class)
class PlanControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreatePlanUseCase createPlanUseCase;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@Test
	void createPlanReturnsCreatedPayload() throws Exception {
		given(createPlanUseCase.execute(eq(1), any()))
			.willReturn(new PlanCreateResponse(
				10,
				"Europe trip fund",
				"Summer travel savings",
				3000000L,
				LocalDate.of(2026, 4, 1),
				LocalDate.of(2026, 9, 1),
				1,
				"WAITING"
			));

		mockMvc.perform(post("/api/v1/plans")
				.contextPath("/api")
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "title": "Europe trip fund",
					  "description": "Summer travel savings",
					  "targetAmount": 3000000,
					  "startDate": "2026-04-01",
					  "endDate": "2026-09-01",
					  "priority": 1
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.planId").value(10))
			.andExpect(jsonPath("$.data.status").value("WAITING"));
	}

	@Test
	void createPlanRejectsInvalidDateRange() throws Exception {
		mockMvc.perform(post("/api/v1/plans")
				.contextPath("/api")
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "title": "Europe trip fund",
					  "description": "Summer travel savings",
					  "targetAmount": 3000000,
					  "startDate": "2026-09-01",
					  "endDate": "2026-04-01",
					  "priority": 1
					}
					"""))
			.andExpect(status().isBadRequest());
	}
}
