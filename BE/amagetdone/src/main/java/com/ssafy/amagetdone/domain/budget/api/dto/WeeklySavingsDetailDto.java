package com.ssafy.amagetdone.domain.budget.api.dto;

import java.time.LocalDate;
import java.util.List;

public final class WeeklySavingsDetailDto {

	private WeeklySavingsDetailDto() {
	}

	public record Response(
		String yearWeek,
		LocalDate weekStartDate,
		LocalDate weekEndDate,
		Long plannedSavingAmount,
		Long finalSavingAmount,
		List<SavingDetail> savings
	) {

		public static Response of(
			String yearWeek,
			LocalDate weekStartDate,
			LocalDate weekEndDate,
			Long plannedSavingAmount,
			Long finalSavingAmount,
			List<SavingDetail> savings
		) {
			return new Response(
				yearWeek,
				weekStartDate,
				weekEndDate,
				zero(plannedSavingAmount),
				zero(finalSavingAmount),
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

	public record SavingDetail(
		String planTitle,
		Integer priority,
		Long plannedAmount,
		Long finalAmount,
		Long savedAmount,
		AdjustmentSummary adjustment
	) {
	}

	public record AdjustmentSummary(
		Integer count,
		String latestAdjustmentType,
		String latestReason
	) {

		public static AdjustmentSummary none() {
			return new AdjustmentSummary(0, null, null);
		}
	}
}
