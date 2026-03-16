package com.ssafy.amagetdone.global.docs.controller;

import com.ssafy.amagetdone.domain.budget.api.dto.WeeklyEssentialExpenseDto;
import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Budget", description = "Budget analysis APIs")
public interface BudgetDocsController {

	@Operation(summary = "Get weekly essential expenses", description = "Returns essential expense guidance for a budget week.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Essential expenses retrieved"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
	})
	ApiResponse<WeeklyEssentialExpenseDto.Response> getWeeklyEssentialExpenses(
		@Parameter(hidden = true) String authorization,
		@Parameter(description = "Target week, yyyy-Www") String yearWeek
	);
}
