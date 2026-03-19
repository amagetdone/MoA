package com.ssafy.amagetdone.domain.plan.api.dto;

import java.time.LocalDate;
import java.util.List;

public record PlanDetailResponse(
	Integer planId,
	String title,
	String description,
	Integer priority,
	Long targetAmount,
	Long savedAmount,
	Integer achievementRate,
	Long dday,
	GraphData graphData,
	List<WeeklyPlan> weeklyPlans,
	Integer currentWeek,
	Integer totalWeeks,
	CompletionProjection completionProjection
) {

	public record GraphData(
		List<Integer> weeks,
		List<Long> planned,
		List<Long> actual
	) {
	}

	public record WeeklyPlan(
		Integer week,
		Long amount
	) {
	}

	public record CompletionProjection(
		Integer weeksToGoal,
		LocalDate estimatedCompletionDate
	) {
	}
}
