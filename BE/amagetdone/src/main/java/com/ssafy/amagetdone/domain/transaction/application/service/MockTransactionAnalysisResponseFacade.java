package com.ssafy.amagetdone.domain.transaction.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.amagetdone.domain.transaction.api.dto.ExpenseSummaryDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.TransactionAnalysisResponseFacade;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "transaction.analysis-response-mode", havingValue = "mock")
public class MockTransactionAnalysisResponseFacade implements TransactionAnalysisResponseFacade {

	private static final String MOCK_ROOT = "mock/transaction-analysis/";

	private final ExpenseSummaryDto.Response expenseSummary;
	private final MonthlyExpenseDto.Response monthlyExpense;
	private final TransactionAnalysisDto.OnboardingResult onboardingResult;
	private final WeeklyTransactionAnalysisDto.Response weeklyAnalysis;
	private final MonthlyTransactionAnalysisDto.Response monthlyAnalysis;

	public MockTransactionAnalysisResponseFacade(ObjectMapper objectMapper) {
		this.expenseSummary = read(objectMapper, "expense-summary.json", ExpenseSummaryDto.Response.class);
		this.monthlyExpense = read(objectMapper, "monthly-expense.json", MonthlyExpenseDto.Response.class);
		this.onboardingResult = read(objectMapper, "onboarding-analysis.json", TransactionAnalysisDto.OnboardingResult.class);
		this.weeklyAnalysis = read(objectMapper, "weekly-analysis.json", WeeklyTransactionAnalysisDto.Response.class);
		this.monthlyAnalysis = read(objectMapper, "monthly-analysis.json", MonthlyTransactionAnalysisDto.Response.class);
	}

	@Override
	public TransactionAnalysisDto.OnboardingResult analyzeTransactions(Integer userId, Integer months) {
		return onboardingResult;
	}

	@Override
	public TransactionAnalysisDto.OnboardingResult analyzeMonthlyTransactions(Integer userId, YearMonth month) {
		return new TransactionAnalysisDto.OnboardingResult(
			month + " 소비 분석을 완료했어요.",
			onboardingResult.averageIncome(),
			onboardingResult.averageExpense(),
			onboardingResult.remainingAmount(),
			onboardingResult.fixedExpenseRatio(),
			onboardingResult.variableExpenseRatio(),
			onboardingResult.insightMessage(),
			onboardingResult.categoryExpenses()
		);
	}

	@Override
	public TransactionAnalysisDto.OnboardingResult getOnboardingAnalysis(Integer userId) {
		return onboardingResult;
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
