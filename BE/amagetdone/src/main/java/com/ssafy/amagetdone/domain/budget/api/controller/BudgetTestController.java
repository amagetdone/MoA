package com.ssafy.amagetdone.domain.budget.api.controller;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSeedRequest;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSeedResponse;
import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetCurrentWeekGoalAllocationTestService;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetWeekSeedService;
import com.ssafy.amagetdone.domain.budget.application.service.GoalSavingAccrualForceTestService;
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
	private final BudgetCurrentWeekGoalAllocationTestService budgetCurrentWeekGoalAllocationTestService;
	private final GoalSavingAccrualForceTestService goalSavingAccrualForceTestService;

	public BudgetTestController(
		BudgetWeekSeedService budgetWeekSeedService,
		BudgetCurrentWeekGoalAllocationTestService budgetCurrentWeekGoalAllocationTestService,
		GoalSavingAccrualForceTestService goalSavingAccrualForceTestService
	) {
		this.budgetWeekSeedService = budgetWeekSeedService;
		this.budgetCurrentWeekGoalAllocationTestService = budgetCurrentWeekGoalAllocationTestService;
		this.goalSavingAccrualForceTestService = goalSavingAccrualForceTestService;
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

	@Operation(
		summary = "Force current-week goal savings allocation",
		description = "Re-runs the goal savings allocation pipeline and returns the current week's allocation result for manual verification.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@PostMapping("/weeks/current/savings-allocation/force")
	public ApiResponse<WeeklySavingsDetailDto.Response> forceCurrentWeekSavingsAllocation(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember
	) {
		return ApiResponse.ok(
			"Current week goal savings allocation test succeeded.",
			budgetCurrentWeekGoalAllocationTestService.forceCurrentWeekAllocation(authenticatedMember.userId())
		);
	}

	@Operation(
		summary = "Backfill goal saving accrual",
		description = "Reconciles goal saving accrual from plan start through cutoff for manual catch-up verification.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@PostMapping("/savings-accrual/backfill")
	public ApiResponse<GoalSavingAccrualForceDto.Response> backfillSavingsAccrual(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody(required = false) GoalSavingAccrualForceDto.BackfillRequest request
	) {
		return ApiResponse.ok(
			"Goal saving accrual backfill test succeeded.",
			goalSavingAccrualForceTestService.backfill(authenticatedMember.userId(), request)
		);
	}

	@Operation(
		summary = "Apply goal saving accrual instantly",
		description = "Applies one day, week, or month of goal saving immediately for manual verification.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@PostMapping("/savings-accrual/apply")
	public ApiResponse<GoalSavingAccrualForceDto.Response> applySavingsAccrual(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody(required = false) GoalSavingAccrualForceDto.ApplyRequest request
	) {
		return ApiResponse.ok(
			"Goal saving accrual apply test succeeded.",
			goalSavingAccrualForceTestService.apply(authenticatedMember.userId(), request)
		);
	}
}
