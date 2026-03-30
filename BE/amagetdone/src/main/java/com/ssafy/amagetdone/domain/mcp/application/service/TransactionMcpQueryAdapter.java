package com.ssafy.amagetdone.domain.mcp.application.service;

import java.time.LocalDate;
import java.time.YearMonth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.ListTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.TransactionAnalysisResponseFacade;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionMcpQueryAdapter {

	private final TransactionAnalysisResponseFacade transactionAnalysisResponseFacade;
	private final ListTransactionsUseCase listTransactionsUseCase;

	@Tool(
		name = "get_monthly_expense_summary",
		description = "Returns the monthly expense headline summary including total expense, fixed expense, variable expense, excluded income, and excluded internal transfers. Use when: the assistant needs a top-line monthly diagnosis before giving habit coaching. Best for: monthly review, explaining spending structure, and deciding whether deeper graph or category analysis is needed. Prefer get_monthly_spending_analysis when the user asks which categories drove the month."
	)
	public MonthlyExpenseDto.Response getMonthlyExpenseSummary(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "Year-month value such as 2026-03") String month
	) {
		return transactionAnalysisResponseFacade.getMonthlyExpense(user_id, parseYearMonth(month));
	}

	@Tool(
		name = "get_weekly_expense_graph",
		description = "Returns a day-by-day weekly expense graph. Use when: the assistant needs to spot which day of the week spending spiked. Best for: weekly behavior coaching, burst-spend detection, and explaining timing patterns. Prefer get_transaction_list or get_weekly_free_spending_detail when individual transactions are needed. Parameter caution: yearWeek must use yyyy-MM-w, for example 2026-03-3."
	)
	public WeeklyExpenseGraphDto.Response getWeeklyExpenseGraph(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "Transaction week value in yyyy-MM-w format such as 2026-03-3") String yearWeek
	) {
		return transactionAnalysisResponseFacade.getWeeklyExpenseGraph(user_id, yearWeek);
	}

	@Tool(
		name = "get_monthly_expense_graph",
		description = "Returns a week-by-week expense graph for one month. Use when: the assistant needs to explain how spending changed across the month instead of on a single day. Best for: monthly pacing review and identifying which weeks caused the month to go off track. Prefer get_monthly_expense_summary for a lighter top-line summary."
	)
	public MonthlyExpenseGraphDto.Response getMonthlyExpenseGraph(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "Year-month value such as 2026-03") String yearMonth
	) {
		return transactionAnalysisResponseFacade.getMonthlyExpenseGraph(user_id, yearMonth);
	}

	@Tool(
		name = "get_transaction_list",
		description = "Returns paged raw transaction history with optional date window and analysis inclusion filtering. Use when: the assistant needs transaction-level evidence, merchant names, or a source list for explanations. Best for: investigating why spending increased, validating a summary with concrete transactions, and building merchant-specific coaching. Prefer summary or graph tools first when the user only needs high-level insight."
	)
	public TransactionListDto.Response getTransactionList(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "Optional from-date in yyyy-MM-dd format") String fromDate,
		@ToolParam(description = "Optional to-date in yyyy-MM-dd format") String toDate,
		@ToolParam(description = "Optional analysis filter such as INCLUDED_ONLY") String analysisInclusion,
		@ToolParam(description = "Zero-based page number") Integer page,
		@ToolParam(description = "Page size") Integer size
	) {
		return listTransactionsUseCase.execute(user_id, new TransactionListDto.Query(
			parseLocalDate(fromDate),
			parseLocalDate(toDate),
			analysisInclusion,
			page,
			size
		));
	}

	@Tool(
		name = "get_onboarding_spending_analysis",
		description = "Returns the user's long-term spending baseline with average income, average expense, remaining amount, fixed-variable ratio, and category averages. Use when: the assistant needs a stable habit profile instead of a single-week or single-month snapshot. Best for: onboarding, habit diagnosis, and setting high-level coaching strategy. Prefer weekly or monthly analysis tools for recent behavior changes."
	)
	public TransactionAnalysisDto.Response getOnboardingSpendingAnalysis(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return TransactionAnalysisDto.Response.from(transactionAnalysisResponseFacade.getOnboardingAnalysis(user_id));
	}

	@Tool(
		name = "get_weekly_spending_analysis",
		description = "Returns weekly spending analysis including total expense, essential expense, fixed-variable essential split, and category actuals. Use when: the assistant needs a coaching view of this week's spending quality and category pressure. Best for: weekly coaching, identifying whether overspending came from essential or discretionary behavior, and picking the next drill-down tool. Parameter caution: yearWeek must use yyyy-MM-w, for example 2026-03-3."
	)
	public WeeklyTransactionAnalysisDto.Response getWeeklySpendingAnalysis(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "Transaction week value in yyyy-MM-w format such as 2026-03-3") String yearWeek
	) {
		return transactionAnalysisResponseFacade.getWeeklyAnalysis(user_id, yearWeek);
	}

	@Tool(
		name = "get_monthly_spending_analysis",
		description = "Returns monthly spending analysis with category totals and monthly essential burden. Use when: the assistant needs to explain which categories dominated the month and how much of the month was structurally essential. Best for: monthly habit review, category coaching, and comparing discretionary categories against mandatory spending load. Prefer get_monthly_expense_summary for a simpler totals-only view."
	)
	public MonthlyTransactionAnalysisDto.Response getMonthlySpendingAnalysis(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "Year-month value such as 2026-03") String yearMonth
	) {
		return transactionAnalysisResponseFacade.getMonthlyAnalysis(user_id, yearMonth);
	}

	private YearMonth parseYearMonth(String value) {
		return value == null || value.isBlank() ? null : YearMonth.parse(value);
	}

	private LocalDate parseLocalDate(String value) {
		return value == null || value.isBlank() ? null : LocalDate.parse(value);
	}
}
