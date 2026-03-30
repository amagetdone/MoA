package com.ssafy.amagetdone.domain.budget.api.dto;

import java.time.LocalDate;

public record BudgetWeekSeedResponse(
	int seededCount,
	LocalDate firstWeekStartDate,
	LocalDate lastWeekEndDate
) {
}
