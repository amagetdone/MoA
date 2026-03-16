package com.ssafy.amagetdone.global.docs.controller;

import com.ssafy.amagetdone.domain.member.api.dto.ProfileCompletionDto;
import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Member", description = "Member profile APIs")
public interface MemberProfileDocsController {

	@Operation(summary = "Complete profile", description = "Completes the profile for the authenticated user.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile completion succeeded"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
	})
	ApiResponse<ProfileCompletionDto.Response> completeProfile(
		@Parameter(hidden = true) String authorization,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "Profile completion request",
			content = @Content(schema = @Schema(implementation = ProfileCompletionDto.Request.class))
		)
		ProfileCompletionDto.Request request
	);
}
