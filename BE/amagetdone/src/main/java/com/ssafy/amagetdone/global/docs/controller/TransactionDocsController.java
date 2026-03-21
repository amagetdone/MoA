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

@Tag(name = "Transaction API", description = "Transaction query and analysis APIs")
public interface TransactionDocsController {

	@Operation(summary = "Get expense summary", description = "Returns expense summary for the requested month range.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expense summary retrieved"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<ExpenseSummaryDto.Response> getExpenseSummary(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "Start month (yyyy-MM)") String fromMonth,
		@Parameter(description = "End month (yyyy-MM)") String toMonth,
		@Parameter(description = "Aggregation bucket: MONTH or WEEK") String bucket
	);

	@Operation(summary = "Get monthly expense", description = "Returns monthly expense totals and composition.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly expense retrieved"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<MonthlyExpenseDto.Response> getMonthlyExpense(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "Target month (yyyy-MM)") String month
	);

	@Operation(summary = "List transactions", description = "Returns transactions filtered by date range and inclusion.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transactions retrieved"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<TransactionListDto.Response> listTransactions(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "Start date (yyyy-MM-dd)") LocalDate fromDate,
		@Parameter(description = "End date (yyyy-MM-dd)") LocalDate toDate,
		@Parameter(description = "Analysis inclusion filter") String analysisInclusion,
		@Parameter(description = "Page number") Integer page,
		@Parameter(description = "Page size") Integer size
	);

	@Operation(summary = "Run transaction analysis", description = "Runs analysis for the latest completed months window.")
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

	@Operation(summary = "Run monthly transaction analysis", description = "Runs analysis only for the requested month.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly transaction analysis completed"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<TransactionAnalysisDto.Response> analyzeMonthlyTransactions(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "Target month (yyyy-MM)", required = true) String month
	);

	@Operation(summary = "Sync transactions", description = "Syncs transaction history from SSAFY into the local database.")
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

	@Operation(summary = "Get onboarding analysis", description = "Returns the latest saved onboarding analysis result.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Onboarding analysis retrieved"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<TransactionAnalysisDto.Response> getOnboardingAnalysis(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember
	);

	@Operation(summary = "Get weekly analysis", description = "Returns weekly transaction analysis result.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Weekly analysis retrieved"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<WeeklyTransactionAnalysisDto.Response> getWeeklyAnalysis(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "Target week (yyyy-Www or yyyy-MM-w)") String yearWeek
	);

	@Operation(summary = "Get monthly analysis", description = "Returns monthly transaction analysis result.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly analysis retrieved"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
	})
	ApiResponse<MonthlyTransactionAnalysisDto.Response> getMonthlyAnalysis(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "Target month (yyyy-MM)") String yearMonth
	);
}
