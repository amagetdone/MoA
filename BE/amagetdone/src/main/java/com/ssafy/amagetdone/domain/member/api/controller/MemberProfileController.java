package com.ssafy.amagetdone.domain.member.api.controller;

import com.ssafy.amagetdone.domain.member.api.dto.ProfileCompletionDto;
import com.ssafy.amagetdone.domain.member.application.service.MemberProfileImageService;
import com.ssafy.amagetdone.domain.member.application.service.MemberProfileService;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Member Profile API")
@RestController
@RequestMapping("/v1/members")
public class MemberProfileController {

	private final MemberProfileService memberProfileService;
	private final MemberProfileImageService memberProfileImageService;

	public MemberProfileController(
		MemberProfileService memberProfileService,
		MemberProfileImageService memberProfileImageService
	) {
		this.memberProfileService = memberProfileService;
		this.memberProfileImageService = memberProfileImageService;
	}

	@Operation(summary = "회원 프로필 입력 완료")
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

	@Operation(
		summary = "프로필 이미지 업로드",
		description = "로그인한 사용자의 프로필 이미지를 업로드하고 기존 이미지가 있으면 교체합니다."
	)
	@PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<String> uploadProfileImage(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@Parameter(
			description = "업로드할 프로필 이미지 파일",
			content = @Content(
				mediaType = "multipart/form-data",
				schema = @Schema(type = "string", format = "binary")
			)
		)
		@RequestPart("image") MultipartFile image
	) {
		return ApiResponse.ok(
			"프로필 이미지가 업로드되었습니다.",
			memberProfileImageService.replaceProfileImage(authenticatedMember.userId(), image)
		);
	}
}
