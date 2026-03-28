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

	@Tool(
		name = "get_budget_income",
		description = "Returns the user's current monthly planned income. Use when: the coaching assistant needs the income baseline before judging spending pace, affordability, or savings capacity. Best for: opening budget-aware coaching and validating the user's assumed income context. Prefer get_budget_home when you also need current free-spending, savings progress, and plan status."
	)
	public BudgetIncomeDto.Response getBudgetIncome(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return getBudgetIncomeUseCase.execute(user_id);
	}

	@Tool(
		name = "get_budget_dashboard",
		description = "Returns the user's current weekly free-spending pace, remaining allowance, and daily safe-spend estimate. Use when: the assistant must answer questions like can I spend more this week or why am I close to overspending. Best for: short-term spending coaching and warning about weekly burn rate. Prefer get_weekly_free_spending_detail when you need the actual transactions causing the problem."
	)
	public BudgetDashboardDto.Response getBudgetDashboard(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return getBudgetDashboardUseCase.execute(user_id);
	}

	@Tool(
		name = "get_budget_home",
		description = "Returns the main coaching snapshot: free-spending status, weekly saving progress, asset summary, and active plans. Use when: the assistant needs one compact view before giving personalized spending or saving advice. Best for: first-response coaching, current status summaries, and deciding which detailed tool to call next. Prefer detailed weekly or analysis tools when the user asks why a number changed."
	)
	public BudgetHomeDto.Response getBudgetHome(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return getBudgetHomeUseCase.execute(user_id);
	}

	@Tool(
		name = "get_budget_summary",
		description = "Returns high-level budget and goal progress counts such as active plans, completed plans, and total saved amount. Use when: the assistant needs a lightweight progress summary without weekly detail. Best for: motivational summaries and goal progress check-ins. Prefer get_budget_home or get_weekly_savings_detail when the user asks about current weekly saving behavior."
	)
	public BudgetSummaryDto.Response getBudgetSummary(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return getBudgetSummaryUseCase.execute(user_id);
	}

	@Tool(
		name = "get_budget_week_setup",
		description = "Returns the weekly budget setup including planned income, essential budget, savings allocation, and free-spending allocation. Use when: the assistant needs to explain how the week was planned before evaluating execution. Best for: planning explanations, allocation review, and comparing planned amounts with later actuals. Prefer get_weekly_free_spending_detail or get_weekly_essential_expense_detail when the user asks about actual transaction drivers."
	)
	public BudgetWeekSetupDto.Response getBudgetWeekSetup(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "ISO week value such as 2026-W13") String yearWeek
	) {
		return getBudgetWeekSetupUseCase.execute(user_id, yearWeek);
	}

	@Tool(
		name = "get_weekly_savings_detail",
		description = "Returns weekly savings detail by plan, including planned saving, final saving, and adjustment history. Use when: the assistant must explain why saving is ahead or behind target this week. Best for: coaching on savings discipline, plan priority trade-offs, and weekly goal execution. Prefer get_budget_summary for broad progress only."
	)
	public WeeklySavingsDetailDto.Response getWeeklySavingsDetail(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "ISO week value such as 2026-W13") String yearWeek
	) {
		return getWeeklySavingsDetailUseCase.execute(user_id, yearWeek);
	}

	@Tool(
		name = "get_weekly_free_spending_detail",
		description = "Returns the weekly free-spending detail and the transactions counted against that allowance. Use when: the assistant needs evidence for why discretionary budget fell or which purchases caused overspending. Best for: spend-control coaching, merchant-level explanations, and pinpointing avoidable discretionary purchases. Prefer get_budget_dashboard when only a top-line weekly allowance summary is needed."
	)
	public WeeklyFreeSpendingDetailDto.Response getWeeklyFreeSpendingDetail(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "ISO week value such as 2026-W13") String yearWeek
	) {
		return getWeeklyFreeSpendingDetailUseCase.execute(user_id, yearWeek);
	}

	@Tool(
		name = "get_weekly_essential_expenses",
		description = "Returns the weekly essential-expense analysis split into fixed and variable essential categories. Use when: the assistant wants to explain how much of the week's pressure came from necessary spending rather than optional spending. Best for: habit coaching that separates controllable spending from mandatory burden. Prefer get_weekly_essential_expense_detail when category or transaction-level drivers are required."
	)
	public WeeklyEssentialExpenseDto.Response getWeeklyEssentialExpenses(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "ISO week value such as 2026-W13") String yearWeek
	) {
		return getWeeklyEssentialExpensesUseCase.execute(user_id, yearWeek);
	}

	@Tool(
		name = "get_weekly_essential_expense_detail",
		description = "Returns detailed weekly essential-expense categories with planned amount, actual amount, remaining amount, and supporting transactions. Use when: the assistant must identify which essential categories are driving budget pressure. Best for: diagnosing rent, transport, food, or other must-pay category overruns and planning corrections. Prefer get_weekly_essential_expenses for a lighter summary view."
	)
	public WeeklyEssentialExpenseDetailDto.Response getWeeklyEssentialExpenseDetail(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "ISO week value such as 2026-W13") String yearWeek
	) {
		return getWeeklyEssentialExpenseDetailUseCase.execute(user_id, yearWeek);
	}
}
