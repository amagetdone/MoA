package com.ssafy.amagetdone.domain.plan.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlanCreationCurrentWeekAllocationPreservationServiceTest {

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Test
	void snapshotsAndRestoresOnlyOtherPlansInCurrentWeek() {
		User user = user(1);
		BudgetWeek currentWeek = budgetWeek(user, 101, LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 5));
		Plan existingPlan = plan(user, 11, "existing", 1);
		Plan newPlan = plan(user, 12, "new", 2);
		Plan extraPlan = plan(user, 13, "extra", 3);

		PlanWeekAllocation existingSnapshot = allocation(existingPlan, currentWeek, 50_000L, 40_000L, 20_000L, "keep-me");
		PlanWeekAllocation excludedSnapshot = allocation(newPlan, currentWeek, 30_000L, 30_000L, null, "new-plan");
		PlanWeekAllocation mutatedExisting = allocation(existingPlan, currentWeek, 10_000L, 10_000L, null, "mutated");
		PlanWeekAllocation unexpectedOther = allocation(extraPlan, currentWeek, 70_000L, 70_000L, null, "remove-me");

		given(budgetWeekRepository.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(
			1,
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 4, 1)
		)).willReturn(Optional.of(currentWeek));
		given(planWeekAllocationRepository.findByBudgetWeek(currentWeek))
			.willReturn(List.of(existingSnapshot, excludedSnapshot), List.of(mutatedExisting, excludedSnapshot, unexpectedOther));

		PlanCreationCurrentWeekAllocationPreservationService service =
			new PlanCreationCurrentWeekAllocationPreservationService(
				budgetWeekRepository,
				planWeekAllocationRepository,
				Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneId.of("Asia/Seoul"))
			);

		List<PlanCreationCurrentWeekAllocationPreservationService.PreservedAllocation> preserved =
			service.snapshotOtherPlanAllocations(1, 12);
		service.restoreOtherPlanAllocations(1, 12, preserved);

		assertThat(preserved).hasSize(1);
		assertThat(preserved.get(0).plan().getId()).isEqualTo(11);
		assertThat(mutatedExisting.getPlannedAmount()).isEqualTo(50_000L);
		assertThat(mutatedExisting.getFinalAmount()).isEqualTo(40_000L);
		assertThat(mutatedExisting.getActualAmount()).isEqualTo(20_000L);
		assertThat(mutatedExisting.getNote()).isEqualTo("keep-me");
		verify(planWeekAllocationRepository).deleteAll(List.of(unexpectedOther));

		ArgumentCaptor<List<PlanWeekAllocation>> savedCaptor = ArgumentCaptor.forClass(List.class);
		verify(planWeekAllocationRepository).saveAll(savedCaptor.capture());
		assertThat(savedCaptor.getValue()).containsExactly(mutatedExisting);
	}

	@Test
	void doesNothingWhenCurrentWeekDoesNotExist() {
		given(budgetWeekRepository.findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualOrderByIdDesc(
			1,
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 4, 1)
		)).willReturn(Optional.empty());

		PlanCreationCurrentWeekAllocationPreservationService service =
			new PlanCreationCurrentWeekAllocationPreservationService(
				budgetWeekRepository,
				planWeekAllocationRepository,
				Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneId.of("Asia/Seoul"))
			);

		List<PlanCreationCurrentWeekAllocationPreservationService.PreservedAllocation> preserved =
			service.snapshotOtherPlanAllocations(1, 12);
		service.restoreOtherPlanAllocations(1, 12, preserved);

		assertThat(preserved).isEmpty();
		verify(planWeekAllocationRepository, never()).findByBudgetWeek(org.mockito.ArgumentMatchers.any());
		verify(planWeekAllocationRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
	}

	private User user(int userId) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}

	private BudgetWeek budgetWeek(User user, int id, LocalDate startDate, LocalDate endDate) {
		BudgetWeek week = BudgetWeek.builder()
			.user(user)
			.weekSequence(1)
			.weekStartDate(startDate)
			.weekEndDate(endDate)
			.periodDays(7)
			.partial(false)
			.status(BudgetWeekStatus.ACTIVE)
			.build();
		ReflectionTestUtils.setField(week, "id", id);
		return week;
	}

	private Plan plan(User user, int id, String title, int priority) {
		Plan plan = Plan.builder()
			.user(user)
			.title(title)
			.description(title)
			.targetAmount(300_000L)
			.savedAmount(50_000L)
			.startDate(LocalDate.of(2026, 3, 30))
			.endDate(LocalDate.of(2026, 4, 26))
			.targetWeeks(4)
			.priority(priority)
			.status(PlanStatus.ACTIVE)
			.build();
		ReflectionTestUtils.setField(plan, "id", id);
		return plan;
	}

	private PlanWeekAllocation allocation(
		Plan plan,
		BudgetWeek budgetWeek,
		long plannedAmount,
		long finalAmount,
		Long actualAmount,
		String note
	) {
		return PlanWeekAllocation.builder()
			.plan(plan)
			.budgetWeek(budgetWeek)
			.plannedAmount(plannedAmount)
			.finalAmount(finalAmount)
			.actualAmount(actualAmount)
			.note(note)
			.build();
	}
}
