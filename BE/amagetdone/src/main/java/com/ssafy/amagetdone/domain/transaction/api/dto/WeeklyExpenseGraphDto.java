package com.ssafy.amagetdone.domain.transaction.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class WeeklyExpenseGraphDto {

	private WeeklyExpenseGraphDto() {
	}

	public record Response(
		String yearWeek,
		LocalDate weekStartDate,
		LocalDate weekEndDate,
		Long totalExpenseAmount,
		List<Point> points,
		LocalDateTime lastAnalyzedAt
	) {
	}

	public record Point(
		LocalDate date,
		String label,
		Long expenseAmount
	) {
	}
}
