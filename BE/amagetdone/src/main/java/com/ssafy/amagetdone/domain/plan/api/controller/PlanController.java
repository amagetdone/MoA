package com.ssafy.amagetdone.domain.plan.api.controller;

import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateRequest;
import com.ssafy.amagetdone.domain.plan.api.dto.PlanCreateResponse;
import com.ssafy.amagetdone.domain.plan.application.usecase.CreatePlanUseCase;
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

@Tag(name = "Plan API")
@RestController
@RequestMapping("/v1/plans")
public class PlanController {

	private final CreatePlanUseCase createPlanUseCase;

	public PlanController(CreatePlanUseCase createPlanUseCase) {
		this.createPlanUseCase = createPlanUseCase;
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
