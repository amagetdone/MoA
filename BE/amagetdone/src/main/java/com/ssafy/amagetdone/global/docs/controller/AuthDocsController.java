package com.ssafy.amagetdone.global.docs.controller;

import com.ssafy.amagetdone.domain.member.api.dto.LogoutDto;
import com.ssafy.amagetdone.domain.member.api.dto.RefreshTokenDto;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.api.dto.TestSocialResetDto;
import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth", description = "Authentication APIs")
public interface AuthDocsController {

	@Operation(summary = "Social login", description = "Logs a user in with the given social provider.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Social login succeeded"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid provider or request", content = @Content)
	})
	ApiResponse<SocialAuthDto.Response> socialAuth(
		@Parameter(description = "Social provider") String provider,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "Social login request",
			content = @Content(schema = @Schema(implementation = SocialAuthDto.Request.class))
		)
		SocialAuthDto.Request request
	);

	@Operation(summary = "Test social login", description = "Logs in with the test social account.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Test login succeeded")
	})
	ApiResponse<SocialAuthDto.Response> socialTestAuth();

	@Operation(summary = "Reset test social user", description = "Resets the test social user.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Test user reset succeeded")
	})
	ApiResponse<TestSocialResetDto.Response> resetTestSocialUser();

	@Operation(summary = "Refresh token", description = "Reissues tokens using a refresh token.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refresh succeeded"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
	})
	ApiResponse<RefreshTokenDto.Response> refresh(
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "Token refresh request",
			content = @Content(schema = @Schema(implementation = RefreshTokenDto.Request.class))
		)
		RefreshTokenDto.Request request
	);

	@Operation(summary = "Logout", description = "Invalidates the refresh token.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout succeeded"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
	})
	ApiResponse<LogoutDto.Response> logout(
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "Logout request",
			content = @Content(schema = @Schema(implementation = LogoutDto.Request.class))
		)
		LogoutDto.Request request
	);
}
