package com.ssafy.amagetdone.domain.transaction.application.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.YearMonth;
import java.util.List;

import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyExpenseDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.MonthlyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.TransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.api.dto.WeeklyTransactionAnalysisDto;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyExpenseGraphUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyExecutionAnalysisResultUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyExpenseUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetMonthlyTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetOnboardingAnalysisResultUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetWeeklyExpenseGraphUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.GetWeeklyTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.RunTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.model.entity.TransactionAnalysisMetadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RealTransactionAnalysisResponseFacadeTest {

	@Mock
	private GetMonthlyExpenseUseCase getMonthlyExpenseUseCase;

	@Mock
	private RunTransactionAnalysisUseCase runTransactionAnalysisUseCase;

	@Mock
	private GetOnboardingAnalysisResultUseCase getOnboardingAnalysisResultUseCase;

	@Mock
	private GetMonthlyExecutionAnalysisResultUseCase getMonthlyExecutionAnalysisResultUseCase;

	@Mock
	private GetWeeklyTransactionAnalysisUseCase getWeeklyTransactionAnalysisUseCase;

	@Mock
	private GetMonthlyTransactionAnalysisUseCase getMonthlyTransactionAnalysisUseCase;

	@Mock
	private GetWeeklyExpenseGraphUseCase getWeeklyExpenseGraphUseCase;

	@Mock
	private GetMonthlyExpenseGraphUseCase getMonthlyExpenseGraphUseCase;

	@Test
	void getWeeklyAnalysisRetriesAfterRunningMonthlyAnalysisWhenWeekHasTransactionsButNoAnalysis() {
		RealTransactionAnalysisResponseFacade facade = facade();
		WeeklyTransactionAnalysisDto.Response emptyAnalysis = new WeeklyTransactionAnalysisDto.Response(
			"2026-02-3",
			java.time.LocalDate.of(2026, 2, 16),
			java.time.LocalDate.of(2026, 2, 22),
			55_950L,
			0L,
			0L,
			0L,
			List.of(),
			null
		);
		WeeklyTransactionAnalysisDto.Response expected = new WeeklyTransactionAnalysisDto.Response(
			"2026-02-3",
			java.time.LocalDate.of(2026, 2, 16),
			java.time.LocalDate.of(2026, 2, 22),
			55_950L,
			40_000L,
			10_000L,
			30_000L,
			List.of(),
			java.time.LocalDateTime.of(2026, 3, 23, 10, 0)
		);
		given(getWeeklyTransactionAnalysisUseCase.execute(1, "2026-02-3"))
			.willReturn(emptyAnalysis, expected);
		given(runTransactionAnalysisUseCase.execute(1, YearMonth.of(2026, 2)))
			.willReturn(TransactionAnalysisMetadata.builder().build());

		WeeklyTransactionAnalysisDto.Response actual = facade.getWeeklyAnalysis(1, "2026-02-3");

		verify(getWeeklyTransactionAnalysisUseCase, times(2)).execute(1, "2026-02-3");
		verify(runTransactionAnalysisUseCase).execute(1, YearMonth.of(2026, 2));
		assertSame(expected, actual);
	}

	@Test
	void getWeeklyAnalysisDoesNotRerunAnalysisWhenWeekAlreadyHasAnalyzedData() {
		RealTransactionAnalysisResponseFacade facade = facade();
		WeeklyTransactionAnalysisDto.Response expected = new WeeklyTransactionAnalysisDto.Response(
			"2026-02-3",
			java.time.LocalDate.of(2026, 2, 16),
			java.time.LocalDate.of(2026, 2, 22),
			55_950L,
			40_000L,
			10_000L,
			30_000L,
			List.of(),
			java.time.LocalDateTime.of(2026, 3, 23, 10, 0)
		);
		given(getWeeklyTransactionAnalysisUseCase.execute(1, "2026-02-3"))
			.willReturn(expected);

		WeeklyTransactionAnalysisDto.Response actual = facade.getWeeklyAnalysis(1, "2026-02-3");

		verify(getWeeklyTransactionAnalysisUseCase).execute(1, "2026-02-3");
		verifyNoInteractions(runTransactionAnalysisUseCase);
		assertSame(expected, actual);
	}

	private RealTransactionAnalysisResponseFacade facade() {
		return new RealTransactionAnalysisResponseFacade(
			getMonthlyExpenseUseCase,
			getWeeklyExpenseGraphUseCase,
			getMonthlyExpenseGraphUseCase,
			runTransactionAnalysisUseCase,
			getOnboardingAnalysisResultUseCase,
			getMonthlyExecutionAnalysisResultUseCase,
			getWeeklyTransactionAnalysisUseCase,
			getMonthlyTransactionAnalysisUseCase
		);
	}
}
