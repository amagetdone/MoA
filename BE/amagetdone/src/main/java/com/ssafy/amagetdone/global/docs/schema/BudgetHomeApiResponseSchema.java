package com.ssafy.amagetdone.global.docs.schema;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BudgetHomeApiResponse")
public record BudgetHomeApiResponseSchema(
	@Schema(example = "ok")
	String status,
	@Schema(example = "홈 화면 통합 정보를 조회했습니다.")
	String message,
	BudgetHomeResponseSchema data
) {

	@Schema(name = "BudgetHomeResponse")
	public record BudgetHomeResponseSchema(
		FreeSpendingSchema freeSpending,
		WeeklySavingSchema weeklySaving,
		AssetSummarySchema assetSummary,
		List<PlanCardSchema> plans
	) {
	}

	public record FreeSpendingSchema(
		long usedAmount,
		long totalAmount,
		long remainingAmount,
		long daysRemaining,
		long dailyAvailable
	) {
	}

	public record WeeklySavingSchema(
		long thisWeekSavingAmount,
		long remainingToTargetAmount,
		long progressRate
	) {
	}

	public record AssetSummarySchema(
		long totalAssetAmount
	) {
	}

	public record PlanCardSchema(
		Long planId,
		String title,
		Integer priority,
		long dDay,
		long savedAmount,
		long targetAmount,
		long progressRate,
		String imageUrl
	) {
	}
}
