package com.ssafy.amagetdone.domain.transaction.api.controller;

import java.time.LocalDate;
import java.time.YearMonth;

import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisJobDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetExpenseSummaryUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyExpenseUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetTransactionAnalysisJobUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.ListTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.StartTransactionAnalysisJobUseCase;
import com.ssafy.amagetdone.global.response.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TransactionQueryController {

	private final JwtAccessTokenReader jwtAccessTokenReader;
	private final GetExpenseSummaryUseCase getExpenseSummaryUseCase;
	private final GetMonthlyExpenseUseCase getMonthlyExpenseUseCase;
	private final ListTransactionsUseCase listTransactionsUseCase;
	private final StartTransactionAnalysisJobUseCase startTransactionAnalysisJobUseCase;
	private final GetTransactionAnalysisJobUseCase getTransactionAnalysisJobUseCase;

	public TransactionQueryController(
		JwtAccessTokenReader jwtAccessTokenReader,
		GetExpenseSummaryUseCase getExpenseSummaryUseCase,
		GetMonthlyExpenseUseCase getMonthlyExpenseUseCase,
		ListTransactionsUseCase listTransactionsUseCase,
		StartTransactionAnalysisJobUseCase startTransactionAnalysisJobUseCase,
		GetTransactionAnalysisJobUseCase getTransactionAnalysisJobUseCase
	) {
		this.jwtAccessTokenReader = jwtAccessTokenReader;
		this.getExpenseSummaryUseCase = getExpenseSummaryUseCase;
		this.getMonthlyExpenseUseCase = getMonthlyExpenseUseCase;
		this.listTransactionsUseCase = listTransactionsUseCase;
		this.startTransactionAnalysisJobUseCase = startTransactionAnalysisJobUseCase;
		this.getTransactionAnalysisJobUseCase = getTransactionAnalysisJobUseCase;
	}

	@GetMapping("/expenses/summary")
	public ApiResponse<ExpenseSummaryDto.Response> getExpenseSummary(
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@RequestParam(name = "fromMonth", required = false) String fromMonth,
		@RequestParam(name = "toMonth", required = false) String toMonth,
		@RequestParam(name = "bucket", required = false) String bucket
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok(
			"지출 요약을 조회했습니다.",
			getExpenseSummaryUseCase.execute(userId, parseYearMonth(fromMonth), parseYearMonth(toMonth), bucket)
		);
	}

	@GetMapping("/expenses/monthly")
	public ApiResponse<MonthlyExpenseDto.Response> getMonthlyExpense(
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@RequestParam(name = "month", required = false) String month
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok(
			"월 소비 금액을 조회했습니다.",
			getMonthlyExpenseUseCase.execute(userId, parseYearMonth(month))
		);
	}

	@GetMapping
	public ApiResponse<TransactionListDto.Response> listTransactions(
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@RequestParam(name = "fromDate", required = false) LocalDate fromDate,
		@RequestParam(name = "toDate", required = false) LocalDate toDate,
		@RequestParam(name = "sourceType", required = false) String sourceType,
		@RequestParam(name = "analysisInclusion", required = false) String analysisInclusion,
		@RequestParam(name = "page", required = false) Integer page,
		@RequestParam(name = "size", required = false) Integer size
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok(
			"거래내역을 조회했습니다.",
			listTransactionsUseCase.execute(userId, new TransactionListDto.Query(
				fromDate,
				toDate,
				sourceType,
				analysisInclusion,
				page,
				size
			))
		);
	}

	@PostMapping("/analysis-jobs")
	public ResponseEntity<ApiResponse<TransactionAnalysisJobDto.CreateResponse>> createAnalysisJob(
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@RequestBody TransactionAnalysisJobDto.CreateRequest request
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ResponseEntity.status(HttpStatus.ACCEPTED)
			.body(ApiResponse.ok(
				"소비 분석 작업을 시작했습니다.",
				TransactionAnalysisJobDto.CreateResponse.from(
					startTransactionAnalysisJobUseCase.execute(
						userId,
						request.months(),
						Boolean.TRUE.equals(request.forceFullSync())
					)
				)
			));
	}

	@GetMapping("/analysis-jobs/{jobId}")
	public ApiResponse<TransactionAnalysisJobDto.StatusResponse> getAnalysisJobStatus(
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@PathVariable("jobId") String jobId
	) {
		jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok(
			"소비 분석 작업 상태를 조회했습니다.",
			TransactionAnalysisJobDto.StatusResponse.from(getTransactionAnalysisJobUseCase.execute(jobId))
		);
	}

	private YearMonth parseYearMonth(String value) {
		return value == null || value.isBlank() ? null : YearMonth.parse(value);
	}
}
