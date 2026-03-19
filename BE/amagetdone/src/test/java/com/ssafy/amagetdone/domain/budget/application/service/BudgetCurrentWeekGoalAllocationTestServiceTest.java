package com.ssafy.amagetdone.domain.budget.application.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.application.usecase.TriggerBudgetAllocationUseCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetCurrentWeekGoalAllocationTestServiceTest {

	@Mock
	private TriggerBudgetAllocationUseCase triggerBudgetAllocationUseCase;

	@Mock
	private WeeklySavingsDetailQueryService weeklySavingsDetailQueryService;

	@Test
	void forceCurrentWeekAllocationRunsAllocationThenReturnsCurrentWeekDetail() {
		Clock clock = Clock.fixed(Instant.parse("2026-03-19T00:00:00Z"), ZoneId.of("Asia/Seoul"));
		BudgetCurrentWeekGoalAllocationTestService service = new BudgetCurrentWeekGoalAllocationTestService(
			triggerBudgetAllocationUseCase,
			weeklySavingsDetailQueryService,
			clock
		);
		WeeklySavingsDetailDto.Response response = WeeklySavingsDetailDto.Response.of(
			"2026-W12",
			LocalDate.of(2026, 3, 16),
			LocalDate.of(2026, 3, 22),
			100000L,
			80000L,
			java.util.List.of()
		);
		given(weeklySavingsDetailQueryService.getDetail(1, "2026-W12")).willReturn(response);

		WeeklySavingsDetailDto.Response actual = service.forceCurrentWeekAllocation(1);

		assertSame(response, actual);
		InOrder inOrder = inOrder(triggerBudgetAllocationUseCase, weeklySavingsDetailQueryService);
		inOrder.verify(triggerBudgetAllocationUseCase).execute(1);
		inOrder.verify(weeklySavingsDetailQueryService).getDetail(1, "2026-W12");
	}
}
