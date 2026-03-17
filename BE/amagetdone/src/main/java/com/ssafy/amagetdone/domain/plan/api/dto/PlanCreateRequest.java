package com.ssafy.amagetdone.domain.plan.api.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(
	name = "PlanCreateRequest",
	example = """
		{
		  "title": "유럽 여행 자금",
		  "description": "여름 여행을 위한 저축",
		  "targetAmount": 3000000,
		  "startDate": "2026-03-16",
		  "endDate": "2026-09-01",
		  "priority": 1
		}
		"""
)
public record PlanCreateRequest(
	@NotBlank String title,
	String description,
	@NotNull @Positive Long targetAmount,
	@NotNull LocalDate startDate,
	@NotNull LocalDate endDate,
	@NotNull @Positive Integer priority
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
