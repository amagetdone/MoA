package com.ssafy.amagetdone.global.docs.controller;

import java.time.LocalDate;

import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisJobDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

@Tag(name = "거래", description = "거래 조회 및 분석 API")
public interface TransactionDocsController {

	@Operation(summary = "지출 요약 조회", description = "요청한 월 범위의 지출 요약을 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지출 요약 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<ExpenseSummaryDto.Response> getExpenseSummary(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "시작 월, yyyy-MM") String fromMonth,
		@Parameter(description = "종료 월, yyyy-MM") String toMonth,
		@Parameter(description = "집계 단위, 예: MONTH") String bucket
	);

	@Operation(summary = "월 지출 조회", description = "단일 월의 지출 합계를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "월 지출 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<MonthlyExpenseDto.Response> getMonthlyExpense(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 월, yyyy-MM") String month
	);

	@Operation(summary = "거래 내역 목록 조회", description = "날짜와 분석 옵션으로 필터링한 거래 내역을 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "거래 내역 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<TransactionListDto.Response> listTransactions(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "시작 날짜, yyyy-MM-dd") LocalDate fromDate,
		@Parameter(description = "종료 날짜, yyyy-MM-dd") LocalDate toDate,
		@Parameter(description = "분석 포함 옵션") String analysisInclusion,
		@Parameter(description = "페이지 번호") Integer page,
		@Parameter(description = "페이지 크기") Integer size
	);

	@Operation(summary = "분석 작업 생성", description = "거래 분석 작업을 생성합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "분석 작업 접수 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ResponseEntity<ApiResponse<TransactionAnalysisJobDto.CreateResponse>> createAnalysisJob(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "분석 작업 생성 요청",
			content = @Content(schema = @Schema(implementation = TransactionAnalysisJobDto.CreateRequest.class))
		)
		TransactionAnalysisJobDto.CreateRequest request
	);

	@Operation(summary = "분석 작업 상태 조회", description = "분석 작업의 상태를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "분석 작업 상태 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<TransactionAnalysisJobDto.StatusResponse> getAnalysisJobStatus(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "분석 작업 ID") String jobId
	);

	@Operation(summary = "주간 분석 조회", description = "주간 거래 분석 결과를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주간 분석 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<WeeklyTransactionAnalysisDto.Response> getWeeklyAnalysis(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 주차, yyyy-Www") String yearWeek
	);

	@Operation(summary = "월간 분석 조회", description = "월간 거래 분석 결과를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "월간 분석 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<MonthlyTransactionAnalysisDto.Response> getMonthlyAnalysis(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 월, yyyy-MM") String yearMonth
	);
}
