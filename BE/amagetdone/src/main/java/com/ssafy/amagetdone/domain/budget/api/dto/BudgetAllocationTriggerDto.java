package com.ssafy.amagetdone.domain.budget.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public final class BudgetAllocationTriggerDto {

	private BudgetAllocationTriggerDto() {
	}

	@Schema(name = "BudgetAllocationTriggerResponse")
	public record Response(
		boolean success
	) {
	}
}
