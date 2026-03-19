package com.ssafy.amagetdone.domain.budget.api.dto;

import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;

import io.swagger.v3.oas.annotations.media.Schema;

public final class BudgetAdjustmentDto {

	private BudgetAdjustmentDto() {
	}

	@Schema(name = "BudgetAdjustmentRequest")
	public record Request(
		BudgetAdjustmentType type,
		Long amount,
		Integer planId,
		String reason
	) {
	}

	@Schema(name = "BudgetAdjustmentResponse")
	public record Response(
		String yearWeek,
		String adjustmentType,
		Long adjustmentAmount,
		Integer planId,
		String reason,
		Long manualAdjustmentAmount,
		Long planFinalAmount,
		Long finalFreeAmount,
		String budgetWeekStatus
	) {
	}
}
