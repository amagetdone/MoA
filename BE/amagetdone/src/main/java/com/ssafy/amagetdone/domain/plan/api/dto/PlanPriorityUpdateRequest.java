package com.ssafy.amagetdone.domain.plan.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(
	name = "PlanPriorityUpdateRequest",
	example = """
		{
		  "plans": [
		    { "planId": 101, "priority": 1 },
		    { "planId": 102, "priority": 2 },
		    { "planId": 103, "priority": 3 }
		  ]
		}
		"""
)
public record PlanPriorityUpdateRequest(
	@NotEmpty List<@Valid PlanPriorityUpdateItem> plans
) {

	public record PlanPriorityUpdateItem(
		@NotNull Integer planId,
		@NotNull @Positive Integer priority
	) {
	}
}
