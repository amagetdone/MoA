package com.ssafy.amagetdone.domain.transaction.api.controller;

import java.time.LocalDate;
import java.time.YearMonth;

import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisJobDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetExpenseSummaryUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyExpenseUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetTransactionAnalysisJobUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetWeeklyTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.ListTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.StartTransactionAnalysisJobUseCase;
import com.ssafy.amagetdone.global.docs.controller.TransactionDocsController;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class TransactionController implements TransactionDocsController {

	private final GetExpenseSummaryUseCase getExpenseSummaryUseCase;
	private final GetMonthlyExpenseUseCase getMonthlyExpenseUseCase;
	private final ListTransactionsUseCase listTransactionsUseCase;
	private final StartTransactionAnalysisJobUseCase startTransactionAnalysisJobUseCase;
	private final GetTransactionAnalysisJobUseCase getTransactionAnalysisJobUseCase;
	private final GetWeeklyTransactionAnalysisUseCase getWeeklyTransactionAnalysisUseCase;
	private final GetMonthlyTransactionAnalysisUseCase getMonthlyTransactionAnalysisUseCase;

	@GetMapping("/expenses/summary")
	public ApiResponse<ExpenseSummaryDto.Response> getExpenseSummary(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestParam(name = "fromMonth", required = false) String fromMonth,
		@RequestParam(name = "toMonth", required = false) String toMonth,
		@RequestParam(name = "bucket", required = false) String bucket
	) {
		return ApiResponse.ok(
			"吏異??붿빟??議고쉶?덉뒿?덈떎.",
			getExpenseSummaryUseCase.execute(
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
			"?붾퀎 ?뚮퉬 湲덉븸??議고쉶?덉뒿?덈떎.",
			getMonthlyExpenseUseCase.execute(authenticatedMember.userId(), parseYearMonth(month))
		);
	}

	@GetMapping
	public ApiResponse<TransactionListDto.Response> listTransactions(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestParam(name = "fromDate", required = false) LocalDate fromDate,
		@RequestParam(name = "toDate", required = false) LocalDate toDate,
		@RequestParam(name = "sourceType", required = false) String sourceType,
		@RequestParam(name = "analysisInclusion", required = false) String analysisInclusion,
		@RequestParam(name = "page", required = false) Integer page,
		@RequestParam(name = "size", required = false) Integer size
	) {
		return ApiResponse.ok(
			"嫄곕옒?댁뿭??議고쉶?덉뒿?덈떎.",
			listTransactionsUseCase.execute(authenticatedMember.userId(), new TransactionListDto.Query(
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
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody TransactionAnalysisJobDto.CreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.ACCEPTED)
			.body(ApiResponse.ok(
				"?뚮퉬 遺꾩꽍 ?묒뾽???쒖옉?덉뒿?덈떎.",
				TransactionAnalysisJobDto.CreateResponse.from(
					startTransactionAnalysisJobUseCase.execute(
						authenticatedMember.userId(),
						request.months(),
						Boolean.TRUE.equals(request.forceFullSync())
					)
				)
			));
	}

	@GetMapping("/analysis-jobs/{jobId}")
	public ApiResponse<TransactionAnalysisJobDto.StatusResponse> getAnalysisJobStatus(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable("jobId") String jobId
	) {
		authenticatedMember.userId();
		return ApiResponse.ok(
			"?뚮퉬 遺꾩꽍 ?묒뾽 ?곹깭瑜?議고쉶?덉뒿?덈떎.",
			TransactionAnalysisJobDto.StatusResponse.from(getTransactionAnalysisJobUseCase.execute(jobId))
		);
	}

	@GetMapping("/analysis/weeks/{yearWeek}")
	public ApiResponse<WeeklyTransactionAnalysisDto.Response> getWeeklyAnalysis(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable("yearWeek") String yearWeek
	) {
		return ApiResponse.ok(
			"二쇨컙 ?뚮퉬 遺꾩꽍??議고쉶?덉뒿?덈떎.",
			getWeeklyTransactionAnalysisUseCase.execute(authenticatedMember.userId(), yearWeek)
		);
	}

	@GetMapping("/analysis/months/{yearMonth}")
	public ApiResponse<MonthlyTransactionAnalysisDto.Response> getMonthlyAnalysis(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable("yearMonth") String yearMonth
	) {
		return ApiResponse.ok(
			"?붽컙 ?뚮퉬 遺꾩꽍??議고쉶?덉뒿?덈떎.",
			getMonthlyTransactionAnalysisUseCase.execute(authenticatedMember.userId(), yearMonth)
		);
	}

	private YearMonth parseYearMonth(String value) {
		return value == null || value.isBlank() ? null : YearMonth.parse(value);
	}
}
