package com.ssafy.amagetdone.domain.budget.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.application.service.BudgetActualSpentRefreshService;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetReadSyncService;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetRolloverResyncService;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionSyncService;
import com.ssafy.amagetdone.domain.transaction.application.usecase.RunTransactionAnalysisUseCase;
import com.ssafy.amagetdone.domain.transaction.application.usecase.SyncLatestTransactionsUseCase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SyncBudgetStateOnReadUseCaseTest {

	@Mock
	private TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase;

	@Mock
	private SyncLatestTransactionsUseCase syncLatestTransactionsUseCase;

	@Mock
	private RunTransactionAnalysisUseCase runTransactionAnalysisUseCase;

	@Mock
	private BudgetActualSpentRefreshService budgetActualSpentRefreshService;

	@Mock
	private BudgetRolloverResyncService budgetRolloverResyncService;

	@Mock
	private ApplyDailyGoalSavingAccrualUseCase applyDailyGoalSavingAccrualUseCase;

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private PlanRepository planRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	private SyncBudgetStateOnReadUseCase useCase;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-03-22T00:00:00Z"), ZoneId.of("Asia/Seoul"));
		BudgetReadSyncService service = new BudgetReadSyncService(
			triggerBudgetAllocationUseCase,
			syncLatestTransactionsUseCase,
			runTransactionAnalysisUseCase,
			budgetActualSpentRefreshService,
			budgetRolloverResyncService,
			applyDailyGoalSavingAccrualUseCase,
			budgetWeekRepository,
			planRepository,
			planWeekAllocationRepository,
			clock
		);
		useCase = new SyncBudgetStateOnReadUseCase(service);
	}

	@Test
	void triggersAllocationWhenCurrentWeekIsMissing() {
		Integer userId = 1;
		LocalDate today = LocalDate.of(2026, 3, 22);

		given(budgetWeekRepository.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(any(User.class), any(), any()))
			.willReturn(Optional.empty());
		given(syncLatestTransactionsUseCase.execute(userId, 3, false))
			.willReturn(new TransactionSyncService.SyncResult(0, 0));

		useCase.execute(userId);

		verify(triggerBudgetAllocationUseCase).execute(userId);
		verify(syncLatestTransactionsUseCase).execute(userId, 3, false);
		verify(runTransactionAnalysisUseCase, never()).execute(any(), any());
		verifyNoInteractions(budgetActualSpentRefreshService, budgetRolloverResyncService);
		verify(applyDailyGoalSavingAccrualUseCase).execute(userId);
	}

	@Test
	void retriggersAllocationWhenCurrentWeekIsMissingAnActivePlanAllocation() {
		Integer userId = 1;
		BudgetWeek currentWeek = budgetWeek(userId, 3, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22));
		Plan activePlan = activePlan(userId, 100);

		given(budgetWeekRepository.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(any(User.class), any(), any()))
			.willReturn(Optional.of(currentWeek));
		given(planRepository.findAllByUser_IdAndStatus(userId, PlanStatus.ACTIVE)).willReturn(List.of(activePlan));
		given(planWeekAllocationRepository.findByBudgetWeek(currentWeek)).willReturn(List.of());
		given(syncLatestTransactionsUseCase.execute(userId, 3, false))
			.willReturn(new TransactionSyncService.SyncResult(0, 0));

		useCase.execute(userId);

		verify(triggerBudgetAllocationUseCase).execute(userId);
		verify(syncLatestTransactionsUseCase).execute(userId, 3, false);
		verify(runTransactionAnalysisUseCase, never()).execute(any(), any());
		verify(applyDailyGoalSavingAccrualUseCase).execute(userId);
	}

	@Test
	void runsFullReadSyncPipelineWhenNewTransactionsWereInserted() {
		Integer userId = 1;
		LocalDate today = LocalDate.of(2026, 3, 22);
		BudgetWeek currentWeek = budgetWeek(userId, 3, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22));
		Plan activePlan = activePlan(userId, 100);
		PlanWeekAllocation allocation = PlanWeekAllocation.builder()
			.budgetWeek(currentWeek)
			.plan(activePlan)
			.plannedAmount(10_000L)
			.finalAmount(10_000L)
			.build();

		given(budgetWeekRepository.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(any(User.class), any(), any()))
			.willReturn(Optional.of(currentWeek));
		given(planRepository.findAllByUser_IdAndStatus(userId, PlanStatus.ACTIVE)).willReturn(List.of(activePlan));
		given(planWeekAllocationRepository.findByBudgetWeek(currentWeek)).willReturn(List.of(allocation));
		given(syncLatestTransactionsUseCase.execute(userId, 3, false))
			.willReturn(new TransactionSyncService.SyncResult(1, 0));

		useCase.execute(userId);

		InOrder inOrder = inOrder(
			syncLatestTransactionsUseCase,
			runTransactionAnalysisUseCase,
			triggerBudgetAllocationUseCase,
			budgetActualSpentRefreshService,
			budgetRolloverResyncService,
			applyDailyGoalSavingAccrualUseCase
		);
		inOrder.verify(syncLatestTransactionsUseCase).execute(userId, 3, false);
		inOrder.verify(runTransactionAnalysisUseCase).execute(userId, 3);
		inOrder.verify(triggerBudgetAllocationUseCase).execute(userId);
		inOrder.verify(budgetActualSpentRefreshService).refresh(userId, today);
		inOrder.verify(budgetRolloverResyncService).resync(userId);
		inOrder.verify(applyDailyGoalSavingAccrualUseCase).execute(userId);
	}

	private BudgetWeek budgetWeek(Integer userId, Integer sequence, LocalDate startDate, LocalDate endDate) {
		BudgetWeek budgetWeek = BudgetWeek.create(null, user(userId), sequence, startDate, endDate);
		budgetWeek.setStatus(BudgetWeekStatus.ACTIVE);
		return budgetWeek;
	}

	private Plan activePlan(Integer userId, Integer planId) {
		Plan plan = Plan.create(user(userId), "Trip", "desc", 300_000L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 30), 1);
		ReflectionTestUtils.setField(plan, "id", planId);
		plan.activate();
		return plan;
	}

	private User user(Integer userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}
}
