package com.ssafy.amagetdone.domain.transaction.api.controller;

import java.time.LocalDate;
import java.time.YearMonth;

import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionSyncDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.ListTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.SyncLatestTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.TransactionAnalysisResponseFacade;
import com.ssafy.amagetdone.global.docs.controller.TransactionDocsController;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TransactionController implements TransactionDocsController {

	private final TransactionAnalysisResponseFacade transactionAnalysisResponseFacade;
	private final ListTransactionsUseCase listTransactionsUseCase;
	private final SyncLatestTransactionsUseCase syncLatestTransactionsUseCase;

	@GetMapping("/expenses/summary")
	public ApiResponse<ExpenseSummaryDto.Response> getExpenseSummary(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestParam(name = "fromMonth", required = false) String fromMonth,
		@RequestParam(name = "toMonth", required = false) String toMonth,
		@RequestParam(name = "bucket", required = false) String bucket
	) {
		return ApiResponse.ok(
			"지출 요약을 조회했습니다.",
			transactionAnalysisResponseFacade.getExpenseSummary(
				authenticatedMember.userId(),
				parseYearMonth(fromMonth),
				parseYearMonth(toMonth),
				bucket
			)
		);
	}

	@GetMapping("/expenses/monthly")
	public ApiResponse<MonthlyExpenseDto.Response> getMonthlyExpense(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestParam(name = "month", required = false) String month
	) {
		return ApiResponse.ok(
			"월간 지출을 조회했습니다.",
			transactionAnalysisResponseFacade.getMonthlyExpense(authenticatedMember.userId(), parseYearMonth(month))
		);
	}

	@GetMapping
	public ApiResponse<TransactionListDto.Response> listTransactions(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestParam(name = "fromDate", required = false) LocalDate fromDate,
		@RequestParam(name = "toDate", required = false) LocalDate toDate,
		@RequestParam(name = "analysisInclusion", required = false) String analysisInclusion,
		@RequestParam(name = "page", required = false) Integer page,
		@RequestParam(name = "size", required = false) Integer size
	) {
		return ApiResponse.ok(
			"거래 내역을 조회했습니다.",
			listTransactionsUseCase.execute(authenticatedMember.userId(), new TransactionListDto.Query(
				fromDate,
				toDate,
				analysisInclusion,
				page,
				size
			))
		);
	}

	@PostMapping("/analysis")
	public ApiResponse<TransactionAnalysisDto.Response> analyzeTransactions(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody TransactionAnalysisDto.Request request
	) {
		return ApiResponse.ok(
			"거래 분석을 완료했습니다.",
			TransactionAnalysisDto.Response.from(
				transactionAnalysisResponseFacade.analyzeTransactions(
					authenticatedMember.userId(),
					request.months()
				)
			)
		);
	}

	@PostMapping("/sync")
	public ApiResponse<TransactionSyncDto.Response> syncTransactions(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody TransactionSyncDto.Request request
	) {
		return ApiResponse.ok(
			"거래 동기화를 완료했습니다.",
			TransactionSyncDto.Response.from(
				syncLatestTransactionsUseCase.execute(
					authenticatedMember.userId(),
					request.months(),
					Boolean.TRUE.equals(request.forceFullSync())
				)
			)
		);
	}

	@GetMapping("/analysis")
	public ApiResponse<TransactionAnalysisDto.Response> getOnboardingAnalysis(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember
	) {
		return ApiResponse.ok(
			"온보딩 분석 결과를 조회했습니다.",
			TransactionAnalysisDto.Response.from(
				transactionAnalysisResponseFacade.getOnboardingAnalysis(authenticatedMember.userId())
			)
		);
	}

	@GetMapping("/analysis/weeks/{yearWeek}")
	public ApiResponse<WeeklyTransactionAnalysisDto.Response> getWeeklyAnalysis(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable("yearWeek") String yearWeek
	) {
		return ApiResponse.ok(
			"주간 분석 결과를 조회했습니다.",
			transactionAnalysisResponseFacade.getWeeklyAnalysis(authenticatedMember.userId(), yearWeek)
		);
	}

	@GetMapping("/analysis/months/{yearMonth}")
	public ApiResponse<MonthlyTransactionAnalysisDto.Response> getMonthlyAnalysis(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable("yearMonth") String yearMonth
	) {
		return ApiResponse.ok(
			"월간 분석 결과를 조회했습니다.",
			transactionAnalysisResponseFacade.getMonthlyAnalysis(authenticatedMember.userId(), yearMonth)
		);
	}

	private YearMonth parseYearMonth(String value) {
		return value == null || value.isBlank() ? null : YearMonth.parse(value);
	}
}
