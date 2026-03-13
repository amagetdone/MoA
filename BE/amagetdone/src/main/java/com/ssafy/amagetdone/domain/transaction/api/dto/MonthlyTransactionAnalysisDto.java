package com.ssafy.amagetdone.domain.transaction.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class MonthlyTransactionAnalysisDto {

	private MonthlyTransactionAnalysisDto() {
	}

	public record Response(
		String yearMonth,
		Long expenseAmount,
		Long monthlyEssentialAmount,
		Long fixedEssentialAmount,
		Long variableEssentialAmount,
		List<CategoryExpense> categoryExpenses,
		LocalDateTime lastAnalyzedAt
	) {
	}

	public record CategoryExpense(
		Integer categoryId,
		String categoryName,
		Long amount,
		Double ratio
	) {
	}
}
