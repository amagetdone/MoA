package com.ssafy.amagetdone.domain.transaction.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class MonthlyExpenseGraphDto {

	private MonthlyExpenseGraphDto() {
	}

	public record Response(
		String yearMonth,
		LocalDate monthStartDate,
		LocalDate monthEndDate,
		Long totalExpenseAmount,
		List<Point> points,
		LocalDateTime lastAnalyzedAt
	) {
	}

	public record Point(
		String yearWeek,
		LocalDate weekStartDate,
		LocalDate weekEndDate,
		String label,
		Long expenseAmount
	) {
	}
}
