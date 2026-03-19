package com.ssafy.amagetdone.domain.budget.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetAdjustmentDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BudgetAdjustmentCommandServiceTest {

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Mock
	private BudgetAdjustmentRepository budgetAdjustmentRepository;

	@Mock
	private EntityManager entityManager;

	private BudgetAdjustmentCommandService service;

	@BeforeEach
	void setUp() {
		service = new BudgetAdjustmentCommandService(
			budgetWeekRepository,
			planWeekAllocationRepository,
			budgetAdjustmentRepository,
			entityManager
		);
	}

	@Test
	void saveAdjustmentTransfersFreeBudgetToPlanSavedAmount() {
		User user = user(1);
		Plan plan = plan(user, 11, 100_000L);
		BudgetWeek budgetWeek = budgetWeek(user, 1, 120_000L, 20_000L);
		PlanWeekAllocation allocation = allocation(plan, budgetWeek, 250_000L, 230_000L);
		BudgetAdjustmentDto.Request request = new BudgetAdjustmentDto.Request(
			BudgetAdjustmentType.PLAN_SAVED_ADD,
			15_000L,
			11,
			"자유비를 목표 저축으로 이동"
		);

		given(entityManager.getReference(User.class, 1)).willReturn(user);
		given(budgetWeekRepository.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(user,
			LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2)))
			.willReturn(Optional.of(budgetWeek));
		given(planWeekAllocationRepository.findByBudgetWeek(budgetWeek)).willReturn(List.of(allocation));
		given(budgetAdjustmentRepository.save(any(BudgetAdjustment.class))).willAnswer(invocation -> invocation.getArgument(0));

		BudgetAdjustmentDto.Response response = service.saveAdjustment(1, "2026-W10", request);

		assertThat(plan.getSavedAmount()).isEqualTo(115_000L);
		assertThat(budgetWeek.getFinalFreeAmount()).isEqualTo(105_000L);
		assertThat(response.adjustmentType()).isEqualTo("PLAN_SAVED_ADD");
		assertThat(response.planId()).isEqualTo(11);
		assertThat(response.planFinalAmount()).isEqualTo(230_000L);
		assertThat(response.finalFreeAmount()).isEqualTo(105_000L);

		ArgumentCaptor<BudgetAdjustment> adjustmentCaptor = ArgumentCaptor.forClass(BudgetAdjustment.class);
		verify(budgetAdjustmentRepository).save(adjustmentCaptor.capture());
		assertThat(adjustmentCaptor.getValue().getPlan()).isSameAs(plan);
		assertThat(adjustmentCaptor.getValue().getAdjustmentType()).isEqualTo(BudgetAdjustmentType.PLAN_SAVED_ADD);
		verify(budgetWeekRepository).save(budgetWeek);
	}

	@Test
	void saveAdjustmentRejectsWhenRequestedAmountExceedsRemainingFreeBudget() {
		User user = user(1);
		Plan plan = plan(user, 11, 100_000L);
		BudgetWeek budgetWeek = budgetWeek(user, 1, 30_000L, 20_000L);
		PlanWeekAllocation allocation = allocation(plan, budgetWeek, 250_000L, 230_000L);
		BudgetAdjustmentDto.Request request = new BudgetAdjustmentDto.Request(
			BudgetAdjustmentType.PLAN_SAVED_ADD,
			15_000L,
			11,
			"자유비를 목표 저축으로 이동"
		);

		given(entityManager.getReference(User.class, 1)).willReturn(user);
		given(budgetWeekRepository.findByUserAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqual(user,
			LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2)))
			.willReturn(Optional.of(budgetWeek));
		given(planWeekAllocationRepository.findByBudgetWeek(budgetWeek)).willReturn(List.of(allocation));

		assertThatThrownBy(() -> service.saveAdjustment(1, "2026-W10", request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Adjustment amount exceeds remaining free budget");
	}

	private User user(int userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}

	private Plan plan(User user, int planId, long savedAmount) {
		Plan plan = Plan.builder()
			.user(user)
			.title("Travel Fund")
			.targetAmount(1_000_000L)
			.savedAmount(savedAmount)
			.startDate(LocalDate.of(2026, 1, 1))
			.endDate(LocalDate.of(2026, 12, 31))
			.targetWeeks(52)
			.priority(1)
			.build();
		ReflectionTestUtils.setField(plan, "id", planId);
		return plan;
	}

	private BudgetWeek budgetWeek(User user, int weekSequence, long finalFreeAmount, long actualSpentAmount) {
		BudgetWeek budgetWeek = BudgetWeek.create(
			null,
			user,
			weekSequence,
			LocalDate.of(2026, 3, 2),
			LocalDate.of(2026, 3, 8)
		);
		budgetWeek.setFinalFreeAmount(finalFreeAmount);
		budgetWeek.setActualSpentAmount(actualSpentAmount);
		return budgetWeek;
	}

	private PlanWeekAllocation allocation(Plan plan, BudgetWeek budgetWeek, long plannedAmount, long finalAmount) {
		return PlanWeekAllocation.builder()
			.plan(plan)
			.budgetWeek(budgetWeek)
			.plannedAmount(plannedAmount)
			.finalAmount(finalAmount)
			.build();
	}
}
