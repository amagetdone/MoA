package com.ssafy.amagetdone.domain.budget.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class WeeklyEssentialExpenseDto {

	private WeeklyEssentialExpenseDto() {
	}

	public record Response(
		String yearWeek,
		LocalDate weekStartDate,
		LocalDate weekEndDate,
		Integer baseMonths,
		Long essentialAmount,
		Long fixedEssentialAmount,
		Long variableEssentialAmount,
		List<CategoryExpense> fixedExpenseCategories,
		List<CategoryExpense> variableExpenseCategories,
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
