package com.ssafy.amagetdone.domain.plan.api.controller;

import com.ssafy.amagetdone.domain.plan.application.service.PlanImageService;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "목표 이미지 API")
@RestController
@RequestMapping("/v1/plans")
@SecurityRequirement(name = "bearerAuth")
public class PlanImageController {

	private final PlanImageService planImageService;

	public PlanImageController(PlanImageService planImageService) {
		this.planImageService = planImageService;
	}

	@Operation(
		summary = "목표 이미지 업로드",
		description = "로그인한 사용자의 특정 목표 이미지를 업로드하고 기존 이미지가 있으면 교체합니다."
	)
	@SecurityRequirement(name = "bearerAuth")
	@PatchMapping(value = "/{planId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<String> uploadPlanImage(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@Parameter(description = "이미지를 업로드할 목표 ID")
		@PathVariable Integer planId,
		@Parameter(
			description = "업로드할 목표 이미지 파일",
			content = @Content(
				mediaType = "multipart/form-data",
				schema = @Schema(type = "string", format = "binary")
			)
		)
		@RequestPart("image") MultipartFile image
	) {
		return ApiResponse.ok(
			"목표 이미지가 업로드되었습니다.",
			planImageService.replacePlanImage(authenticatedMember.userId(), planId, image)
		);
	}
}
