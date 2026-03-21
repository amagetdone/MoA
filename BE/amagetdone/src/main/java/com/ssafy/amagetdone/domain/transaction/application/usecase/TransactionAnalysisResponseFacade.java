package com.ssafy.amagetdone.domain.transaction.application.usecase;

import java.time.YearMonth;

import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;

public interface TransactionAnalysisResponseFacade {

	TransactionAnalysisDto.OnboardingResult analyzeTransactions(Integer userId, Integer months);

	TransactionAnalysisDto.OnboardingResult analyzeMonthlyTransactions(Integer userId, YearMonth month);

	TransactionAnalysisDto.OnboardingResult getOnboardingAnalysis(Integer userId);

	ExpenseSummaryDto.Response getExpenseSummary(Integer userId, YearMonth fromMonth, YearMonth toMonth, String bucket);

	MonthlyExpenseDto.Response getMonthlyExpense(Integer userId, YearMonth month);

	WeeklyTransactionAnalysisDto.Response getWeeklyAnalysis(Integer userId, String yearWeek);

	MonthlyTransactionAnalysisDto.Response getMonthlyAnalysis(Integer userId, String yearMonth);
}
