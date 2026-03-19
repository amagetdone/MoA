package com.ssafy.amagetdone.domain.plan.api.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.ssafy.amagetdone.domain.plan.model.entity.Plan;

public record PlanListItemResponse(
	Long planId,
	String title,
	Long savedAmount,
	Long targetAmount,
	Integer priority,
	Integer weekNumber,
	Long dday,
	BigDecimal achievementRate
) {

	public static PlanListItemResponse from(Plan plan, LocalDate today) {
		long safeSavedAmount = zero(plan.getSavedAmount());
		long safeTargetAmount = zero(plan.getTargetAmount());

		return new PlanListItemResponse(
			plan.getId() == null ? null : plan.getId().longValue(),
			plan.getTitle(),
			safeSavedAmount,
			safeTargetAmount,
			plan.getPriority(),
			plan.getTargetWeeks() == null ? 0 : plan.getTargetWeeks(),
			calculateDday(today, plan.getEndDate()),
			calculateAchievementRate(safeSavedAmount, safeTargetAmount)
		);
	}

	private static long calculateDday(LocalDate today, LocalDate endDate) {
		if (endDate == null) {
			return 0L;
		}

		return ChronoUnit.DAYS.between(today, endDate);
	}

	private static BigDecimal calculateAchievementRate(long savedAmount, long targetAmount) {
		if (targetAmount <= 0L) {
			return BigDecimal.ZERO;
		}

		return BigDecimal.valueOf(savedAmount)
			.multiply(BigDecimal.valueOf(100))
			.divide(BigDecimal.valueOf(targetAmount), 1, RoundingMode.HALF_UP)
			.stripTrailingZeros();
	}

	private static long zero(Long amount) {
		return amount == null ? 0L : amount;
	}
}
