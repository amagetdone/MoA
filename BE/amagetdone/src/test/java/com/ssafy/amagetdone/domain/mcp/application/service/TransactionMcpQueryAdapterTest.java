package com.ssafy.amagetdone.domain.mcp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionListDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyExpenseGraphDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.ListTransactionsUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.TransactionAnalysisResponseFacade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionMcpQueryAdapterTest {

	@Mock
	private TransactionAnalysisResponseFacade transactionAnalysisResponseFacade;

	@Mock
	private ListTransactionsUseCase listTransactionsUseCase;

	@InjectMocks
	private TransactionMcpQueryAdapter adapter;

	@Test
	void getMonthlyExpenseSummaryParsesYearMonthAndDelegates() {
		MonthlyExpenseDto.Response response = new MonthlyExpenseDto.Response(
			"2026-03",
			400000L,
			150000L,
			250000L,
			0L,
			0L,
			12,
			LocalDateTime.of(2026, 3, 28, 9, 0)
		);
		when(transactionAnalysisResponseFacade.getMonthlyExpense(7, YearMonth.of(2026, 3))).thenReturn(response);

		assertThat(adapter.getMonthlyExpenseSummary(7, "2026-03")).isSameAs(response);
		verify(transactionAnalysisResponseFacade).getMonthlyExpense(7, YearMonth.of(2026, 3));
	}

	@Test
	void getWeeklyExpenseGraphDelegatesToFacade() {
		WeeklyExpenseGraphDto.Response response = new WeeklyExpenseGraphDto.Response(
			"2026-W13",
			LocalDate.of(2026, 3, 23),
			LocalDate.of(2026, 3, 29),
			300000L,
			List.of(),
			LocalDateTime.of(2026, 3, 28, 9, 0)
		);
		when(transactionAnalysisResponseFacade.getWeeklyExpenseGraph(7, "2026-W13")).thenReturn(response);

		assertThat(adapter.getWeeklyExpenseGraph(7, "2026-W13")).isSameAs(response);
		verify(transactionAnalysisResponseFacade).getWeeklyExpenseGraph(7, "2026-W13");
	}

	@Test
	void getMonthlyExpenseGraphDelegatesToFacade() {
		MonthlyExpenseGraphDto.Response response = new MonthlyExpenseGraphDto.Response(
			"2026-03",
			LocalDate.of(2026, 3, 1),
			LocalDate.of(2026, 3, 31),
			900000L,
			List.of(),
			LocalDateTime.of(2026, 3, 28, 9, 0)
		);
		when(transactionAnalysisResponseFacade.getMonthlyExpenseGraph(7, "2026-03")).thenReturn(response);

		assertThat(adapter.getMonthlyExpenseGraph(7, "2026-03")).isSameAs(response);
		verify(transactionAnalysisResponseFacade).getMonthlyExpenseGraph(7, "2026-03");
	}

	@Test
	void getTransactionListBuildsControllerEquivalentQuery() {
		TransactionListDto.Response response = new TransactionListDto.Response(null, 0, 20, 1L, List.of());
		TransactionListDto.Query expectedQuery = new TransactionListDto.Query(
			LocalDate.of(2026, 3, 1),
			LocalDate.of(2026, 3, 28),
			"INCLUDED_ONLY",
			0,
			20
		);
		when(listTransactionsUseCase.execute(7, expectedQuery)).thenReturn(response);

		assertThat(adapter.getTransactionList(7, "2026-03-01", "2026-03-28", "INCLUDED_ONLY", 0, 20)).isSameAs(response);
		verify(listTransactionsUseCase).execute(7, expectedQuery);
	}

	@Test
	void getOnboardingSpendingAnalysisConvertsFacadeResultToControllerResponse() {
		TransactionAnalysisDto.OnboardingResult result = new TransactionAnalysisDto.OnboardingResult(
			"summary",
			3000000L,
			1800000L,
			1200000L,
			0.3,
			0.7,
			"insight",
			List.of(new TransactionAnalysisDto.CategoryExpense(1, "food", 200000L))
		);
		when(transactionAnalysisResponseFacade.getOnboardingAnalysis(7)).thenReturn(result);

		TransactionAnalysisDto.Response response = adapter.getOnboardingSpendingAnalysis(7);

		assertThat(response).isEqualTo(TransactionAnalysisDto.Response.from(result));
		verify(transactionAnalysisResponseFacade).getOnboardingAnalysis(7);
	}

	@Test
	void getWeeklySpendingAnalysisDelegatesToFacade() {
		WeeklyTransactionAnalysisDto.Response response = WeeklyTransactionAnalysisDto.Response.empty("2026-W13");
		when(transactionAnalysisResponseFacade.getWeeklyAnalysis(7, "2026-W13")).thenReturn(response);

		assertThat(adapter.getWeeklySpendingAnalysis(7, "2026-W13")).isSameAs(response);
		verify(transactionAnalysisResponseFacade).getWeeklyAnalysis(7, "2026-W13");
	}

	@Test
	void getMonthlySpendingAnalysisDelegatesToFacade() {
		MonthlyTransactionAnalysisDto.Response response = new MonthlyTransactionAnalysisDto.Response(
			"2026-03",
			900000L,
			350000L,
			200000L,
			150000L,
			List.of(),
			LocalDateTime.of(2026, 3, 28, 9, 0)
		);
		when(transactionAnalysisResponseFacade.getMonthlyAnalysis(7, "2026-03")).thenReturn(response);

		assertThat(adapter.getMonthlySpendingAnalysis(7, "2026-03")).isSameAs(response);
		verify(transactionAnalysisResponseFacade).getMonthlyAnalysis(7, "2026-03");
	}
}
