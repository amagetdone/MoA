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

@Tag(name = "거래", description = "거래 조회 및 분석 문서")
public interface TransactionDocsController {

	@Operation(summary = "지출 요약 조회", description = "요청한 월 범위의 지출 요약 정보를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지출 요약 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다", content = @Content)
	})
	ApiResponse<ExpenseSummaryDto.Response> getExpenseSummary(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "조회 시작 월 (yyyy-MM)") String fromMonth,
		@Parameter(description = "조회 종료 월 (yyyy-MM)") String toMonth,
		@Parameter(description = "집계 기준: MONTH 또는 WEEK") String bucket
	);

	@Operation(summary = "월간 지출 조회", description = "특정 월의 총지출과 구성 정보를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "월간 지출 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다", content = @Content)
	})
	ApiResponse<MonthlyExpenseDto.Response> getMonthlyExpense(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 월 (yyyy-MM)") String month
	);

	@Operation(summary = "거래 내역 조회", description = "기간과 포함 조건으로 필터링한 거래 내역을 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "거래 내역 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다", content = @Content)
	})
	ApiResponse<TransactionListDto.Response> listTransactions(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "조회 시작일 (yyyy-MM-dd)") LocalDate fromDate,
		@Parameter(description = "조회 종료일 (yyyy-MM-dd)") LocalDate toDate,
		@Parameter(description = "분석 포함 여부 필터") String analysisInclusion,
		@Parameter(description = "페이지 번호") Integer page,
		@Parameter(description = "페이지 크기") Integer size
	);

	@Operation(summary = "거래 분석 실행", description = "가장 최근 완료된 월 구간을 기준으로 거래 분석을 실행합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "거래 분석 실행 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다", content = @Content)
	})
	ApiResponse<TransactionAnalysisDto.Response> analyzeTransactions(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "거래 분석 요청",
			content = @Content(schema = @Schema(implementation = TransactionAnalysisDto.Request.class))
		)
		TransactionAnalysisDto.Request request
	);

	@Operation(summary = "월별 거래 분석 실행", description = "요청한 월에 대해서만 거래 분석을 실행합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "월별 거래 분석 실행 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다", content = @Content)
	})
	ApiResponse<TransactionAnalysisDto.Response> analyzeMonthlyTransactions(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 월 (yyyy-MM)", required = true) String month
	);

	@Operation(summary = "거래 동기화", description = "싸피 거래 내역을 로컬 데이터베이스로 동기화합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "거래 동기화 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다", content = @Content)
	})
	ApiResponse<TransactionSyncDto.Response> syncTransactions(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "거래 동기화 요청",
			content = @Content(schema = @Schema(implementation = TransactionSyncDto.Request.class))
		)
		TransactionSyncDto.Request request
	);

	@Operation(summary = "온보딩 분석 조회", description = "가장 최근에 저장된 온보딩 분석 결과를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "온보딩 분석 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다", content = @Content)
	})
	ApiResponse<TransactionAnalysisDto.Response> getOnboardingAnalysis(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember
	);

	@Operation(summary = "주간 분석 조회", description = "주간 거래 분석 결과를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주간 분석 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다", content = @Content)
	})
	ApiResponse<WeeklyTransactionAnalysisDto.Response> getWeeklyAnalysis(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 주차 (yyyy-MM-w)") String yearWeek
	);

	@Operation(summary = "월간 분석 조회", description = "월간 거래 분석 결과를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "월간 분석 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다", content = @Content)
	})
	ApiResponse<MonthlyTransactionAnalysisDto.Response> getMonthlyAnalysis(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@Parameter(description = "대상 월 (yyyy-MM)") String yearMonth
	);
}
