package com.ssafy.amagetdone.domain.transaction.application.usecase;

import java.time.YearMonth;

import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;

public interface TransactionAnalysisResponseFacade {

	TransactionAnalysisDto.OnboardingResult analyzeTransactions(Integer userId, Integer months);

	TransactionAnalysisDto.OnboardingResult analyzeMonthlyTransactions(Integer userId, YearMonth month);

	TransactionAnalysisDto.OnboardingResult getOnboardingAnalysis(Integer userId);

	MonthlyExpenseDto.Response getMonthlyExpense(Integer userId, YearMonth month);

	WeeklyExpenseGraphDto.Response getWeeklyExpenseGraph(Integer userId, String yearWeek);

	MonthlyExpenseGraphDto.Response getMonthlyExpenseGraph(Integer userId, String yearMonth);

	WeeklyTransactionAnalysisDto.Response getWeeklyAnalysis(Integer userId, String yearWeek);

	MonthlyTransactionAnalysisDto.Response getMonthlyAnalysis(Integer userId, String yearMonth);
}
