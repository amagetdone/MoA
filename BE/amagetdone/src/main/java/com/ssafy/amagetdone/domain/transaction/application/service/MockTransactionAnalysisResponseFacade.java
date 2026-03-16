package com.ssafy.amagetdone.domain.transaction.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisJobDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.TransactionAnalysisResponseFacade;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisJob;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionAnalysisJobStatus;
import com.ssafy.amagetdone.domain.transaction.model.vo.TransactionAnalysisStep;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "transaction.analysis-response-mode", havingValue = "mock")
public class MockTransactionAnalysisResponseFacade implements TransactionAnalysisResponseFacade {

	private static final String MOCK_ROOT = "mock/transaction-analysis/";

	private final ExpenseSummaryDto.Response expenseSummary;
	private final MonthlyExpenseDto.Response monthlyExpense;
	private final TransactionAnalysisJobDto.StatusResponse analysisJobStatus;
	private final WeeklyTransactionAnalysisDto.Response weeklyAnalysis;
	private final MonthlyTransactionAnalysisDto.Response monthlyAnalysis;

	public MockTransactionAnalysisResponseFacade(ObjectMapper objectMapper) {
		this.expenseSummary = read(objectMapper, "expense-summary.json", ExpenseSummaryDto.Response.class);
		this.monthlyExpense = read(objectMapper, "monthly-expense.json", MonthlyExpenseDto.Response.class);
		this.analysisJobStatus = read(objectMapper, "analysis-job-status.json", TransactionAnalysisJobDto.StatusResponse.class);
		this.weeklyAnalysis = read(objectMapper, "weekly-analysis.json", WeeklyTransactionAnalysisDto.Response.class);
		this.monthlyAnalysis = read(objectMapper, "monthly-analysis.json", MonthlyTransactionAnalysisDto.Response.class);
	}

	@Override
	public TransactionAnalysisJob createAnalysisJob(Integer userId, Integer months, boolean forceFullSync) {
		return TransactionAnalysisJob.builder()
			.id("mock-job-001")
			.user(null)
			.requestedMonths(analysisJobStatus.requestedMonths())
			.status(TransactionAnalysisJobStatus.valueOf(analysisJobStatus.status()))
			.currentStep(TransactionAnalysisStep.valueOf(analysisJobStatus.progress().step()))
			.requestedAt(analysisJobStatus.requestedAt())
			.startedAt(analysisJobStatus.startedAt())
			.finishedAt(analysisJobStatus.finishedAt())
			.errorCode(analysisJobStatus.errorCode())
			.errorMessage(analysisJobStatus.errorMessage())
			.build();
	}

	@Override
	public ExpenseSummaryDto.Response getExpenseSummary(Integer userId, YearMonth fromMonth, YearMonth toMonth,
		String bucket) {
		return expenseSummary;
	}

	@Override
	public MonthlyExpenseDto.Response getMonthlyExpense(Integer userId, YearMonth month) {
		return monthlyExpense;
	}

	@Override
	public TransactionAnalysisJobDto.StatusResponse getAnalysisJobStatus(Integer userId, String jobId) {
		return new TransactionAnalysisJobDto.StatusResponse(
			jobId,
			analysisJobStatus.status(),
			analysisJobStatus.progress(),
			analysisJobStatus.requestedMonths(),
			analysisJobStatus.requestedAt(),
			analysisJobStatus.startedAt(),
			analysisJobStatus.finishedAt(),
			analysisJobStatus.errorCode(),
			analysisJobStatus.errorMessage(),
			analysisJobStatus.onboardingResult()
		);
	}

	@Override
	public WeeklyTransactionAnalysisDto.Response getWeeklyAnalysis(Integer userId, String yearWeek) {
		return new WeeklyTransactionAnalysisDto.Response(
			yearWeek,
			weeklyAnalysis.weekStartDate(),
			weeklyAnalysis.weekEndDate(),
			weeklyAnalysis.expenseAmount(),
			weeklyAnalysis.essentialAmount(),
			weeklyAnalysis.fixedEssentialAmount(),
			weeklyAnalysis.variableEssentialAmount(),
			weeklyAnalysis.categoryExpenses(),
			weeklyAnalysis.lastAnalyzedAt()
		);
	}

	@Override
	public MonthlyTransactionAnalysisDto.Response getMonthlyAnalysis(Integer userId, String yearMonth) {
		return new MonthlyTransactionAnalysisDto.Response(
			yearMonth,
			monthlyAnalysis.expenseAmount(),
			monthlyAnalysis.monthlyEssentialAmount(),
			monthlyAnalysis.fixedEssentialAmount(),
			monthlyAnalysis.variableEssentialAmount(),
			monthlyAnalysis.categoryExpenses(),
			monthlyAnalysis.lastAnalyzedAt()
		);
	}

	private <T> T read(ObjectMapper objectMapper, String fileName, Class<T> type) {
		ClassPathResource resource = new ClassPathResource(MOCK_ROOT + fileName);
		try (InputStream inputStream = resource.getInputStream()) {
			return objectMapper.readValue(inputStream, type);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to load transaction analysis mock data: " + fileName, exception);
		}
	}
}
