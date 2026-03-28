package com.ssafy.amagetdone.domain.mcp.application.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetDashboardDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetHomeDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetIncomeDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetSummaryDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSetupDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDetailDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyFreeSpendingDetailDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetDashboardUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetHomeUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetIncomeUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetSummaryUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetWeekSetupUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyEssentialExpenseDetailUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyEssentialExpensesUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyFreeSpendingDetailUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklySavingsDetailUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BudgetMcpQueryAdapter {

	private final GetBudgetIncomeUseCase getBudgetIncomeUseCase;
	private final GetBudgetDashboardUseCase getBudgetDashboardUseCase;
	private final GetBudgetHomeUseCase getBudgetHomeUseCase;
	private final GetBudgetSummaryUseCase getBudgetSummaryUseCase;
	private final GetBudgetWeekSetupUseCase getBudgetWeekSetupUseCase;
	private final GetWeeklySavingsDetailUseCase getWeeklySavingsDetailUseCase;
	private final GetWeeklyFreeSpendingDetailUseCase getWeeklyFreeSpendingDetailUseCase;
	private final GetWeeklyEssentialExpensesUseCase getWeeklyEssentialExpensesUseCase;
	private final GetWeeklyEssentialExpenseDetailUseCase getWeeklyEssentialExpenseDetailUseCase;

	@Tool(name = "get_budget_income", description = "Returns the current monthly income budget for the given user.")
	public BudgetIncomeDto.Response getBudgetIncome(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return getBudgetIncomeUseCase.execute(user_id);
	}

	@Tool(name = "get_budget_dashboard", description = "Returns the current weekly free-spending dashboard for the given user.")
	public BudgetDashboardDto.Response getBudgetDashboard(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return getBudgetDashboardUseCase.execute(user_id);
	}

	@Tool(name = "get_budget_home", description = "Returns the budget home summary for the given user.")
	public BudgetHomeDto.Response getBudgetHome(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return getBudgetHomeUseCase.execute(user_id);
	}

	@Tool(name = "get_budget_summary", description = "Returns the budget summary counts and saved amount for the given user.")
	public BudgetSummaryDto.Response getBudgetSummary(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return getBudgetSummaryUseCase.execute(user_id);
	}

	@Tool(name = "get_budget_week_setup", description = "Returns the budget week setup view for the given user and ISO year-week.")
	public BudgetWeekSetupDto.Response getBudgetWeekSetup(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "ISO week value such as 2026-W13") String yearWeek
	) {
		return getBudgetWeekSetupUseCase.execute(user_id, yearWeek);
	}

	@Tool(name = "get_weekly_savings_detail", description = "Returns weekly savings detail for the given user and ISO year-week.")
	public WeeklySavingsDetailDto.Response getWeeklySavingsDetail(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "ISO week value such as 2026-W13") String yearWeek
	) {
		return getWeeklySavingsDetailUseCase.execute(user_id, yearWeek);
	}

	@Tool(name = "get_weekly_free_spending_detail", description = "Returns weekly free-spending detail for the given user and ISO year-week.")
	public WeeklyFreeSpendingDetailDto.Response getWeeklyFreeSpendingDetail(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "ISO week value such as 2026-W13") String yearWeek
	) {
		return getWeeklyFreeSpendingDetailUseCase.execute(user_id, yearWeek);
	}

	@Tool(name = "get_weekly_essential_expenses", description = "Returns weekly essential expense analysis for the given user and ISO year-week.")
	public WeeklyEssentialExpenseDto.Response getWeeklyEssentialExpenses(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "ISO week value such as 2026-W13") String yearWeek
	) {
		return getWeeklyEssentialExpensesUseCase.execute(user_id, yearWeek);
	}

	@Tool(name = "get_weekly_essential_expense_detail", description = "Returns weekly essential expense detail for the given user and ISO year-week.")
	public WeeklyEssentialExpenseDetailDto.Response getWeeklyEssentialExpenseDetail(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "ISO week value such as 2026-W13") String yearWeek
	) {
		return getWeeklyEssentialExpenseDetailUseCase.execute(user_id, yearWeek);
	}
}
