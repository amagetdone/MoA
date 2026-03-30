package com.ssafy.amagetdone.domain.budget.api.dto;

public final class BudgetSummaryDto {

	private BudgetSummaryDto() {
	}

	public record Response(
		long activePlanCount,
		long completedPlanCount,
		long totalSavedAmount
	) {
	}
}
