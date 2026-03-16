package com.ssafy.amagetdone.global.docs.controller;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisJobDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

@Tag(name = "Transaction", description = "Transaction query and analysis APIs")
public interface TransactionDocsController {

	@Operation(summary = "Get expense summary", description = "Returns summarized expenses for the requested month range.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expense summary retrieved"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
	})
	ApiResponse<ExpenseSummaryDto.Response> getExpenseSummary(
		@Parameter(hidden = true) String authorization,
		@Parameter(description = "Start month, yyyy-MM") String fromMonth,
		@Parameter(description = "End month, yyyy-MM") String toMonth,
		@Parameter(description = "Bucket unit, for example MONTH") String bucket
	);

	@Operation(summary = "Get monthly expense", description = "Returns expense totals for a single month.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly expense retrieved"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
	})
	ApiResponse<MonthlyExpenseDto.Response> getMonthlyExpense(
		@Parameter(hidden = true) String authorization,
		@Parameter(description = "Target month, yyyy-MM") String month
	);

	@Operation(summary = "List transactions", description = "Returns transactions filtered by date and analysis options.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transactions retrieved"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
	})
	ApiResponse<TransactionListDto.Response> listTransactions(
		@Parameter(hidden = true) String authorization,
		@Parameter(description = "Start date, yyyy-MM-dd") LocalDate fromDate,
		@Parameter(description = "End date, yyyy-MM-dd") LocalDate toDate,
		@Parameter(description = "Transaction source type") String sourceType,
		@Parameter(description = "Analysis inclusion option") String analysisInclusion,
		@Parameter(description = "Page number") Integer page,
		@Parameter(description = "Page size") Integer size
	);

	@Operation(summary = "Create analysis job", description = "Creates a transaction analysis job.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Analysis job accepted"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
	})
	ResponseEntity<ApiResponse<TransactionAnalysisJobDto.CreateResponse>> createAnalysisJob(
		@Parameter(hidden = true) String authorization,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "Analysis job request",
			content = @Content(schema = @Schema(implementation = TransactionAnalysisJobDto.CreateRequest.class))
		)
		TransactionAnalysisJobDto.CreateRequest request
	);

	@Operation(summary = "Get analysis job status", description = "Returns the status of an analysis job.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Analysis job status retrieved"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
	})
	ApiResponse<TransactionAnalysisJobDto.StatusResponse> getAnalysisJobStatus(
		@Parameter(hidden = true) String authorization,
		@Parameter(description = "Analysis job id") String jobId
	);

	@Operation(summary = "Get weekly analysis", description = "Returns weekly transaction analysis.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Weekly analysis retrieved"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
	})
	ApiResponse<WeeklyTransactionAnalysisDto.Response> getWeeklyAnalysis(
		@Parameter(hidden = true) String authorization,
		@Parameter(description = "Target week, yyyy-Www") String yearWeek
	);

	@Operation(summary = "Get monthly analysis", description = "Returns monthly transaction analysis.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly analysis retrieved"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
	})
	ApiResponse<MonthlyTransactionAnalysisDto.Response> getMonthlyAnalysis(
		@Parameter(hidden = true) String authorization,
		@Parameter(description = "Target month, yyyy-MM") String yearMonth
	);
}
