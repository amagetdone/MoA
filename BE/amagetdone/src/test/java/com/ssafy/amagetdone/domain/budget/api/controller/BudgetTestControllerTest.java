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
import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetCurrentWeekGoalAllocationTestService;
import com.ssafy.amagetdone.domain.budget.application.service.GoalSavingAccrualForceTestService;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetWeekSeedService;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
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
	private GoalSavingAccrualForceTestService goalSavingAccrualForceTestService;

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

	@Test
	void forceSavingsAccrualReturnsUnifiedResponseWithDefaultWeekScope() throws Exception {
		given(goalSavingAccrualForceTestService.force(eq(1), any()))
			.willReturn(new GoalSavingAccrualForceDto.Response(
				GoalSavingAccrualForceDto.Scope.WEEK,
				GoalSavingAccrualForceDto.TargetType.ALL,
				null,
				30_000L,
				LocalDate.of(2026, 3, 19),
				LocalDate.of(2026, 3, 18),
				LocalDate.of(2026, 3, 17),
				LocalDate.of(2026, 3, 23),
				"2026-W12",
				List.of(
					new GoalSavingAccrualForceDto.PlanResult(11, "Travel Fund", 1, 70_000L, 70_000L, 30_000L, 130_000L, 40_000L),
					new GoalSavingAccrualForceDto.PlanResult(12, "Camera Fund", 2, 50_000L, 40_000L, 0L, 90_000L, 40_000L)
				)
			));

		mockMvc.perform(post("/api/test/budget/savings-accrual/force")
				.contextPath("/api")
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("Goal saving accrual force test succeeded."))
			.andExpect(jsonPath("$.data.appliedScope").value("WEEK"))
			.andExpect(jsonPath("$.data.targetType").value("ALL"))
			.andExpect(jsonPath("$.data.appliedAmount").value(30000))
			.andExpect(jsonPath("$.data.periodStartDate").value("2026-03-17"))
			.andExpect(jsonPath("$.data.periodEndDate").value("2026-03-23"))
			.andExpect(jsonPath("$.data.periodLabel").value("2026-W12"))
			.andExpect(jsonPath("$.data.plans[0].planId").value(11))
			.andExpect(jsonPath("$.data.plans[0].priority").value(1))
			.andExpect(jsonPath("$.data.plans[0].plannedAmount").value(70000))
			.andExpect(jsonPath("$.data.plans[0].finalAmount").value(70000))
			.andExpect(jsonPath("$.data.plans[0].accruedAmount").value(30000))
			.andExpect(jsonPath("$.data.plans[0].remainingScheduledAmount").value(40000))
			.andExpect(jsonPath("$.data.plans[1].accruedAmount").value(0));
	}

	@Test
	void forceSavingsAccrualReturnsSingleTargetResponseWhenPlanIdProvided() throws Exception {
		given(goalSavingAccrualForceTestService.force(eq(1), any()))
			.willReturn(new GoalSavingAccrualForceDto.Response(
				GoalSavingAccrualForceDto.Scope.DAY,
				GoalSavingAccrualForceDto.TargetType.SINGLE,
				11,
				10_000L,
				LocalDate.of(2026, 3, 19),
				LocalDate.of(2026, 3, 18),
				LocalDate.of(2026, 3, 18),
				LocalDate.of(2026, 3, 18),
				"2026-03-18",
				List.of(
					new GoalSavingAccrualForceDto.PlanResult(11, "Travel Fund", 1, 10_000L, 10_000L, 10_000L, 130_000L, 0L)
				)
			));

		mockMvc.perform(post("/api/test/budget/savings-accrual/force")
				.contextPath("/api")
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "planId": 11,
					  "scope": "DAY"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.targetType").value("SINGLE"))
			.andExpect(jsonPath("$.data.planId").value(11))
			.andExpect(jsonPath("$.data.appliedScope").value("DAY"));
	}

	@Test
	void forceSavingsAccrualReturnsNotFoundWhenPlanDoesNotBelongToUser() throws Exception {
		given(goalSavingAccrualForceTestService.force(eq(1), any()))
			.willThrow(new PlanException(PlanErrorCode.PLAN_NOT_FOUND));

		mockMvc.perform(post("/api/test/budget/savings-accrual/force")
				.contextPath("/api")
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "planId": 99,
					  "scope": "WEEK"
					}
					"""))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("PLAN_001"));
	}

	@Test
	void forceSavingsAccrualRejectsInvalidScope() throws Exception {
		mockMvc.perform(post("/api/test/budget/savings-accrual/force")
				.contextPath("/api")
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "scope": "YEAR"
					}
					"""))
			.andExpect(status().isBadRequest());
	}
}
