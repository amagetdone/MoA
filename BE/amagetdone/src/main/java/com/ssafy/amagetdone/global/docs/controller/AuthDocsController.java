package com.ssafy.amagetdone.global.docs.controller;

import com.ssafy.amagetdone.domain.member.api.dto.LogoutDto;
import com.ssafy.amagetdone.domain.member.api.dto.RefreshTokenDto;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.api.dto.TestSocialAuthDto;
import com.ssafy.amagetdone.domain.member.api.dto.TestSocialResetDto;
import com.ssafy.amagetdone.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "인증", description = "인증 관련 API")
public interface AuthDocsController {

	@Operation(summary = "소셜 로그인", description = "지정한 소셜 제공자로 로그인합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "소셜 로그인 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 제공자 또는 요청입니다.", content = @Content)
	})
	ApiResponse<SocialAuthDto.Response> socialAuth(
		@Parameter(description = "소셜 제공자") String provider,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "소셜 로그인 요청",
			content = @Content(schema = @Schema(implementation = SocialAuthDto.Request.class))
		)
		SocialAuthDto.Request request
	);

	@Operation(summary = "테스트 소셜 로그인", description = "테스트용 소셜 계정으로 로그인합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "테스트 로그인 성공")
	})
	ApiResponse<SocialAuthDto.Response> socialTestAuth(
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = false,
			description = "테스트 로그인 요청",
			content = @Content(schema = @Schema(implementation = TestSocialAuthDto.Request.class))
		)
		TestSocialAuthDto.Request request
	);

	@Operation(summary = "테스트 소셜 사용자 초기화", description = "테스트용 소셜 사용자를 초기화합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "테스트 사용자 초기화 성공")
	})
	ApiResponse<TestSocialResetDto.Response> resetTestSocialUser();

	@Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 토큰을 재발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다.", content = @Content)
	})
	ApiResponse<RefreshTokenDto.Response> refresh(
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "토큰 재발급 요청",
			content = @Content(schema = @Schema(implementation = RefreshTokenDto.Request.class))
		)
		RefreshTokenDto.Request request
	);

	@Operation(summary = "로그아웃", description = "리프레시 토큰을 무효화합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다.", content = @Content)
	})
	ApiResponse<LogoutDto.Response> logout(
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			required = true,
			description = "로그아웃 요청",
			content = @Content(schema = @Schema(implementation = LogoutDto.Request.class))
		)
		LogoutDto.Request request
	);
}
