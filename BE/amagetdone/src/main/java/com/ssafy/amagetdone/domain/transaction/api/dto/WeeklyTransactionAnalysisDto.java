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
	}

	public record CategoryExpense(
		Integer categoryId,
		String categoryName,
		Long amount
	) {
	}
}
