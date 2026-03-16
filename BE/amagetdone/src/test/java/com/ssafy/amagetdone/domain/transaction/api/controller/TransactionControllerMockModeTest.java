package com.ssafy.amagetdone.domain.transaction.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.domain.transaction.application.service.MockTransactionAnalysisResponseFacade;
import com.ssafy.amagetdone.domain.transaction.application.usecase.ListTransactionsUseCase;
import com.ssafy.amagetdone.global.config.SecurityConfig;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, MockTransactionAnalysisResponseFacade.class})
@TestPropertySource(properties = "transaction.analysis-response-mode=mock")
class TransactionControllerMockModeTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@MockitoBean
	private ListTransactionsUseCase listTransactionsUseCase;

	@Test
	void getAnalysisJobStatusReturnsMockedOnboardingPayloadInMockMode() throws Exception {
		mockMvc.perform(apiGet("/transactions/analysis-jobs/mock-job-001")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.jobId").value("mock-job-001"))
			.andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
			.andExpect(jsonPath("$.data.onboardingResult").exists())
			.andExpect(jsonPath("$.data.onboardingResult.categoryExpenses[0].categoryName").value("주거"));
	}

	@Test
	void getMonthlyAnalysisReturnsMockedPayloadInMockMode() throws Exception {
		mockMvc.perform(apiGet("/transactions/analysis/months/2026-03")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.yearMonth").value("2026-03"))
			.andExpect(jsonPath("$.data.categoryExpenses[0].categoryName").value("식비"));
	}

	@Test
	void createAnalysisJobReturnsMockedPayloadInMockMode() throws Exception {
		mockMvc.perform(apiPost("/transactions/analysis-jobs")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, List.of())
				))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "months": 3,
					  "forceFullSync": false,
					  "relabel": false
					}
					"""))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.jobId").value("mock-job-001"))
			.andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
			.andExpect(jsonPath("$.data.requestedMonths").value(3));
	}

	private MockHttpServletRequestBuilder apiGet(String path) {
		return get("/api" + path).contextPath("/api");
	}

	private MockHttpServletRequestBuilder apiPost(String path) {
		return post("/api" + path).contextPath("/api");
	}
}
