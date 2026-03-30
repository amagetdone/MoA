package com.ssafy.amagetdone.domain.budget.api.dto;

import java.time.LocalDate;

public final class BudgetDashboardDto {

	private BudgetDashboardDto() {
	}

	public record Response(
		LocalDate weekStartDate,
		LocalDate weekEndDate,
		long daysRemaining,
		long finalFreeAmount,
		long actualSpentAmount,
		long remainingAmount,
		long dailyAvailable
	) {
	}
}
