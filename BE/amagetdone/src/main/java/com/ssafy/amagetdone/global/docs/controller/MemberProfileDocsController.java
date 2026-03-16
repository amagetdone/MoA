package com.ssafy.amagetdone.global.docs.controller;

import com.ssafy.amagetdone.domain.member.api.dto.ProfileCompletionDto;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "회원", description = "회원 프로필 API")
public interface MemberProfileDocsController {

	@Operation(summary = "회원 프로필 입력 완료", description = "인증된 사용자의 프로필 입력을 완료합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 입력 완료 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content)
	})
	ApiResponse<ProfileCompletionDto.Response> completeProfile(
		@Parameter(hidden = true) AuthenticatedMember authenticatedMember,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "프로필 입력 완료 요청",
			content = @Content(schema = @Schema(implementation = ProfileCompletionDto.Request.class))
		)
		ProfileCompletionDto.Request request
	);
}
