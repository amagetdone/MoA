package com.ssafy.amagetdone.domain.plan.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateResponse;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanDetailResponse;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanListItemResponse;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanWeeklyPlansPageResponse;
import com.ssafy.amagetdone.domain.plan.application.usecase.CreatePlanUseCase;
import com.ssafy.amagetdone.domain.plan.application.usecase.DeletePlanUseCase;
import com.ssafy.amagetdone.domain.plan.application.usecase.GetPlanDetailUseCase;
import com.ssafy.amagetdone.domain.plan.application.usecase.GetPlanVideoDownloadUrlUseCase;
import com.ssafy.amagetdone.domain.plan.application.usecase.ListPlanWeeklyPlansUseCase;
import com.ssafy.amagetdone.domain.plan.application.usecase.ListPlansUseCase;
import com.ssafy.amagetdone.domain.plan.application.usecase.UpdatePlanPrioritiesUseCase;
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

@WebMvcTest({PlanController.class, PlanVideoController.class})
@Import(SecurityConfig.class)
class PlanControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreatePlanUseCase createPlanUseCase;

	@MockitoBean
	private DeletePlanUseCase deletePlanUseCase;

	@MockitoBean
	private ListPlansUseCase listPlansUseCase;

	@MockitoBean
	private GetPlanVideoDownloadUrlUseCase getPlanVideoDownloadUrlUseCase;

	@MockitoBean
	private GetPlanDetailUseCase getPlanDetailUseCase;

	@MockitoBean
	private ListPlanWeeklyPlansUseCase listPlanWeeklyPlansUseCase;

	@MockitoBean
	private UpdatePlanPrioritiesUseCase updatePlanPrioritiesUseCase;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@Test
	void listPlansReturnsActivePlanPayload() throws Exception {
		given(listPlansUseCase.execute(1))
			.willReturn(List.of(
				new PlanListItemResponse(
					1L,
					"아이패드 구매",
					320000L,
					1000000L,
					1,
					3,
					14L,
					new java.math.BigDecimal("32")
				),
				new PlanListItemResponse(
					2L,
					"여행 자금",
					500000L,
					1500000L,
					2,
					5,
					72L,
					new java.math.BigDecimal("33.5")
				)
			));

		mockMvc.perform(get("/api/v1/plans")
				.contextPath("/api")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("목표 목록 조회 성공"))
			.andExpect(jsonPath("$.data[0].planId").value(1))
			.andExpect(jsonPath("$.data[0].title").value("아이패드 구매"))
			.andExpect(jsonPath("$.data[0].achievementRate").value(32))
			.andExpect(jsonPath("$.data[1].achievementRate").value(33.5));
	}

	@Test
	void createPlanReturnsCreatedPayload() throws Exception {
		given(createPlanUseCase.execute(eq(1), any()))
			.willReturn(new PlanCreateResponse(
				10,
				"Europe trip fund",
				new PlanCreateResponse.WeeklyMission(300_000L, 24),
				new PlanCreateResponse.WeeklyBudget(
					1_500_000L,
					300_000L,
					new PlanCreateResponse.BudgetBucket(
						700_000L,
						List.of(
							new PlanCreateResponse.BudgetItem("Housing", 350_000L),
							new PlanCreateResponse.BudgetItem("Phone", 200_000L),
							new PlanCreateResponse.BudgetItem("Subscription", 150_000L)
						)
					),
					new PlanCreateResponse.BudgetBucket(
						300_000L,
						List.of(
							new PlanCreateResponse.BudgetItem("Food", 180_000L),
							new PlanCreateResponse.BudgetItem("Leisure", 70_000L),
							new PlanCreateResponse.BudgetItem("Living", 50_000L)
						)
					),
					200_000L
				),
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
			.andExpect(jsonPath("$.data.planTitle").value("Europe trip fund"))
			.andExpect(jsonPath("$.data.weeklyMission.weeklySavingsAmount").value(300000))
			.andExpect(jsonPath("$.data.weeklyMission.weeksToGoal").value(24))
			.andExpect(jsonPath("$.data.weeklyBudget.totalIncomeAmount").value(1500000))
			.andExpect(jsonPath("$.data.weeklyBudget.savingAmount").value(300000))
			.andExpect(jsonPath("$.data.weeklyBudget.fixedExpense.totalAmount").value(700000))
			.andExpect(jsonPath("$.data.weeklyBudget.fixedExpense.items[0].category").value("Housing"))
			.andExpect(jsonPath("$.data.weeklyBudget.variableExpense.totalAmount").value(300000))
			.andExpect(jsonPath("$.data.weeklyBudget.variableExpense.items[0].category").value("Food"))
			.andExpect(jsonPath("$.data.status").value("WAITING"))
			.andExpect(jsonPath("$.data.weeklyBudget.freeAmount").value(200000));
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

	@Test
	void getPlanVideoDownloadUrlReturnsVideoUrl() throws Exception {
		given(getPlanVideoDownloadUrlUseCase.execute(1, 10))
			.willReturn("https://cdn.example.com/plan-10.mp4");

		mockMvc.perform(get("/api/plans/10/video/download")
				.contextPath("/api")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data").value("https://cdn.example.com/plan-10.mp4"));
	}

	@Test
	void getPlanVideoDownloadUrlReturnsNotFoundWhenVideoMissing() throws Exception {
		given(getPlanVideoDownloadUrlUseCase.execute(1, 10))
			.willThrow(new PlanException(PlanErrorCode.PLAN_VIDEO_NOT_FOUND));

		mockMvc.perform(get("/api/plans/10/video/download")
				.contextPath("/api")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("PLAN_004"));
	}

	@Test
	void getPlanDetailReturnsDetailPayload() throws Exception {
		given(getPlanDetailUseCase.execute(1, 1))
			.willReturn(new PlanDetailResponse(
				1,
				"留λ턿 援щℓ",
				"留λ턿 ?꾨줈 14?몄튂",
				1,
				3_000_000L,
				1_200_000L,
				40,
				60L,
				new PlanDetailResponse.GraphData(
					List.of(10, 11, 12),
					List.of(50_000L, 70_000L, 60_000L),
					List.of(40_000L, 60_000L, 50_000L)
				),
				List.of(
					new PlanDetailResponse.WeeklyPlan(10, 50_000L),
					new PlanDetailResponse.WeeklyPlan(11, 70_000L),
					new PlanDetailResponse.WeeklyPlan(12, 60_000L)
				),
				11,
				3,
				new PlanDetailResponse.CompletionProjection(5, java.time.LocalDate.of(2026, 5, 3))
			));

		mockMvc.perform(get("/api/v1/plans/1")
				.contextPath("/api")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.planId").value(1))
			.andExpect(jsonPath("$.data.graphData.weeks[0]").value(10))
			.andExpect(jsonPath("$.data.weeklyPlans[0].week").value(10))
			.andExpect(jsonPath("$.data.currentWeek").value(11))
			.andExpect(jsonPath("$.data.totalWeeks").value(3))
			.andExpect(jsonPath("$.data.completionProjection.weeksToGoal").value(5))
			.andExpect(jsonPath("$.data.completionProjection.estimatedCompletionDate").value("2026-05-03"));
	}

	@Test
	void listPlanWeeklyPlansReturnsPagedPayload() throws Exception {
		given(listPlanWeeklyPlansUseCase.execute(1, 1, 0, 3))
			.willReturn(new PlanWeeklyPlansPageResponse(
				1,
				List.of(
					new PlanWeeklyPlansPageResponse.WeeklyPlan(10, 50_000L),
					new PlanWeeklyPlansPageResponse.WeeklyPlan(11, 70_000L),
					new PlanWeeklyPlansPageResponse.WeeklyPlan(12, 60_000L)
				),
				0,
				3,
				21,
				7,
				true
			));

		mockMvc.perform(get("/api/v1/plans/1/weekly-plans")
				.contextPath("/api")
				.param("page", "0")
				.param("size", "3")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.planId").value(1))
			.andExpect(jsonPath("$.data.weeklyPlans.length()").value(3))
			.andExpect(jsonPath("$.data.page").value(0))
			.andExpect(jsonPath("$.data.totalPages").value(7))
			.andExpect(jsonPath("$.data.hasNext").value(true));
	}

	@Test
	void updatePlanPrioritiesReturnsSuccessMessage() throws Exception {
		mockMvc.perform(patch("/api/v1/plans/priority")
				.contextPath("/api")
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "plans": [
					    { "planId": 101, "priority": 1 },
					    { "planId": 102, "priority": 2 },
					    { "planId": 103, "priority": 3 }
					  ]
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("목표 우선순위가 변경되었습니다."))
			.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	void deletePlanReturnsSuccessMessage() throws Exception {
		mockMvc.perform(delete("/api/v1/plans/10")
				.contextPath("/api")
				.with(csrf())
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.message").value("목표가 삭제되었습니다."))
			.andExpect(jsonPath("$.data").isEmpty());
	}
}
