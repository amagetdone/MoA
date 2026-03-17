package com.ssafy.amagetdone.domain.transaction.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class ExpenseSummaryDto {

	private ExpenseSummaryDto() {
	}

	public record Response(
		Range range,
		List<Point> points,
		Comparison comparison,
		LocalDateTime lastAnalyzedAt
	) {
	}

	public record Range(
		String fromMonth,
		String toMonth,
		String bucket
	) {
	}

	public record Point(
		String label,
		Long expenseAmount,
		Long fixedExpenseAmount,
		Long variableExpenseAmount
	) {
	}

	public record Comparison(
		Long latestAmount,
		Long previousAmount,
		Long changeAmount,
		Double changeRate
	) {
	}
}
