package com.ssafy.amagetdone.domain.plan.api.dto;

import java.util.List;

public record PlanWeeklyPlansPageResponse(
	Integer planId,
	List<WeeklyPlan> weeklyPlans,
	Integer page,
	Integer size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {

	public record WeeklyPlan(
		Integer week,
		Long amount
	) {
	}
}
