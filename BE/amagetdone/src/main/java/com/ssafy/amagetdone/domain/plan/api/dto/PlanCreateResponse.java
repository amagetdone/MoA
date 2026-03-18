package com.ssafy.amagetdone.domain.plan.api.dto;

import java.time.LocalDate;

public record PlanCreateResponse(
	Integer planId,
	String title,
	String description,
	Long targetAmount,
	LocalDate startDate,
	LocalDate endDate,
	Integer priority,
	String status
) {
}
