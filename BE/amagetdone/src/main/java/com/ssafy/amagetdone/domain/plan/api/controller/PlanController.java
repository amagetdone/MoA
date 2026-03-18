package com.ssafy.amagetdone.domain.plan.api.controller;

import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateRequest;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateResponse;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanListItemResponse;
import com.ssafy.amagetdone.domain.plan.application.usecase.CreatePlanUseCase;
import com.ssafy.amagetdone.domain.plan.application.usecase.ListPlansUseCase;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Tag(name = "Plan API")
@RestController
@RequestMapping("/v1/plans")
public class PlanController {

	private final CreatePlanUseCase createPlanUseCase;
	private final ListPlansUseCase listPlansUseCase;

	public PlanController(CreatePlanUseCase createPlanUseCase, ListPlansUseCase listPlansUseCase) {
		this.createPlanUseCase = createPlanUseCase;
		this.listPlansUseCase = listPlansUseCase;
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
}
