package com.ssafy.amagetdone.domain.transaction.application.usecase;

import java.time.YearMonth;

import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisJobDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisJob;

public interface TransactionAnalysisResponseFacade {

	TransactionAnalysisJob createAnalysisJob(Integer userId, Integer months, boolean forceFullSync);

	ExpenseSummaryDto.Response getExpenseSummary(Integer userId, YearMonth fromMonth, YearMonth toMonth, String bucket);

	MonthlyExpenseDto.Response getMonthlyExpense(Integer userId, YearMonth month);

	TransactionAnalysisJobDto.StatusResponse getAnalysisJobStatus(Integer userId, String jobId);

	WeeklyTransactionAnalysisDto.Response getWeeklyAnalysis(Integer userId, String yearWeek);

	MonthlyTransactionAnalysisDto.Response getMonthlyAnalysis(Integer userId, String yearMonth);
}
