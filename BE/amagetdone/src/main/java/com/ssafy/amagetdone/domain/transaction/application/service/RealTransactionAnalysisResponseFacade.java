package com.ssafy.amagetdone.domain.transaction.application.service;

import java.time.YearMonth;

import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetExpenseSummaryUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyExecutionAnalysisResultUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyExpenseUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetOnboardingAnalysisResultUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetWeeklyTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.RunTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.TransactionAnalysisResponseFacade;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
	name = "transaction.analysis-response-mode",
	havingValue = "real",
	matchIfMissing = true
)
public class RealTransactionAnalysisResponseFacade implements TransactionAnalysisResponseFacade {

	private final GetExpenseSummaryUseCase getExpenseSummaryUseCase;
	private final GetMonthlyExpenseUseCase getMonthlyExpenseUseCase;
	private final RunTransactionAnalysisUseCase runTransactionAnalysisUseCase;
	private final GetOnboardingAnalysisResultUseCase getOnboardingAnalysisResultUseCase;
	private final GetMonthlyExecutionAnalysisResultUseCase getMonthlyExecutionAnalysisResultUseCase;
	private final GetWeeklyTransactionAnalysisUseCase getWeeklyTransactionAnalysisUseCase;
	private final GetMonthlyTransactionAnalysisUseCase getMonthlyTransactionAnalysisUseCase;

	@Override
	public TransactionAnalysisDto.OnboardingResult analyzeTransactions(Integer userId, Integer months) {
		runTransactionAnalysisUseCase.execute(userId, months);
		return getOnboardingAnalysisResultUseCase.execute(userId);
	}

	@Override
	public TransactionAnalysisDto.OnboardingResult analyzeMonthlyTransactions(Integer userId, YearMonth month) {
		return getMonthlyExecutionAnalysisResultUseCase.execute(
			userId,
			runTransactionAnalysisUseCase.execute(userId, month),
			month
		);
	}

	@Override
	public TransactionAnalysisDto.OnboardingResult getOnboardingAnalysis(Integer userId) {
		return getOnboardingAnalysisResultUseCase.execute(userId);
	}

	@Override
	public ExpenseSummaryDto.Response getExpenseSummary(Integer userId, YearMonth fromMonth, YearMonth toMonth,
		String bucket) {
		return getExpenseSummaryUseCase.execute(userId, fromMonth, toMonth, bucket);
	}

	@Override
	public MonthlyExpenseDto.Response getMonthlyExpense(Integer userId, YearMonth month) {
		return getMonthlyExpenseUseCase.execute(userId, month);
	}

	@Override
	public WeeklyTransactionAnalysisDto.Response getWeeklyAnalysis(Integer userId, String yearWeek) {
		return getWeeklyTransactionAnalysisUseCase.execute(userId, yearWeek);
	}

	@Override
	public MonthlyTransactionAnalysisDto.Response getMonthlyAnalysis(Integer userId, String yearMonth) {
		return getMonthlyTransactionAnalysisUseCase.execute(userId, yearMonth);
	}
}
