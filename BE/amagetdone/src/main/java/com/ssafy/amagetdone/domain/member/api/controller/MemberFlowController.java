package com.ssafy.amagetdone.domain.member.api.controller;

import com.ssafy.amagetdone.domain.member.api.dto.MemberFlowDto;
import com.ssafy.amagetdone.domain.member.application.usecase.ResetUserFlowStateUseCase;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원 플로우 유틸 API")
@RestController
@RequestMapping("/v1/members/me")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class MemberFlowController {

	private final ResetUserFlowStateUseCase resetUserFlowStateUseCase;

	@Operation(summary = "사용자 플로우 상태 초기화")
	@PostMapping("/flow-reset")
	public ApiResponse<MemberFlowDto.ResetResponse> resetFlowState(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember
	) {
		resetUserFlowStateUseCase.execute(authenticatedMember.userId());
		return ApiResponse.ok(
			"사용자 플로우 상태를 초기화했습니다.",
			new MemberFlowDto.ResetResponse(true)
		);
	}
}
