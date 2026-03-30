package com.ssafy.amagetdone.domain.budget.application.usecase;

import static org.mockito.Mockito.verify;

import com.ssafy.amagetdone.domain.budget.application.usecase.SyncBudgetStateOnReadUseCase;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetDashboardQueryService;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetHomeQueryService;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetWeekSetupQueryService;
import com.ssafy.amagetdone.domain.budget.application.service.WeeklyEssentialExpenseDetailQueryService;
import com.ssafy.amagetdone.domain.budget.application.service.WeeklyFreeSpendingDetailQueryService;
import com.ssafy.amagetdone.domain.budget.application.service.WeeklySavingsDetailQueryService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReadBudgetUseCaseSyncHookTest {

	@Mock
	private SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase;

	@Mock
	private BudgetHomeQueryService budgetHomeQueryService;

	@Mock
	private BudgetDashboardQueryService budgetDashboardQueryService;

	@Mock
	private BudgetWeekSetupQueryService budgetWeekSetupQueryService;

	@Mock
	private WeeklyFreeSpendingDetailQueryService weeklyFreeSpendingDetailQueryService;

	@Mock
	private WeeklyEssentialExpenseDetailQueryService weeklyEssentialExpenseDetailQueryService;

	@Mock
	private WeeklySavingsDetailQueryService weeklySavingsDetailQueryService;

	@Test
	void homeUseCaseInvokesSyncBeforeQuery() {
		new GetBudgetHomeUseCase(syncBudgetStateOnReadUseCase, budgetHomeQueryService).execute(1);
		InOrder inOrder = org.mockito.Mockito.inOrder(syncBudgetStateOnReadUseCase, budgetHomeQueryService);
		inOrder.verify(syncBudgetStateOnReadUseCase).execute(1);
		inOrder.verify(budgetHomeQueryService).getHome(1);
		verify(budgetHomeQueryService).getHome(1);
	}

	@Test
	void dashboardUseCaseInvokesSyncBeforeQuery() {
		new GetBudgetDashboardUseCase(syncBudgetStateOnReadUseCase, budgetDashboardQueryService).execute(1);
		InOrder inOrder = org.mockito.Mockito.inOrder(syncBudgetStateOnReadUseCase, budgetDashboardQueryService);
		inOrder.verify(syncBudgetStateOnReadUseCase).execute(1);
		inOrder.verify(budgetDashboardQueryService).getDashboard(1);
		verify(budgetDashboardQueryService).getDashboard(1);
	}

	@Test
	void weekSetupUseCaseInvokesSyncBeforeQuery() {
		new GetBudgetWeekSetupUseCase(syncBudgetStateOnReadUseCase, budgetWeekSetupQueryService).execute(1, "2026-W12");
		InOrder inOrder = org.mockito.Mockito.inOrder(syncBudgetStateOnReadUseCase, budgetWeekSetupQueryService);
		inOrder.verify(syncBudgetStateOnReadUseCase).execute(1);
		inOrder.verify(budgetWeekSetupQueryService).getSetup(1, "2026-W12");
		verify(budgetWeekSetupQueryService).getSetup(1, "2026-W12");
	}

	@Test
	void freeSpendingDetailUseCaseInvokesSyncBeforeQuery() {
		new GetWeeklyFreeSpendingDetailUseCase(syncBudgetStateOnReadUseCase, weeklyFreeSpendingDetailQueryService).execute(1, "2026-W12");
		InOrder inOrder = org.mockito.Mockito.inOrder(syncBudgetStateOnReadUseCase, weeklyFreeSpendingDetailQueryService);
		inOrder.verify(syncBudgetStateOnReadUseCase).execute(1);
		inOrder.verify(weeklyFreeSpendingDetailQueryService).getDetail(1, "2026-W12");
		verify(weeklyFreeSpendingDetailQueryService).getDetail(1, "2026-W12");
	}

	@Test
	void essentialExpenseDetailUseCaseInvokesSyncBeforeQuery() {
		new GetWeeklyEssentialExpenseDetailUseCase(syncBudgetStateOnReadUseCase, weeklyEssentialExpenseDetailQueryService).execute(1, "2026-W12");
		InOrder inOrder = org.mockito.Mockito.inOrder(syncBudgetStateOnReadUseCase, weeklyEssentialExpenseDetailQueryService);
		inOrder.verify(syncBudgetStateOnReadUseCase).execute(1);
		inOrder.verify(weeklyEssentialExpenseDetailQueryService).getDetail(1, "2026-W12");
		verify(weeklyEssentialExpenseDetailQueryService).getDetail(1, "2026-W12");
	}

	@Test
	void savingsDetailUseCaseInvokesSyncBeforeQuery() {
		new GetWeeklySavingsDetailUseCase(syncBudgetStateOnReadUseCase, weeklySavingsDetailQueryService).execute(1, "2026-W12");
		InOrder inOrder = org.mockito.Mockito.inOrder(syncBudgetStateOnReadUseCase, weeklySavingsDetailQueryService);
		inOrder.verify(syncBudgetStateOnReadUseCase).execute(1);
		inOrder.verify(weeklySavingsDetailQueryService).getDetail(1, "2026-W12");
		verify(weeklySavingsDetailQueryService).getDetail(1, "2026-W12");
	}
}
