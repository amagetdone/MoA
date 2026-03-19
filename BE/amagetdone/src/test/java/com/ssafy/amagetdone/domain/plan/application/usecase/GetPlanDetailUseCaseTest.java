package com.ssafy.amagetdone.domain.plan.application.usecase;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.amagetdone.domain.budget.application.usecase.SyncBudgetStateOnReadUseCase;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanDetailResponse;
import com.ssafy.amagetdone.domain.plan.application.service.PlanQueryService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPlanDetailUseCaseTest {

	@Mock
	private SyncBudgetStateOnReadUseCase syncBudgetStateOnReadUseCase;

	@Mock
	private PlanQueryService planQueryService;

	@InjectMocks
	private GetPlanDetailUseCase getPlanDetailUseCase;

	@Test
	void syncsBudgetStateBeforeLoadingPlanDetail() {
		PlanDetailResponse response = new PlanDetailResponse(
			1,
			"Trip Fund",
			"desc",
			1,
			1_000_000L,
			250_000L,
			25,
			60L,
			new PlanDetailResponse.GraphData(List.of(1), List.of(30_000L), List.of(20_000L)),
			List.of(new PlanDetailResponse.WeeklyPlan(1, 30_000L)),
			3,
			10,
			new PlanDetailResponse.CompletionProjection(5, LocalDate.of(2026, 4, 26))
		);
		when(planQueryService.getPlanDetail(1, 1)).thenReturn(response);

		PlanDetailResponse actual = getPlanDetailUseCase.execute(1, 1);

		assertSame(response, actual);
		InOrder inOrder = inOrder(syncBudgetStateOnReadUseCase, planQueryService);
		inOrder.verify(syncBudgetStateOnReadUseCase).execute(1);
		inOrder.verify(planQueryService).getPlanDetail(1, 1);
	}
}
