package com.ssafy.amagetdone.domain.plan.api.dto;

import java.util.List;

public record PlanCreateResponse(
	Integer planId,
	String planTitle,
	WeeklyMission weeklyMission,
	WeeklyBudget weeklyBudget,
	String status
) {

	public record WeeklyMission(
		Long weeklySavingsAmount,
		Integer weeksToGoal
	) {
	}

	public record WeeklyBudget(
		Long totalIncomeAmount,
		Long savingAmount,
		BudgetBucket fixedExpense,
		BudgetBucket variableExpense,
		Long freeAmount
	) {
	}

	public record BudgetBucket(
		Long totalAmount,
		List<BudgetItem> items
	) {
	}

	public record BudgetItem(
		String category,
		Long amount
	) {
	}
}
