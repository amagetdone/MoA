package com.ssafy.amagetdone.domain.member.api.controller;

import com.ssafy.amagetdone.domain.member.api.dto.ProfileCompletionDto;
import com.ssafy.amagetdone.domain.member.application.service.MemberProfileService;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/members")
public class MemberProfileController {

	private final MemberProfileService memberProfileService;

	public MemberProfileController(MemberProfileService memberProfileService) {
		this.memberProfileService = memberProfileService;
	}

	@PatchMapping("/me/profile")
	public ApiResponse<ProfileCompletionDto.Response> completeProfile(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@RequestBody ProfileCompletionDto.Request request
	) {
		return ApiResponse.ok(
			"프로필 입력이 완료되었습니다.",
			memberProfileService.completeProfile(authenticatedMember.userId(), request)
		);
	}
}
