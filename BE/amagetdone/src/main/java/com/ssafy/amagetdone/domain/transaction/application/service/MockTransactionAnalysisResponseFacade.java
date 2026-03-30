package com.ssafy.amagetdone.domain.transaction.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.TransactionAnalysisResponseFacade;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "transaction.analysis-response-mode", havingValue = "mock")
public class MockTransactionAnalysisResponseFacade implements TransactionAnalysisResponseFacade {

	private static final String MOCK_ROOT = "mock/transaction-analysis/";

	private final MonthlyExpenseDto.Response monthlyExpense;
	private final TransactionAnalysisDto.OnboardingResult onboardingResult;
	private final WeeklyTransactionAnalysisDto.Response weeklyAnalysis;
	private final MonthlyTransactionAnalysisDto.Response monthlyAnalysis;

	public MockTransactionAnalysisResponseFacade(ObjectMapper objectMapper) {
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
	public MonthlyExpenseDto.Response getMonthlyExpense(Integer userId, YearMonth month) {
		return monthlyExpense;
	}

	@Override
	public WeeklyExpenseGraphDto.Response getWeeklyExpenseGraph(Integer userId, String yearWeek) {
		LocalDate startDate = weeklyAnalysis.weekStartDate();
		List<WeeklyExpenseGraphDto.Point> points = startDate == null
			? List.of()
			: List.of(
				new WeeklyExpenseGraphDto.Point(startDate, "월", 50_000L),
				new WeeklyExpenseGraphDto.Point(startDate.plusDays(1L), "화", 30_000L),
				new WeeklyExpenseGraphDto.Point(startDate.plusDays(2L), "수", 20_000L),
				new WeeklyExpenseGraphDto.Point(startDate.plusDays(3L), "목", 0L),
				new WeeklyExpenseGraphDto.Point(startDate.plusDays(4L), "금", 0L),
				new WeeklyExpenseGraphDto.Point(startDate.plusDays(5L), "토", 0L),
				new WeeklyExpenseGraphDto.Point(startDate.plusDays(6L), "일", 0L)
			);
		long totalExpenseAmount = points.stream().mapToLong(WeeklyExpenseGraphDto.Point::expenseAmount).sum();
		return new WeeklyExpenseGraphDto.Response(
			yearWeek,
			startDate,
			weeklyAnalysis.weekEndDate(),
			totalExpenseAmount,
			points,
			weeklyAnalysis.lastAnalyzedAt()
		);
	}

	@Override
	public MonthlyExpenseGraphDto.Response getMonthlyExpenseGraph(Integer userId, String yearMonth) {
		YearMonth targetMonth = YearMonth.parse(yearMonth);
		LocalDate monthStartDate = targetMonth.atDay(1);
		LocalDate firstWeekStartDate = monthStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		List<MonthlyExpenseGraphDto.Point> points = List.of(
			new MonthlyExpenseGraphDto.Point(
				yearMonth + "-1",
				firstWeekStartDate,
				firstWeekStartDate.plusDays(6L),
				targetMonth.getMonthValue() + "월 1주차",
				120_000L
			),
			new MonthlyExpenseGraphDto.Point(
				yearMonth + "-2",
				firstWeekStartDate.plusWeeks(1L),
				firstWeekStartDate.plusWeeks(1L).plusDays(6L),
				targetMonth.getMonthValue() + "월 2주차",
				180_000L
			)
		);
		long totalExpenseAmount = points.stream().mapToLong(MonthlyExpenseGraphDto.Point::expenseAmount).sum();
		return new MonthlyExpenseGraphDto.Response(
			yearMonth,
			monthStartDate,
			targetMonth.atEndOfMonth(),
			totalExpenseAmount,
			points,
			monthlyAnalysis.lastAnalyzedAt()
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
