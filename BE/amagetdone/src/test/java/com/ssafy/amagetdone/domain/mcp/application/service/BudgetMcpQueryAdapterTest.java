package com.ssafy.amagetdone.domain.mcp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetDashboardDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetHomeDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetIncomeDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetSummaryDto;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSetupDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDetailDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyFreeSpendingDetailDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetDashboardUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetHomeUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetIncomeUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetSummaryUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetBudgetWeekSetupUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyEssentialExpenseDetailUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyEssentialExpensesUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklyFreeSpendingDetailUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.GetWeeklySavingsDetailUseCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetMcpQueryAdapterTest {

	@Mock
	private GetBudgetIncomeUseCase getBudgetIncomeUseCase;

	@Mock
	private GetBudgetDashboardUseCase getBudgetDashboardUseCase;

	@Mock
	private GetBudgetHomeUseCase getBudgetHomeUseCase;

	@Mock
	private GetBudgetSummaryUseCase getBudgetSummaryUseCase;

	@Mock
	private GetBudgetWeekSetupUseCase getBudgetWeekSetupUseCase;

	@Mock
	private GetWeeklySavingsDetailUseCase getWeeklySavingsDetailUseCase;

	@Mock
	private GetWeeklyFreeSpendingDetailUseCase getWeeklyFreeSpendingDetailUseCase;

	@Mock
	private GetWeeklyEssentialExpensesUseCase getWeeklyEssentialExpensesUseCase;

	@Mock
	private GetWeeklyEssentialExpenseDetailUseCase getWeeklyEssentialExpenseDetailUseCase;

	@InjectMocks
	private BudgetMcpQueryAdapter adapter;

	@Test
	void getBudgetIncomeDelegatesToUseCase() {
		BudgetIncomeDto.Response response = new BudgetIncomeDto.Response(2500000L, LocalDate.of(2026, 3, 1));
		when(getBudgetIncomeUseCase.execute(7)).thenReturn(response);

		assertThat(adapter.getBudgetIncome(7)).isSameAs(response);
		verify(getBudgetIncomeUseCase).execute(7);
	}

	@Test
	void getBudgetDashboardDelegatesToUseCase() {
		BudgetDashboardDto.Response response = new BudgetDashboardDto.Response(
			LocalDate.of(2026, 3, 23),
			LocalDate.of(2026, 3, 29),
			2L,
			400000L,
			120000L,
			280000L,
			140000L
		);
		when(getBudgetDashboardUseCase.execute(7)).thenReturn(response);

		assertThat(adapter.getBudgetDashboard(7)).isSameAs(response);
		verify(getBudgetDashboardUseCase).execute(7);
	}

	@Test
	void getBudgetHomeDelegatesToUseCase() {
		BudgetHomeDto.Response response = BudgetHomeDto.Response.of(
			BudgetHomeDto.FreeSpending.of(10000L, 50000L, 40000L, 4L, 10000L),
			BudgetHomeDto.WeeklySaving.of(30000L, 200000L, 15L),
			BudgetHomeDto.AssetSummary.of(1200000L),
			List.of()
		);
		when(getBudgetHomeUseCase.execute(7)).thenReturn(response);

		assertThat(adapter.getBudgetHome(7)).isSameAs(response);
		verify(getBudgetHomeUseCase).execute(7);
	}

	@Test
	void getBudgetSummaryDelegatesToUseCase() {
		BudgetSummaryDto.Response response = new BudgetSummaryDto.Response(2L, 1L, 330000L);
		when(getBudgetSummaryUseCase.execute(7)).thenReturn(response);

		assertThat(adapter.getBudgetSummary(7)).isSameAs(response);
		verify(getBudgetSummaryUseCase).execute(7);
	}

	@Test
	void getBudgetWeekSetupDelegatesToUseCase() {
		BudgetWeekSetupDto.Response response = BudgetWeekSetupDto.Response.of(
			1000000L,
			300000L,
			200000L,
			450000L,
			50000L,
			List.of(),
			List.of()
		);
		when(getBudgetWeekSetupUseCase.execute(7, "2026-W13")).thenReturn(response);

		assertThat(adapter.getBudgetWeekSetup(7, "2026-W13")).isSameAs(response);
		verify(getBudgetWeekSetupUseCase).execute(7, "2026-W13");
	}

	@Test
	void getWeeklySavingsDetailDelegatesToUseCase() {
		WeeklySavingsDetailDto.Response response = WeeklySavingsDetailDto.Response.of(
			"2026-W13",
			LocalDate.of(2026, 3, 23),
			LocalDate.of(2026, 3, 29),
			200000L,
			180000L,
			List.of()
		);
		when(getWeeklySavingsDetailUseCase.execute(7, "2026-W13")).thenReturn(response);

		assertThat(adapter.getWeeklySavingsDetail(7, "2026-W13")).isSameAs(response);
		verify(getWeeklySavingsDetailUseCase).execute(7, "2026-W13");
	}

	@Test
	void getWeeklyFreeSpendingDetailDelegatesToUseCase() {
		WeeklyFreeSpendingDetailDto.Response response = new WeeklyFreeSpendingDetailDto.Response(
			"2026-W13",
			LocalDate.of(2026, 3, 23),
			LocalDate.of(2026, 3, 29),
			300000L,
			150000L,
			150000L,
			List.of()
		);
		when(getWeeklyFreeSpendingDetailUseCase.execute(7, "2026-W13")).thenReturn(response);

		assertThat(adapter.getWeeklyFreeSpendingDetail(7, "2026-W13")).isSameAs(response);
		verify(getWeeklyFreeSpendingDetailUseCase).execute(7, "2026-W13");
	}

	@Test
	void getWeeklyEssentialExpensesDelegatesToUseCase() {
		WeeklyEssentialExpenseDto.Response response = new WeeklyEssentialExpenseDto.Response(
			"2026-W13",
			LocalDate.of(2026, 3, 23),
			LocalDate.of(2026, 3, 29),
			3,
			220000L,
			140000L,
			80000L,
			List.of(),
			List.of(),
			null
		);
		when(getWeeklyEssentialExpensesUseCase.execute(7, "2026-W13")).thenReturn(response);

		assertThat(adapter.getWeeklyEssentialExpenses(7, "2026-W13")).isSameAs(response);
		verify(getWeeklyEssentialExpensesUseCase).execute(7, "2026-W13");
	}

	@Test
	void getWeeklyEssentialExpenseDetailDelegatesToUseCase() {
		WeeklyEssentialExpenseDetailDto.Response response = new WeeklyEssentialExpenseDetailDto.Response(
			"2026-W13",
			LocalDate.of(2026, 3, 23),
			LocalDate.of(2026, 3, 29),
			220000L,
			100000L,
			120000L,
			List.of(),
			List.of()
		);
		when(getWeeklyEssentialExpenseDetailUseCase.execute(7, "2026-W13")).thenReturn(response);

		assertThat(adapter.getWeeklyEssentialExpenseDetail(7, "2026-W13")).isSameAs(response);
		verify(getWeeklyEssentialExpenseDetailUseCase).execute(7, "2026-W13");
	}
}
