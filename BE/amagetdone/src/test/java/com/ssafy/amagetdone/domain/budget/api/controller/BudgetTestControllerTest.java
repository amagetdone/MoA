package com.ssafy.amagetdone.domain.budget.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSeedResponse;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetCurrentWeekGoalAllocationTestService;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetWeekSeedService;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
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

@WebMvcTest(BudgetTestController.class)
@Import(SecurityConfig.class)
class BudgetTestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BudgetWeekSeedService budgetWeekSeedService;

	@MockitoBean
	private BudgetCurrentWeekGoalAllocationTestService budgetCurrentWeekGoalAllocationTestService;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@Test
	void seedBudgetWeeksReturnsSummary() throws Exception {
		given(budgetWeekSeedService.seed(eq(1), any()))
			.willReturn(new BudgetWeekSeedResponse(
				3,
				LocalDate.of(2026, 3, 16),
				LocalDate.of(2026, 4, 5)
			));

		mockMvc.perform(post("/api/test/budget/weeks/seed")
				.contextPath("/api")
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "startDate": "2026-03-16",
					  "endDate": "2026-03-31"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("Budget week seed test succeeded."))
			.andExpect(jsonPath("$.data.seededCount").value(3))
			.andExpect(jsonPath("$.data.firstWeekStartDate").value("2026-03-16"))
			.andExpect(jsonPath("$.data.lastWeekEndDate").value("2026-04-05"));
	}

	@Test
	void forceCurrentWeekSavingsAllocationReturnsCurrentWeekAllocations() throws Exception {
		given(budgetCurrentWeekGoalAllocationTestService.forceCurrentWeekAllocation(1))
			.willReturn(WeeklySavingsDetailDto.Response.of(
				"2026-W12",
				LocalDate.of(2026, 3, 16),
				LocalDate.of(2026, 3, 22),
				300000L,
				220000L,
				List.of(
					new WeeklySavingsDetailDto.SavingDetail(
						"Travel Fund",
						1,
						180000L,
						180000L,
						500000L,
						WeeklySavingsDetailDto.AdjustmentSummary.none()
					),
					new WeeklySavingsDetailDto.SavingDetail(
						"Camera Fund",
						2,
						120000L,
						40000L,
						100000L,
						new WeeklySavingsDetailDto.AdjustmentSummary(1, "ROLLOVER_APPLIED", "초과지출 차감")
					)
				)
			));

		mockMvc.perform(post("/api/test/budget/weeks/current/savings-allocation/force")
				.contextPath("/api")
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("Current week goal savings allocation test succeeded."))
			.andExpect(jsonPath("$.data.yearWeek").value("2026-W12"))
			.andExpect(jsonPath("$.data.finalSavingAmount").value(220000))
			.andExpect(jsonPath("$.data.savings[1].finalAmount").value(40000));
	}
}
