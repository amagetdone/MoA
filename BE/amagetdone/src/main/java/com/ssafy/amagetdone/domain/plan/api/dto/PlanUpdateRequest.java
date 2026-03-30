package com.ssafy.amagetdone.domain.plan.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PlanUpdateRequest(
	@NotBlank String title,
	String description
) {
}
