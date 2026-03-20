package com.ssafy.amagetdone.domain.budget.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public final class BudgetHomeDto {

	private BudgetHomeDto() {
	}

	@Schema(name = "BudgetHomeResponse")
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
		long totalAssetAmount,
		List<AssetAccount> accounts
	) {

		public static AssetSummary of(Long totalAssetAmount) {
			return of(totalAssetAmount, null);
		}

		public static AssetSummary of(Long totalAssetAmount, List<AssetAccount> accounts) {
			return new AssetSummary(
				zero(totalAssetAmount),
				copyOfOrEmpty(accounts)
			);
		}

		public static AssetSummary empty() {
			return of(null, null);
		}
	}

	public record AssetAccount(
		String accountId,
		String bankName,
		String accountNo,
		long balanceAmount
	) {

		public static AssetAccount of(
			String accountId,
			String bankName,
			String accountNo,
			Long balanceAmount
		) {
			return new AssetAccount(
				accountId,
				bankName,
				accountNo,
				zero(balanceAmount)
			);
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
