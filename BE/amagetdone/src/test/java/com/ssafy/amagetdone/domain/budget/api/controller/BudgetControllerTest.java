package com.ssafy.amagetdone.domain.budget.api.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetAdjustmentDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetDashboardDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetHomeDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetSummaryDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSetupDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDetailDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetIncomeDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyFreeSpendingDetailDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.application.usecase.SaveBudgetAdjustmentUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetHomeUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetIncomeUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetDashboardUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetSummaryUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetWeekSetupUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyEssentialExpenseDetailUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyEssentialExpensesUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyFreeSpendingDetailUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklySavingsDetailUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.SaveBudgetIncomeUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
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

@WebMvcTest(BudgetController.class)
@Import(SecurityConfig.class)
class BudgetControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtAccessTokenReader jwtAccessTokenReader;

	@MockitoBean
	private GetBudgetSummaryUseCase getBudgetSummaryUseCase;

	@MockitoBean
	private GetBudgetDashboardUseCase getBudgetDashboardUseCase;

	@MockitoBean
	private GetBudgetHomeUseCase getBudgetHomeUseCase;

	@MockitoBean
	private GetWeeklyEssentialExpensesUseCase getWeeklyEssentialExpensesUseCase;

	@MockitoBean
	private GetWeeklyEssentialExpenseDetailUseCase getWeeklyEssentialExpenseDetailUseCase;

	@MockitoBean
	private GetWeeklySavingsDetailUseCase getWeeklySavingsDetailUseCase;

	@MockitoBean
	private GetWeeklyFreeSpendingDetailUseCase getWeeklyFreeSpendingDetailUseCase;

	@MockitoBean
	private GetBudgetWeekSetupUseCase getBudgetWeekSetupUseCase;

	@MockitoBean
	private SaveBudgetAdjustmentUseCase saveBudgetAdjustmentUseCase;

	@MockitoBean
	private SaveBudgetIncomeUseCase saveBudgetIncomeUseCase;

	@MockitoBean
	private GetBudgetIncomeUseCase getBudgetIncomeUseCase;

	@MockitoBean
	private TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase;

	@Test
	void saveIncomeStoresCurrentMonthIncome() throws Exception {
		given(saveBudgetIncomeUseCase.execute(1, new BudgetIncomeDto.Request(3200000L)))
			.willReturn(new BudgetIncomeDto.Response(3200000L, LocalDate.of(2026, 3, 1)));

		mockMvc.perform(apiPost("/budgets/income")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "monthlyIncome": 3200000
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.monthlyIncome").value(3200000))
			.andExpect(jsonPath("$.data.baseMonth").value("2026-03-01"));
	}

	@Test
	void getIncomeReturnsZeroWhenCurrentMonthIncomeMissing() throws Exception {
		given(getBudgetIncomeUseCase.execute(1)).willReturn(new BudgetIncomeDto.Response(0L, LocalDate.of(2026, 3, 1)));

		mockMvc.perform(apiGet("/budgets/income")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.monthlyIncome").value(0))
			.andExpect(jsonPath("$.data.baseMonth").value("2026-03-01"));
	}

	@Test
	void allocateBudgetTriggersExistingAllocationUseCase() throws Exception {
		mockMvc.perform(apiPost("/budgets/allocate")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.success").value(true));

		verify(triggerBudgetAllocationUseCase).execute(1);
	}

	@Test
	void getBudgetDashboardReturnsFreeSpendBalance() throws Exception {
		given(getBudgetDashboardUseCase.execute(1))
			.willReturn(new BudgetDashboardDto.Response(
				LocalDate.of(2026, 3, 9),
				LocalDate.of(2026, 3, 15),
				3L,
				120000L,
				45000L,
				75000L,
				25000L
			));

		mockMvc.perform(apiGet("/budgets/home/dashboard")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.weekStartDate").value("2026-03-09"))
			.andExpect(jsonPath("$.data.weekEndDate").value("2026-03-15"))
			.andExpect(jsonPath("$.data.daysRemaining").value(3))
			.andExpect(jsonPath("$.data.finalFreeAmount").value(120000))
			.andExpect(jsonPath("$.data.actualSpentAmount").value(45000))
			.andExpect(jsonPath("$.data.remainingAmount").value(75000))
			.andExpect(jsonPath("$.data.dailyAvailable").value(25000));
	}

	@Test
	void getBudgetHomeReturnsCompositeResponse() throws Exception {
		given(getBudgetHomeUseCase.execute(1))
			.willReturn(BudgetHomeDto.Response.of(
				BudgetHomeDto.FreeSpending.of(45000L, 120000L, 75000L, 3L, 25000L),
				BudgetHomeDto.WeeklySaving.of(180000L, 820000L, 18L),
				BudgetHomeDto.AssetSummary.of(5400000L),
				List.of(BudgetHomeDto.PlanCard.of(11L, "Travel Fund", 1, 45L, 680000L, 1000000L, 68L, "https://example.com/travel.png"))
			));

		mockMvc.perform(apiGet("/budgets/home")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.freeSpending.usedAmount").value(45000))
			.andExpect(jsonPath("$.data.weeklySaving.thisWeekSavingAmount").value(180000))
			.andExpect(jsonPath("$.data.assetSummary.totalAssetAmount").value(5400000))
			.andExpect(jsonPath("$.data.plans[0].title").value("Travel Fund"))
			.andExpect(jsonPath("$.data.plans[0].priority").value(1));
	}

	@Test
	void getBudgetSummaryReturnsSummaryMetrics() throws Exception {
		given(getBudgetSummaryUseCase.execute(1))
			.willReturn(new BudgetSummaryDto.Response(2L, 1L, 450000L));

		mockMvc.perform(apiGet("/budgets/me/summary")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.activePlanCount").value(2))
			.andExpect(jsonPath("$.data.completedPlanCount").value(1))
			.andExpect(jsonPath("$.data.totalSavedAmount").value(450000));
	}

	@Test
	void getBudgetWeekSetupReturnsWeeklySetupView() throws Exception {
		given(getBudgetWeekSetupUseCase.execute(1, "2026-W10"))
			.willReturn(BudgetWeekSetupDto.Response.of(
				1200000L,
				300000L,
				400000L,
				450000L,
				120000L,
				List.of(
					new BudgetWeekSetupDto.Essential(2, "Subscription", "FIXED", 70000L, 65000L),
					new BudgetWeekSetupDto.Essential(10, "Food", "VARIABLE", 230000L, 110000L)
				),
				List.of(
					new BudgetWeekSetupDto.Saving("Travel Fund", 1, 250000L, 230000L),
					new BudgetWeekSetupDto.Saving("Emergency Fund", 2, 150000L, 120000L)
				)
			));

		mockMvc.perform(apiGet("/budgets/weeks/2026-W10/setup")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.plannedIncomeAmount").value(1200000))
			.andExpect(jsonPath("$.data.plannedEssentialAmount").value(300000))
			.andExpect(jsonPath("$.data.plannedSavingAmount").value(400000))
			.andExpect(jsonPath("$.data.plannedFreeAmount").value(500000))
			.andExpect(jsonPath("$.data.finalFreeAmount").value(450000))
			.andExpect(jsonPath("$.data.actualSpentAmount").value(120000))
			.andExpect(jsonPath("$.data.remainingFreeAmount").value(330000))
			.andExpect(jsonPath("$.data.essentials[0].expenseScope").value("FIXED"))
			.andExpect(jsonPath("$.data.essentials[1].categoryName").value("Food"))
			.andExpect(jsonPath("$.data.savings[0].planTitle").value("Travel Fund"))
			.andExpect(jsonPath("$.data.savings[1].finalAmount").value(120000));
	}

	@Test
	void saveBudgetAdjustmentReturnsAdjustedWeekValues() throws Exception {
		BudgetAdjustmentDto.Request request = new BudgetAdjustmentDto.Request(
			BudgetAdjustmentType.FREE_BUDGET_ADD,
			20000L,
			null,
			"비상 자유비 보정"
		);
		given(saveBudgetAdjustmentUseCase.execute(1, "2026-W10", request))
			.willReturn(new BudgetAdjustmentDto.Response(
				"2026-W10",
				"FREE_BUDGET_ADD",
				20000L,
				null,
				"비상 자유비 보정",
				20000L,
				null,
				170000L,
				"ADJUSTED"
			));

		mockMvc.perform(apiPost("/budgets/weeks/2026-W10/adjustments")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "type": "FREE_BUDGET_ADD",
					  "amount": 20000,
					  "planId": null,
					  "reason": "비상 자유비 보정"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.adjustmentType").value("FREE_BUDGET_ADD"))
			.andExpect(jsonPath("$.data.adjustmentAmount").value(20000))
			.andExpect(jsonPath("$.data.manualAdjustmentAmount").value(20000))
			.andExpect(jsonPath("$.data.finalFreeAmount").value(170000))
			.andExpect(jsonPath("$.data.budgetWeekStatus").value("ADJUSTED"));
	}

	@Test
	void saveBudgetAdjustmentReturnsBadRequestForClosedWeek() throws Exception {
		BudgetAdjustmentDto.Request request = new BudgetAdjustmentDto.Request(
			BudgetAdjustmentType.FREE_BUDGET_SUBTRACT,
			10000L,
			null,
			"마감 주차 차감"
		);
		given(saveBudgetAdjustmentUseCase.execute(1, "2026-W10", request))
			.willThrow(new IllegalArgumentException("Closed budget weeks cannot be adjusted"));

		mockMvc.perform(apiPost("/budgets/weeks/2026-W10/adjustments")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "type": "FREE_BUDGET_SUBTRACT",
					  "amount": 10000,
					  "planId": null,
					  "reason": "마감 주차 차감"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value("error"))
			.andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
	}

	@Test
	void getWeeklySavingsDetailReturnsPlanAllocationsAndAdjustmentSummary() throws Exception {
		given(getWeeklySavingsDetailUseCase.execute(1, "2026-W10"))
			.willReturn(WeeklySavingsDetailDto.Response.of(
				"2026-W10",
				LocalDate.of(2026, 3, 2),
				LocalDate.of(2026, 3, 8),
				400000L,
				350000L,
				List.of(
					new WeeklySavingsDetailDto.SavingDetail(
						"Travel Fund",
						1,
						250000L,
						230000L,
						900000L,
						new WeeklySavingsDetailDto.AdjustmentSummary(2, "PLAN_SUBTRACT", "일정 변경")
					),
					new WeeklySavingsDetailDto.SavingDetail(
						"Emergency Fund",
						2,
						150000L,
						120000L,
						400000L,
						WeeklySavingsDetailDto.AdjustmentSummary.none()
					)
				)
			));

		mockMvc.perform(apiGet("/budgets/weeks/2026-W10/savings")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.plannedSavingAmount").value(400000))
			.andExpect(jsonPath("$.data.finalSavingAmount").value(350000))
			.andExpect(jsonPath("$.data.savings[0].planTitle").value("Travel Fund"))
			.andExpect(jsonPath("$.data.savings[0].priority").value(1))
			.andExpect(jsonPath("$.data.savings[0].savedAmount").value(900000))
			.andExpect(jsonPath("$.data.savings[0].adjustment.count").value(2))
			.andExpect(jsonPath("$.data.savings[0].adjustment.latestAdjustmentType").value("PLAN_SUBTRACT"))
			.andExpect(jsonPath("$.data.savings[1].adjustment.count").value(0));
	}

	@Test
	void getWeeklyFreeSpendingDetailReturnsAllocatedSpentAndTransactions() throws Exception {
		given(getWeeklyFreeSpendingDetailUseCase.execute(1, "2026-W10"))
			.willReturn(new WeeklyFreeSpendingDetailDto.Response(
				"2026-W10",
				LocalDate.of(2026, 3, 2),
				LocalDate.of(2026, 3, 8),
				180000L,
				55000L,
				125000L,
				List.of(
					new WeeklyFreeSpendingDetailDto.TransactionDetail(
						401,
						"CARD",
						"998877",
						LocalDateTime.of(2026, 3, 6, 20, 15),
						"Movie Theater",
						22000L,
						new WeeklyFreeSpendingDetailDto.CategoryView(15, "Leisure"),
						"VARIABLE",
						false,
						"NON_ESSENTIAL"
					),
					new WeeklyFreeSpendingDetailDto.TransactionDetail(
						402,
						"ACCOUNT",
						"ACCOUNT-1",
						LocalDateTime.of(2026, 3, 4, 12, 5),
						"Cafe",
						33000L,
						new WeeklyFreeSpendingDetailDto.CategoryView(16, "Dining Out"),
						"VARIABLE",
						false,
						"NON_ESSENTIAL"
					)
				)
			));

		mockMvc.perform(apiGet("/budgets/weeks/2026-W10/free-spending")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.allocatedAmount").value(180000))
			.andExpect(jsonPath("$.data.spentAmount").value(55000))
			.andExpect(jsonPath("$.data.remainingAmount").value(125000))
			.andExpect(jsonPath("$.data.transactions[0].occurredAt").value("2026-03-06T20:15:00"))
			.andExpect(jsonPath("$.data.transactions[0].merchantText").value("Movie Theater"))
			.andExpect(jsonPath("$.data.transactions[0].sourceType").value("CARD"))
			.andExpect(jsonPath("$.data.transactions[0].sourceId").value("998877"))
			.andExpect(jsonPath("$.data.transactions[0].category.name").value("Leisure"))
			.andExpect(jsonPath("$.data.transactions[0].analysisIncluded").value(false))
			.andExpect(jsonPath("$.data.transactions[0].excludedReason").value("NON_ESSENTIAL"))
			.andExpect(jsonPath("$.data.transactions[1].occurredAt").value("2026-03-04T12:05:00"));
	}

	@Test
	void getWeeklyEssentialExpensesReturnsOnlyEssentialExpenseSections() throws Exception {
		given(getWeeklyEssentialExpensesUseCase.execute(1, "2026-W10"))
			.willReturn(new WeeklyEssentialExpenseDto.Response(
				"2026-W10",
				LocalDate.of(2026, 3, 2),
				LocalDate.of(2026, 3, 8),
				3,
				155000L,
				72000L,
				83000L,
				List.of(
					new WeeklyEssentialExpenseDto.CategoryExpense(2, "Subscription", 17000L),
					new WeeklyEssentialExpenseDto.CategoryExpense(12, "Housing", 55000L)
				),
				List.of(new WeeklyEssentialExpenseDto.CategoryExpense(10, "Food", 83000L)),
				LocalDateTime.of(2026, 3, 13, 15, 20)
			));

		mockMvc.perform(apiGet("/budgets/weeks/2026-W10/essential-expenses")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.essentialAmount").value(155000))
			.andExpect(jsonPath("$.data.fixedExpenseCategories[0].categoryName").value("Subscription"))
			.andExpect(jsonPath("$.data.variableExpenseCategories[0].categoryName").value("Food"))
			.andExpect(jsonPath("$.data.savingBudget").doesNotExist())
			.andExpect(jsonPath("$.data.freeSpendable").doesNotExist());
	}

	@Test
	void getWeeklyEssentialExpenseDetailReturnsCategoryAmountsAndTransactions() throws Exception {
		given(getWeeklyEssentialExpenseDetailUseCase.execute(1, "2026-W10"))
			.willReturn(new WeeklyEssentialExpenseDetailDto.Response(
				"2026-W10",
				LocalDate.of(2026, 3, 2),
				LocalDate.of(2026, 3, 8),
				155000L,
				72000L,
				83000L,
				List.of(
					new WeeklyEssentialExpenseDetailDto.CategoryDetail(
						2,
						"Subscription",
						17000L,
						15000L,
						2000L,
						List.of(
							new WeeklyEssentialExpenseDetailDto.TransactionDetail(
								301,
								"CARD",
								"998877",
								LocalDateTime.of(2026, 3, 3, 9, 30),
								"Netflix",
								15000L,
								new WeeklyEssentialExpenseDetailDto.CategoryView(2, "Subscription"),
								"FIXED"
							)
						)
					)
				),
				List.of(
					new WeeklyEssentialExpenseDetailDto.CategoryDetail(
						10,
						"Food",
						138000L,
						57000L,
						81000L,
						List.of(
							new WeeklyEssentialExpenseDetailDto.TransactionDetail(
								302,
								"ACCOUNT",
								"ACCOUNT-1",
								LocalDateTime.of(2026, 3, 5, 18, 10),
								"Market",
								57000L,
								new WeeklyEssentialExpenseDetailDto.CategoryView(10, "Food"),
								"VARIABLE"
							)
						)
					)
				)
			));

		mockMvc.perform(apiGet("/budgets/weeks/2026-W10/essential-expenses/detail")
				.with(authentication(
					UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedMember(1), null, java.util.List.of())
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"))
			.andExpect(jsonPath("$.data.plannedEssentialAmount").value(155000))
			.andExpect(jsonPath("$.data.actualSpentAmount").value(72000))
			.andExpect(jsonPath("$.data.remainingAmount").value(83000))
			.andExpect(jsonPath("$.data.fixedExpenseCategories[0].plannedAmount").value(17000))
			.andExpect(jsonPath("$.data.fixedExpenseCategories[0].transactions[0].merchantText").value("Netflix"))
			.andExpect(jsonPath("$.data.variableExpenseCategories[0].actualSpentAmount").value(57000))
			.andExpect(jsonPath("$.data.variableExpenseCategories[0].transactions[0].sourceType").value("ACCOUNT"));
	}

	private MockHttpServletRequestBuilder apiGet(String path) {
		return get("/api" + path).contextPath("/api");
	}

	private MockHttpServletRequestBuilder apiPost(String path) {
		return post("/api" + path).contextPath("/api");
	}
}
