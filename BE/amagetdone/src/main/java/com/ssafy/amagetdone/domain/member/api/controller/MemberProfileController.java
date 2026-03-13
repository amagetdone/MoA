package com.ssafy.amagetdone.domain.member.api.controller;

import com.ssafy.amagetdone.domain.member.api.dto.ProfileCompletionDto;
import com.ssafy.amagetdone.domain.member.application.service.MemberProfileService;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.response.ApiResponse;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/members")
public class MemberProfileController {

	private final MemberProfileService memberProfileService;
	private final JwtAccessTokenReader jwtAccessTokenReader;

	public MemberProfileController(
		MemberProfileService memberProfileService,
		JwtAccessTokenReader jwtAccessTokenReader
	) {
		this.memberProfileService = memberProfileService;
		this.jwtAccessTokenReader = jwtAccessTokenReader;
	}

	@PatchMapping("/me/profile")
	public ApiResponse<ProfileCompletionDto.Response> completeProfile(
		@RequestHeader(name = "Authorization", required = false) String authorization,
		@RequestBody ProfileCompletionDto.Request request
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		return ApiResponse.ok("프로필 입력이 완료되었습니다.", memberProfileService.completeProfile(userId, request));
	}
}
