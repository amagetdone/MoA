package com.ssafy.amagetdone.domain.transaction.api.controller;

import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetWeeklyTransactionAnalysisUseCase;
import com.ssafy.amagetdone.global.response.ApiResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions/analysis")
public class TransactionAnalysisReadController {

	private final JwtAccessTokenReader jwtAccessTokenReader;
	private final GetWeeklyTransactionAnalysisUseCase getWeeklyTransactionAnalysisUseCase;
	private final GetMonthlyTransactionAnalysisUseCase getMonthlyTransactionAnalysisUseCase;

	public TransactionAnalysisReadController(
		JwtAccessTokenReader jwtAccessTokenReader,
		GetWeeklyTransactionAnalysisUseCase getWeeklyTransactionAnalysisUseCase,
		GetMonthlyTransactionAnalysisUseCase getMonthlyTransactionAnalysisUseCase
	) {
		this.jwtAccessTokenReader = jwtAccessTokenReader;
		this.getWeeklyTransactionAnalysisUseCase = getWeeklyTransactionAnalysisUseCase;
		this.getMonthlyTransactionAnalysisUseCase = getMonthlyTransactionAnalysisUseCase;
	}

	@GetMapping("/weeks/{yearWeek}")
	public ApiResponse<WeeklyTransactionAnalysisDto.Response> getWeeklyAnalysis(
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@PathVariable("yearWeek") String yearWeek
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok(
			"주간 소비 분석을 조회했습니다.",
			getWeeklyTransactionAnalysisUseCase.execute(userId, yearWeek)
		);
	}

	@GetMapping("/months/{yearMonth}")
	public ApiResponse<MonthlyTransactionAnalysisDto.Response> getMonthlyAnalysis(
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@PathVariable("yearMonth") String yearMonth
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok(
			"월간 소비 분석을 조회했습니다.",
			getMonthlyTransactionAnalysisUseCase.execute(userId, yearMonth)
		);
	}
}
