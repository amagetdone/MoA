package com.ssafy.amagetdone.domain.budget.api.dto;

import java.util.List;

public final class BudgetHomeDto {

	private BudgetHomeDto() {
	}

	public record Response(
		FreeSpending freeSpending,
		WeeklySaving weeklySaving,
		AssetSummary assetSummary,
		List<PlanCard> plans
	) {

		public static Response of(
			FreeSpending freeSpending,
			WeeklySaving weeklySaving,
			AssetSummary assetSummary,
			List<PlanCard> plans
		) {
			return new Response(
				freeSpending == null ? FreeSpending.empty() : freeSpending,
				weeklySaving == null ? WeeklySaving.empty() : weeklySaving,
				assetSummary == null ? AssetSummary.empty() : assetSummary,
				copyOfOrEmpty(plans)
			);
		}
	}

	public record FreeSpending(
		long usedAmount,
		long totalAmount,
		long remainingAmount,
		long daysRemaining,
		long dailyAvailable
	) {

		public static FreeSpending of(
			Long usedAmount,
			Long totalAmount,
			Long remainingAmount,
			Long daysRemaining,
			Long dailyAvailable
		) {
			return new FreeSpending(
				zero(usedAmount),
				zero(totalAmount),
				zero(remainingAmount),
				zero(daysRemaining),
				zero(dailyAvailable)
			);
		}

		public static FreeSpending empty() {
			return of(null, null, null, null, null);
		}
	}

	public record WeeklySaving(
		long thisWeekSavingAmount,
		long remainingToTargetAmount,
		long progressRate
	) {

		public static WeeklySaving of(
			Long thisWeekSavingAmount,
			Long remainingToTargetAmount,
			Long progressRate
		) {
			return new WeeklySaving(
				zero(thisWeekSavingAmount),
				zero(remainingToTargetAmount),
				zero(progressRate)
			);
		}

		public static WeeklySaving empty() {
			return of(null, null, null);
		}
	}

	public record AssetSummary(
		long totalAssetAmount
	) {

		public static AssetSummary of(Long totalAssetAmount) {
			return new AssetSummary(zero(totalAssetAmount));
		}

		public static AssetSummary empty() {
			return of(null);
		}
	}

	public record PlanCard(
		Long planId,
		String title,
		Integer priority,
		long dDay,
		long savedAmount,
		long targetAmount,
		long progressRate,
		String imageUrl
	) {

		public static PlanCard of(
			Long planId,
			String title,
			Integer priority,
			Long dDay,
			Long savedAmount,
			Long targetAmount,
			Long progressRate,
			String imageUrl
		) {
			return new PlanCard(
				planId,
				title,
				priority,
				zero(dDay),
				zero(savedAmount),
				zero(targetAmount),
				zero(progressRate),
				imageUrl
			);
		}
	}

	private static long zero(Long amount) {
		return amount == null ? 0L : amount;
	}

	private static <T> List<T> copyOfOrEmpty(List<T> items) {
		return items == null ? List.of() : List.copyOf(items);
	}
}
