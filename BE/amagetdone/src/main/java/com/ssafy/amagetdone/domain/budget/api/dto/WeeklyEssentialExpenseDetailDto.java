package com.ssafy.amagetdone.domain.budget.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class WeeklyEssentialExpenseDetailDto {

	private WeeklyEssentialExpenseDetailDto() {
	}

	public record Response(
		String yearWeek,
		LocalDate weekStartDate,
		LocalDate weekEndDate,
		Long plannedEssentialAmount,
		Long actualSpentAmount,
		Long remainingAmount,
		List<CategoryDetail> fixedExpenseCategories,
		List<CategoryDetail> variableExpenseCategories
	) {
	}

	public record CategoryDetail(
		Integer categoryId,
		String categoryName,
		Long plannedAmount,
		Long actualSpentAmount,
		Long remainingAmount,
		List<TransactionDetail> transactions
	) {
	}

	public record TransactionDetail(
		Integer transactionId,
		String sourceType,
		String sourceId,
		LocalDateTime occurredAt,
		String merchantText,
		Long amount,
		CategoryView category,
		String expenseType
	) {
	}

	public record CategoryView(
		Integer id,
		String name
	) {
	}
}
