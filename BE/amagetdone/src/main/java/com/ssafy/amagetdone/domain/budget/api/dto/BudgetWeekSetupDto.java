package com.ssafy.amagetdone.domain.budget.api.dto;

import java.util.List;

public final class BudgetWeekSetupDto {

	private BudgetWeekSetupDto() {
	}

	public record Response(
		Long plannedIncomeAmount,
		Long plannedEssentialAmount,
		Long plannedSavingAmount,
		Long plannedFreeAmount,
		Long finalFreeAmount,
		Long actualSpentAmount,
		Long remainingFreeAmount,
		List<Essential> essentials,
		List<Saving> savings
	) {

		public static Response of(
			Long plannedIncomeAmount,
			Long plannedEssentialAmount,
			Long plannedSavingAmount,
			Long finalFreeAmount,
			Long actualSpentAmount,
			List<Essential> essentials,
			List<Saving> savings
		) {
			long resolvedPlannedIncomeAmount = zero(plannedIncomeAmount);
			long resolvedPlannedEssentialAmount = zero(plannedEssentialAmount);
			long resolvedPlannedSavingAmount = zero(plannedSavingAmount);
			long resolvedFinalFreeAmount = zero(finalFreeAmount);
			long resolvedActualSpentAmount = zero(actualSpentAmount);

			return new Response(
				resolvedPlannedIncomeAmount,
				resolvedPlannedEssentialAmount,
				resolvedPlannedSavingAmount,
				resolvedPlannedIncomeAmount - resolvedPlannedEssentialAmount - resolvedPlannedSavingAmount,
				resolvedFinalFreeAmount,
				resolvedActualSpentAmount,
				resolvedFinalFreeAmount - resolvedActualSpentAmount,
				copyOfOrEmpty(essentials),
				copyOfOrEmpty(savings)
			);
		}

		private static long zero(Long amount) {
			return amount == null ? 0L : amount;
		}

		private static <T> List<T> copyOfOrEmpty(List<T> items) {
			return items == null ? List.of() : List.copyOf(items);
		}
	}

	public record Essential(
		Integer categoryId,
		String categoryName,
		String expenseScope,
		Long plannedAmount,
		Long actualSpentAmount
	) {
	}

	public record Saving(
		String planTitle,
		Integer priority,
		Long plannedAmount,
		Long finalAmount
	) {
	}
}
