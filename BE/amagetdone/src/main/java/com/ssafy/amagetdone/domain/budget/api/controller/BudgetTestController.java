package com.ssafy.amagetdone.domain.budget.api.controller;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSeedRequest;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSeedResponse;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetWeekSeedService;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Tag(name = "Budget Test", description = "Local manual test endpoints for budget week seed flows")
@RestController
@RequestMapping("/test/budget")
public class BudgetTestController {

	private final BudgetWeekSeedService budgetWeekSeedService;

	public BudgetTestController(BudgetWeekSeedService budgetWeekSeedService) {
		this.budgetWeekSeedService = budgetWeekSeedService;
	}

	@Operation(
		summary = "Seed budget weeks",
		description = "Creates Monday-Sunday budget_week rows for the authenticated user so plan creation can be tested.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@PostMapping("/weeks/seed")
	public ApiResponse<BudgetWeekSeedResponse> seedBudgetWeeks(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@Valid @RequestBody BudgetWeekSeedRequest request
	) {
		return ApiResponse.ok(
			"Budget week seed test succeeded.",
			budgetWeekSeedService.seed(authenticatedMember.userId(), request)
		);
	}
}
