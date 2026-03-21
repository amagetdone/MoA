package com.ssafy.amagetdone.domain.transaction.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class WeeklyTransactionAnalysisDto {

	private WeeklyTransactionAnalysisDto() {
	}

	public record Response(
		String yearWeek,
		LocalDate weekStartDate,
		LocalDate weekEndDate,
		Long expenseAmount,
		Long essentialAmount,
		Long fixedEssentialAmount,
		Long variableEssentialAmount,
		List<CategoryExpense> categoryExpenses,
		LocalDateTime lastAnalyzedAt
	) {
		public static Response empty(String yearWeek) {
			return new Response(
				yearWeek,
				null,
				null,
				0L,
				0L,
				0L,
				0L,
				List.of(),
				null
			);
		}
	}

	public record CategoryExpense(
		Integer categoryId,
		String categoryName,
		Long plannedAmount,
		Long actualSpentAmount,
		Long remainingAmount
	) {
	}
}
