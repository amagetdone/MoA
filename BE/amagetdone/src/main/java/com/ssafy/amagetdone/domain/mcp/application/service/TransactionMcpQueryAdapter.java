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

	@Tool(name = "get_monthly_expense_summary", description = "Returns the monthly expense summary for the given user and year-month.")
	public MonthlyExpenseDto.Response getMonthlyExpenseSummary(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "Year-month value such as 2026-03") String month
	) {
		return transactionAnalysisResponseFacade.getMonthlyExpense(user_id, parseYearMonth(month));
	}

	@Tool(name = "get_weekly_expense_graph", description = "Returns the weekly expense graph for the given user and ISO year-week.")
	public WeeklyExpenseGraphDto.Response getWeeklyExpenseGraph(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "ISO week value such as 2026-W13") String yearWeek
	) {
		return transactionAnalysisResponseFacade.getWeeklyExpenseGraph(user_id, yearWeek);
	}

	@Tool(name = "get_monthly_expense_graph", description = "Returns the monthly expense graph for the given user and year-month.")
	public MonthlyExpenseGraphDto.Response getMonthlyExpenseGraph(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "Year-month value such as 2026-03") String yearMonth
	) {
		return transactionAnalysisResponseFacade.getMonthlyExpenseGraph(user_id, yearMonth);
	}

	@Tool(name = "get_transaction_list", description = "Returns paged transaction history for the given user.")
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

	@Tool(name = "get_onboarding_spending_analysis", description = "Returns the onboarding spending analysis for the given user.")
	public TransactionAnalysisDto.Response getOnboardingSpendingAnalysis(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id
	) {
		return TransactionAnalysisDto.Response.from(transactionAnalysisResponseFacade.getOnboardingAnalysis(user_id));
	}

	@Tool(name = "get_weekly_spending_analysis", description = "Returns the weekly spending analysis for the given user and ISO year-week.")
	public WeeklyTransactionAnalysisDto.Response getWeeklySpendingAnalysis(
		@JsonProperty("user_id") @ToolParam(description = "Internal user identifier") Integer user_id,
		@ToolParam(description = "ISO week value such as 2026-W13") String yearWeek
	) {
		return transactionAnalysisResponseFacade.getWeeklyAnalysis(user_id, yearWeek);
	}

	@Tool(name = "get_monthly_spending_analysis", description = "Returns the monthly spending analysis for the given user and year-month.")
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
