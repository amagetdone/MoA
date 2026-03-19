package com.ssafy.amagetdone.domain.plan.api.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.amagetdone.domain.plan.application.usecase.GetPlanVideoDownloadUrlUseCase;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Plan Video API")
@RestController
@RequestMapping("/plans")
@SecurityRequirement(name = "bearerAuth")
public class PlanVideoController {

	private final GetPlanVideoDownloadUrlUseCase getPlanVideoDownloadUrlUseCase;

	public PlanVideoController(GetPlanVideoDownloadUrlUseCase getPlanVideoDownloadUrlUseCase) {
		this.getPlanVideoDownloadUrlUseCase = getPlanVideoDownloadUrlUseCase;
	}

	@Operation(summary = "목표 영상 다운로드 URL 조회", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/{planId}/video/download")
	public ApiResponse<String> getPlanVideoDownloadUrl(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@PathVariable Integer planId
	) {
		return ApiResponse.ok(
			"목표 영상 다운로드 URL 조회 성공",
			getPlanVideoDownloadUrlUseCase.execute(authenticatedMember.userId(), planId)
		);
	}
}
