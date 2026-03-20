package com.ssafy.amagetdone.domain.transaction.api.dto;

import java.util.List;

public final class TransactionAnalysisDto {

	private TransactionAnalysisDto() {
	}

	public record Request(
		Integer months
	) {
	}

	public record Response(
		String summaryText,
		Long averageIncome,
		Long averageExpense,
		Long remainingAmount,
		Double fixedExpenseRatio,
		Double variableExpenseRatio,
		String insightMessage,
		List<CategoryExpense> categoryExpenses
	) {
		public static Response from(OnboardingResult result) {
			return new Response(
				result.summaryText(),
				result.averageIncome(),
				result.averageExpense(),
				result.remainingAmount(),
				result.fixedExpenseRatio(),
				result.variableExpenseRatio(),
				result.insightMessage(),
				result.categoryExpenses()
			);
		}
	}

	public record OnboardingResult(
		String summaryText,
		Long averageIncome,
		Long averageExpense,
		Long remainingAmount,
		Double fixedExpenseRatio,
		Double variableExpenseRatio,
		String insightMessage,
		List<CategoryExpense> categoryExpenses
	) {
	}

	public record CategoryExpense(
		Integer categoryId,
		String categoryName,
		Long amount
	) {
	}
}
