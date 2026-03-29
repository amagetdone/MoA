package com.ssafy.amagetdone.domain.plan.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SeedDemoPlanDataUseCaseTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PlanRepository planRepository;

	@Mock
	private BudgetWeekRepository budgetWeekRepository;

	@Mock
	private PlanWeekAllocationRepository planWeekAllocationRepository;

	@Mock
	private BudgetAdjustmentRepository budgetAdjustmentRepository;

	private SeedDemoPlanDataUseCase useCase;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-03-30T03:00:00Z"), ZoneId.of("Asia/Seoul"));
		useCase = new SeedDemoPlanDataUseCase(
			userRepository,
			planRepository,
			budgetWeekRepository,
			planWeekAllocationRepository,
			budgetAdjustmentRepository,
			clock
		);
	}

	@Test
	void rejectsWhenWeekCountDoesNotMatchPlanDuration() {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 12);
		given(userRepository.findById(12)).willReturn(Optional.of(user));

		AccountAdminDto.DemoPlanSeedRequest request = new AccountAdminDto.DemoPlanSeedRequest(
			12,
			List.of(
				new AccountAdminDto.DemoPlanSeedPlanRequest(
					"유럽 여행",
					"유럽 배낭 여행",
					1_000_000L,
					1,
					PlanStatus.ACTIVE,
					LocalDate.of(2026, 3, 9),
					LocalDate.of(2026, 4, 26),
					LocalDate.of(2026, 4, 26),
					List.of(
						new AccountAdminDto.DemoPlanSeedWeekRequest(1, 125_000L, null, 125_000L),
						new AccountAdminDto.DemoPlanSeedWeekRequest(2, 125_000L, null, 125_000L)
					)
				)
			)
		);

		assertThrows(PlanException.class, () -> useCase.execute(request));
	}

	@Test
	void seedsPlansWeeksAndAdjustmentsConsistently() {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 12);
		given(userRepository.findById(12)).willReturn(Optional.of(user));
		given(planRepository.findAllByUserIdAndDeletedFalse(12)).willReturn(List.of());
		given(budgetWeekRepository.findAllByUser_IdOrderByWeekStartDateAsc(12)).willReturn(List.of());
		given(planRepository.saveAll(any())).willAnswer(invocation -> {
			List<Plan> plans = invocation.getArgument(0);
			AtomicInteger nextId = new AtomicInteger(100);
			plans.forEach(plan -> ReflectionTestUtils.setField(plan, "id", nextId.getAndIncrement()));
			return plans;
		});
		given(budgetWeekRepository.saveAll(any())).willAnswer(invocation -> {
			List<BudgetWeek> weeks = invocation.getArgument(0);
			AtomicInteger nextId = new AtomicInteger(200);
			for (BudgetWeek week : weeks) {
				if (week.getId() == null) {
					ReflectionTestUtils.setField(week, "id", nextId.getAndIncrement());
				}
			}
			return weeks;
		});
		given(planWeekAllocationRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(budgetAdjustmentRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		AccountAdminDto.DemoPlanSeedRequest request = new AccountAdminDto.DemoPlanSeedRequest(
			12,
			List.of(
				new AccountAdminDto.DemoPlanSeedPlanRequest(
					"고구마",
					"고구마 밭 구매하기",
					1_000_000L,
					2,
					PlanStatus.ACTIVE,
					LocalDate.of(2026, 3, 9),
					LocalDate.of(2026, 4, 19),
					LocalDate.of(2026, 4, 5),
					List.of(
						new AccountAdminDto.DemoPlanSeedWeekRequest(1, 200_000L, null, 200_000L),
						new AccountAdminDto.DemoPlanSeedWeekRequest(2, 200_000L, null, 200_000L),
						new AccountAdminDto.DemoPlanSeedWeekRequest(3, 200_000L, null, 200_000L),
						new AccountAdminDto.DemoPlanSeedWeekRequest(4, 200_000L, null, 100_000L),
						new AccountAdminDto.DemoPlanSeedWeekRequest(5, 100_000L, null, 0L),
						new AccountAdminDto.DemoPlanSeedWeekRequest(6, 100_000L, null, 0L)
					)
				)
			)
		);

		AccountAdminDto.DemoPlanSeedResponse response = useCase.execute(request);

		assertEquals(1, response.seededPlanCount());
		assertEquals(6, response.seededWeekCount());
		assertEquals(700_000L, response.seededActualAmount());
		assertEquals(LocalDate.of(2026, 4, 5), response.plans().get(0).originalEndDate());
		assertEquals(LocalDate.of(2026, 4, 19), response.plans().get(0).endDate());
		assertEquals(700_000L, response.plans().get(0).savedAmount());

		ArgumentCaptor<List<PlanWeekAllocation>> allocationCaptor = ArgumentCaptor.forClass(List.class);
		verify(planWeekAllocationRepository).saveAll(allocationCaptor.capture());
		List<PlanWeekAllocation> savedAllocations = new ArrayList<>(allocationCaptor.getValue());
		assertEquals(6, savedAllocations.size());
		assertEquals(100_000L, savedAllocations.get(3).getActualAmount());
		assertNull(savedAllocations.get(4).getActualAmount());

		ArgumentCaptor<List<BudgetAdjustment>> adjustmentCaptor = ArgumentCaptor.forClass(List.class);
		verify(budgetAdjustmentRepository).saveAll(adjustmentCaptor.capture());
		List<BudgetAdjustment> savedAdjustments = new ArrayList<>(adjustmentCaptor.getValue());
		assertEquals(4, savedAdjustments.size());
		assertEquals(200_000L, savedAdjustments.get(0).getAdjustmentAmount());
	}
}
