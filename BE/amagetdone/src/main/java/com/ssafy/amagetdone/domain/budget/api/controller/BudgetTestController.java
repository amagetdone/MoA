package com.ssafy.amagetdone.domain.budget.api.controller;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSeedRequest;
import com.ssafy.amagetdone.domain.budget.api.dto.BudgetWeekSeedResponse;
import com.ssafy.amagetdone.domain.budget.api.dto.GoalSavingAccrualForceDto;
import com.ssafy.amagetdone.domain.budget.api.dto.WeeklySavingsDetailDto;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetCurrentWeekGoalAllocationTestService;
import com.ssafy.amagetdone.domain.budget.application.service.BudgetCurrentWeekSavingsReapplyTestService;
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

@Tag(name = "예산 테스트", description = "예산 주차 시드 및 저축 테스트용 수동 호출 문서")
@RestController
@RequestMapping("/test/budget")
public class BudgetTestController {

	private final BudgetWeekSeedService budgetWeekSeedService;
	private final BudgetCurrentWeekGoalAllocationTestService budgetCurrentWeekGoalAllocationTestService;
	private final BudgetCurrentWeekSavingsReapplyTestService budgetCurrentWeekSavingsReapplyTestService;
	private final GoalSavingAccrualForceTestService goalSavingAccrualForceTestService;

	public BudgetTestController(
		BudgetWeekSeedService budgetWeekSeedService,
		BudgetCurrentWeekGoalAllocationTestService budgetCurrentWeekGoalAllocationTestService,
		BudgetCurrentWeekSavingsReapplyTestService budgetCurrentWeekSavingsReapplyTestService,
		GoalSavingAccrualForceTestService goalSavingAccrualForceTestService
	) {
		this.budgetWeekSeedService = budgetWeekSeedService;
		this.budgetCurrentWeekGoalAllocationTestService = budgetCurrentWeekGoalAllocationTestService;
		this.budgetCurrentWeekSavingsReapplyTestService = budgetCurrentWeekSavingsReapplyTestService;
		this.goalSavingAccrualForceTestService = goalSavingAccrualForceTestService;
	}

	@Operation(
		summary = "예산 주차 시드 생성",
		description = "인증된 사용자의 예산 주차 데이터를 월요일부터 일요일 기준으로 생성해 목표 생성 흐름을 수동 검증합니다.",
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
		summary = "현재 주차 목표 저축 배분 강제 실행",
		description = "목표 저축 배분 파이프라인을 다시 실행하고 현재 주차 배분 결과를 반환해 수동 검증합니다.",
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
		summary = "?꾩옱 二쇱감 紐⑺몴 ?異? ?곸슜 ?ㅼ떆 怨꾩궛",
		description = "?꾩옱 二쇱감 ?먮룞 ?異??곷┰???쒓굅?섍퀬 ?꾩옱 ?덉궛 ?곹깭濡??ㅼ떆 ?異??곸슜?덉뿬 ?붾㈃ ?곷깭瑜??섎룞 寃利앺빀?덈떎.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	@PostMapping("/weeks/current/savings-accrual/reapply")
	public ApiResponse<WeeklySavingsDetailDto.Response> reapplyCurrentWeekSavingsAccrual(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember
	) {
		return ApiResponse.ok(
			"Current week goal savings accrual reapply test succeeded.",
			budgetCurrentWeekSavingsReapplyTestService.reapplyCurrentWeekSavings(authenticatedMember.userId())
		);
	}

	@Operation(
		summary = "목표 저축 적립 백필 실행",
		description = "목표 시작일부터 기준 시점까지의 저축 적립을 재정산해 누락 적립 여부를 수동 검증합니다.",
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
		summary = "목표 저축 적립 즉시 적용",
		description = "하루, 일주일 또는 한 달치 목표 저축 적립을 즉시 반영해 수동 검증합니다.",
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
