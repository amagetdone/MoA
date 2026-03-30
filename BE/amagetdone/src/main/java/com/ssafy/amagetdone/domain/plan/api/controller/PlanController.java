package com.ssafy.amagetdone.domain.plan.api.controller;

import java.util.List;

import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateRequest;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateResponse;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanDetailResponse;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanListItemResponse;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanPriorityUpdateRequest;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanUpdateRequest;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanWeeklyPlansPageResponse;
import com.ssafy.amagetdone.domain.plan.application.usecase.CreatePlanUseCase;
import com.ssafy.amagetdone.domain.plan.application.usecase.DeletePlanUseCase;
import com.ssafy.amagetdone.domain.plan.application.usecase.GetPlanDetailUseCase;
import com.ssafy.amagetdone.domain.plan.application.usecase.ListPlanWeeklyPlansUseCase;
import com.ssafy.amagetdone.domain.plan.application.usecase.ListPlansUseCase;
import com.ssafy.amagetdone.domain.plan.application.usecase.UpdatePlanUseCase;
import com.ssafy.amagetdone.domain.plan.application.usecase.UpdatePlanPrioritiesUseCase;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Tag(name = "Plan API")
@RestController
@RequestMapping("/v1/plans")
public class PlanController {

	private final CreatePlanUseCase createPlanUseCase;
	private final DeletePlanUseCase deletePlanUseCase;
	private final ListPlansUseCase listPlansUseCase;
	private final GetPlanDetailUseCase getPlanDetailUseCase;
	private final ListPlanWeeklyPlansUseCase listPlanWeeklyPlansUseCase;
	private final UpdatePlanPrioritiesUseCase updatePlanPrioritiesUseCase;
	private final UpdatePlanUseCase updatePlanUseCase;

	public PlanController(
		CreatePlanUseCase createPlanUseCase,
		DeletePlanUseCase deletePlanUseCase,
		ListPlansUseCase listPlansUseCase,
		GetPlanDetailUseCase getPlanDetailUseCase,
		ListPlanWeeklyPlansUseCase listPlanWeeklyPlansUseCase,
		UpdatePlanPrioritiesUseCase updatePlanPrioritiesUseCase,
		UpdatePlanUseCase updatePlanUseCase
	) {
		this.createPlanUseCase = createPlanUseCase;
		this.deletePlanUseCase = deletePlanUseCase;
		this.listPlansUseCase = listPlansUseCase;
		this.getPlanDetailUseCase = getPlanDetailUseCase;
		this.listPlanWeeklyPlansUseCase = listPlanWeeklyPlansUseCase;
		this.updatePlanPrioritiesUseCase = updatePlanPrioritiesUseCase;
		this.updatePlanUseCase = updatePlanUseCase;
	}

	@Operation(summary = "목표 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping
	public ApiResponse<List<PlanListItemResponse>> listPlans(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember
	) {
		return ApiResponse.ok(
			"목표 목록 조회 성공",
			listPlansUseCase.execute(authenticatedMember.userId())
		);
	}

	@Operation(summary = "목표 상세 조회", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/{planId}")
	public ApiResponse<PlanDetailResponse> getPlanDetail(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable Integer planId
	) {
		return ApiResponse.ok(
			"목표 상세 조회 성공",
			getPlanDetailUseCase.execute(authenticatedMember.userId(), planId)
		);
	}

	@Operation(summary = "목표 주간 계획 페이지 조회", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/{planId}/weekly-plans")
	public ApiResponse<PlanWeeklyPlansPageResponse> listPlanWeeklyPlans(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable Integer planId,
		@RequestParam(name = "page", required = false) Integer page,
		@RequestParam(name = "size", required = false) Integer size
	) {
		return ApiResponse.ok(
			"목표 주간 계획 조회 성공",
			listPlanWeeklyPlansUseCase.execute(authenticatedMember.userId(), planId, page, size)
		);
	}

	@Operation(summary = "목표 생성", security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping
	public ApiResponse<PlanCreateResponse> createPlan(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@Valid @RequestBody PlanCreateRequest request
	) {
		return ApiResponse.ok(
			"목표가 생성되었습니다.",
			createPlanUseCase.execute(authenticatedMember.userId(), request)
		);
	}

	@Operation(summary = "목표 수정", security = @SecurityRequirement(name = "bearerAuth"))
	@PatchMapping("/{planId}")
	public ApiResponse<Void> updatePlan(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable Integer planId,
		@Valid @RequestBody PlanUpdateRequest request
	) {
		updatePlanUseCase.execute(authenticatedMember.userId(), planId, request);
		return ApiResponse.ok("목표가 수정되었습니다.", null);
	}

	@Operation(summary = "목표 삭제", security = @SecurityRequirement(name = "bearerAuth"))
	@DeleteMapping("/{planId}")
	public ApiResponse<Void> deletePlan(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable Integer planId
	) {
		deletePlanUseCase.execute(authenticatedMember.userId(), planId);
		return ApiResponse.ok("목표가 삭제되었습니다.", null);
	}

	@Operation(summary = "목표 우선순위 변경", security = @SecurityRequirement(name = "bearerAuth"))
	@PatchMapping("/priority")
	public ApiResponse<Void> updatePlanPriorities(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@Valid @RequestBody PlanPriorityUpdateRequest request
	) {
		updatePlanPrioritiesUseCase.execute(authenticatedMember.userId(), request);
		return ApiResponse.ok("목표 우선순위가 변경되었습니다.", null);
	}
}
