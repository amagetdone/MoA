package com.ssafy.amagetdone.domain.budget.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.budget.application.usecase.AllocateBudgetUseCase;
import com.ssafy.amagetdone.domain.budget.application.usecase.RefreshEssentialExpenseMonthFromAnalysisUseCase;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetMonthStatus;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BudgetAllocationTriggerServiceTest {

	@Mock
	private AllocateBudgetUseCase allocateBudgetUseCase;

	@Mock
	private RefreshEssentialExpenseMonthFromAnalysisUseCase refreshEssentialExpenseMonthFromAnalysisUseCase;

	@Mock
	private BudgetMonthRepository budgetMonthRepository;

	@Mock
	private EssentialExpenseMonthRepository essentialExpenseMonthRepository;

	@Mock
	private PlanRepository planRepository;

	@Test
	void triggersRefreshBeforeRunningAllocationPipeline() {
		Clock clock = Clock.fixed(Instant.parse("2026-03-19T00:00:00Z"), ZoneId.of("Asia/Seoul"));
		BudgetAllocationTriggerService service = new BudgetAllocationTriggerService(
			allocateBudgetUseCase,
			refreshEssentialExpenseMonthFromAnalysisUseCase,
			budgetMonthRepository,
			essentialExpenseMonthRepository,
			planRepository,
			clock
		);
		Integer userId = 1;
		LocalDate baseMonth = LocalDate.of(2026, 3, 1);
		User user = user(userId);
		BudgetMonth budgetMonth = BudgetMonth.builder()
			.user(user)
			.baseDate(baseMonth)
			.status(BudgetMonthStatus.OPEN)
			.totalIncomeAmount(3_200_000L)
			.build();
		EssentialExpenseMonth essential = EssentialExpenseMonth.builder()
			.id(1)
			.budgetMonth(budgetMonth)
			.category(Category.create(10, "주거비"))
			.expenseScope(ExpenseScope.FIXED.name())
			.amount(300_000L)
			.build();
		Plan activePlan = Plan.create(user, "Travel", "desc", 500_000L, baseMonth, baseMonth.plusMonths(1), 1);
		activePlan.activate();

		given(budgetMonthRepository.findByUser_IdAndBaseDate(userId, baseMonth)).willReturn(Optional.of(budgetMonth));
		given(essentialExpenseMonthRepository.findAllByBudgetMonth_User_IdOrderByBudgetMonth_BaseDateAscIdAsc(userId))
			.willReturn(List.of(essential));
		given(planRepository.findAllByUser_IdAndStatusAndDeletedFalse(userId, PlanStatus.ACTIVE)).willReturn(List.of(activePlan));

		service.trigger(userId);

		InOrder inOrder = inOrder(
			refreshEssentialExpenseMonthFromAnalysisUseCase,
			budgetMonthRepository,
			essentialExpenseMonthRepository,
			planRepository,
			allocateBudgetUseCase
		);
		inOrder.verify(refreshEssentialExpenseMonthFromAnalysisUseCase).execute(userId, baseMonth);
		inOrder.verify(budgetMonthRepository).findByUser_IdAndBaseDate(userId, baseMonth);
		inOrder.verify(essentialExpenseMonthRepository).findAllByBudgetMonth_User_IdOrderByBudgetMonth_BaseDateAscIdAsc(userId);
		inOrder.verify(planRepository).findAllByUser_IdAndStatus(userId, PlanStatus.ACTIVE);
		inOrder.verify(allocateBudgetUseCase).execute(
			any(User.class),
			eq(baseMonth),
			eq(3_200_000L),
			eq(List.of(essential)),
			eq(List.of(activePlan)),
			eq(baseMonth.withDayOfMonth(baseMonth.lengthOfMonth()))
		);
	}

	private User user(int id) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
