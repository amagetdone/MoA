package com.ssafy.amagetdone.domain.budget.api.dto;

import java.time.LocalDate;
import java.util.List;

public final class GoalSavingAccrualForceDto {

	private GoalSavingAccrualForceDto() {
	}

	public record Request(
		Integer planId,
		Scope scope
	) {

		public Scope resolvedScope() {
			return scope == null ? Scope.WEEK : scope;
		}
	}

	public record Response(
		Scope appliedScope,
		TargetType targetType,
		Integer planId,
		Long appliedAmount,
		LocalDate executedDate,
		LocalDate cutoffDate,
		LocalDate periodStartDate,
		LocalDate periodEndDate,
		String periodLabel,
		List<PlanResult> plans
	) {
		public Response {
			appliedAmount = zero(appliedAmount);
			periodLabel = periodLabel == null ? "" : periodLabel;
			plans = plans == null ? List.of() : List.copyOf(plans);
		}

		private static long zero(Long amount) {
			return amount == null ? 0L : amount;
		}
	}

	public record PlanResult(
		Integer planId,
		String planTitle,
		Integer priority,
		Long plannedAmount,
		Long finalAmount,
		Long accruedAmount,
		Long savedAmount,
		Long remainingScheduledAmount
	) {
		public PlanResult {
			plannedAmount = zero(plannedAmount);
			finalAmount = zero(finalAmount);
			accruedAmount = zero(accruedAmount);
			savedAmount = zero(savedAmount);
			remainingScheduledAmount = zero(remainingScheduledAmount);
		}

		private static long zero(Long amount) {
			return amount == null ? 0L : amount;
		}
	}

	public enum Scope {
		DAY,
		WEEK,
		MONTH
	}

	public enum TargetType {
		ALL,
		SINGLE
	}
}
