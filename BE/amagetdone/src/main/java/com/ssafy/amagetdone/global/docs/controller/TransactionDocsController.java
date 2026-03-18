package com.ssafy.amagetdone.global.docs.controller;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionSyncDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Transactions", description = "Transaction query and analysis API")
public interface TransactionDocsController {

	@Operation(summary = "Get expense summary", description = "Returns an expense summary for the requested month range.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expense summary returned"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<ExpenseSummaryDto.Response> getExpenseSummary(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "Start month in yyyy-MM") String fromMonth,
		@Parameter(description = "End month in yyyy-MM") String toMonth,
		@Parameter(description = "Aggregation bucket such as MONTH") String bucket
	);

	@Operation(summary = "Get monthly expense", description = "Returns monthly expense totals for a single month.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly expense returned"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<MonthlyExpenseDto.Response> getMonthlyExpense(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "Target month in yyyy-MM") String month
	);

	@Operation(summary = "List transactions", description = "Returns filtered transactions by date range and analysis options.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction list returned"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<TransactionListDto.Response> listTransactions(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "Start date in yyyy-MM-dd") LocalDate fromDate,
		@Parameter(description = "End date in yyyy-MM-dd") LocalDate toDate,
		@Parameter(description = "Analysis inclusion option") String analysisInclusion,
		@Parameter(description = "Page number") Integer page,
		@Parameter(description = "Page size") Integer size
	);

	@Operation(
		summary = "Analyze transactions",
		description = "Runs synchronous transaction analysis for recently completed months and returns onboarding output."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction analysis completed"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<TransactionAnalysisDto.Response> analyzeTransactions(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "Transaction analysis request",
			content = @Content(schema = @Schema(implementation = TransactionAnalysisDto.Request.class))
		)
		TransactionAnalysisDto.Request request
	);

	@Operation(summary = "Sync transactions", description = "Synchronizes SSAFY transactions into the local database.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction sync completed"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<TransactionSyncDto.Response> syncTransactions(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "Transaction sync request",
			content = @Content(schema = @Schema(implementation = TransactionSyncDto.Request.class))
		)
		TransactionSyncDto.Request request
	);

	@Operation(
		summary = "Get onboarding analysis",
		description = "Returns the latest stored onboarding analysis result produced by transaction analysis."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Onboarding analysis returned"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<TransactionAnalysisDto.Response> getOnboardingAnalysis(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember
	);

	@Operation(summary = "Get weekly analysis", description = "Returns weekly transaction analysis output.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Weekly analysis returned"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<WeeklyTransactionAnalysisDto.Response> getWeeklyAnalysis(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "ISO year-week in yyyy-Www") String yearWeek
	);

	@Operation(summary = "Get monthly analysis", description = "Returns monthly transaction analysis output.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly analysis returned"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<MonthlyTransactionAnalysisDto.Response> getMonthlyAnalysis(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "Target month in yyyy-MM") String yearMonth
	);
}
