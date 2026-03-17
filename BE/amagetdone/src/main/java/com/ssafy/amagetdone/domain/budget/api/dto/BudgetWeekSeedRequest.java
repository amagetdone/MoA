package com.ssafy.amagetdone.domain.budget.api.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Schema(
	name = "BudgetWeekSeedRequest",
	example = """
		{
		  "startDate": "2026-03-18",
		  "endDate": "2026-09-01"
		}
		"""
)
public record BudgetWeekSeedRequest(
	@NotNull LocalDate startDate,
	@NotNull LocalDate endDate
) {

	@JsonIgnore
	@Schema(hidden = true)
	@AssertTrue(message = "startDate must be on or before endDate")
	public boolean isDateRangeValid() {
		if (startDate == null || endDate == null) {
			return true;
		}
		return !startDate.isAfter(endDate);
	}
}
