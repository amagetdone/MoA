package com.ssafy.amagetdone.domain.member.api.controller;

import com.ssafy.amagetdone.domain.account.application.usecase.SyncTestUserAssetsUseCase;
import com.ssafy.amagetdone.domain.member.api.dto.LogoutDto;
import com.ssafy.amagetdone.domain.member.api.dto.RefreshTokenDto;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.api.dto.TestSocialAuthDto;
import com.ssafy.amagetdone.domain.member.api.dto.TestSocialResetDto;
import com.ssafy.amagetdone.domain.member.api.dto.TestSocialSyncDto;
import com.ssafy.amagetdone.domain.member.application.service.MemberAuthService;
import com.ssafy.amagetdone.domain.member.application.service.MemberTokenService;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.member.support.util.JwtAccessTokenReader;
import com.ssafy.amagetdone.global.docs.controller.AuthDocsController;
import com.ssafy.amagetdone.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthDocsController {

	private final MemberAuthService memberAuthService;
	private final MemberTokenService memberTokenService;
	private final JwtAccessTokenReader jwtAccessTokenReader;
	private final SyncTestUserAssetsUseCase syncTestUserAssetsUseCase;

	@PostMapping("/social/{provider}")
	public ApiResponse<SocialAuthDto.Response> socialAuth(
		@PathVariable String provider,
		@RequestBody SocialAuthDto.Request request
	) {
		SocialProvider socialProvider;
		try {
			socialProvider = SocialProvider.from(provider);
		} catch (IllegalArgumentException exception) {
			throw new MemberException(MemberErrorCode.UNSUPPORTED_SOCIAL_PROVIDER, exception);
		}
		if (!socialProvider.isSupportedForSocialAuth()) {
			throw new MemberException(MemberErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
		}
		return ApiResponse.ok("소셜 로그인에 성공했습니다.", memberAuthService.authenticate(socialProvider, request));
	}

	@PostMapping("/social/test")
	public ApiResponse<SocialAuthDto.Response> socialTestAuth(
		@Valid @RequestBody(required = false) TestSocialAuthDto.Request request
	) {
		TestSocialAuthDto.Request testRequest = request == null ? TestSocialAuthDto.Request.defaultRequest() : request;
		return ApiResponse.ok("테스트 소셜 로그인에 성공했습니다.", memberAuthService.authenticateTest(testRequest));
	}

	@PostMapping("/social/test/reset")
	public ApiResponse<TestSocialResetDto.Response> resetTestSocialUser(
		@Parameter(hidden = true)
		@RequestHeader(name = "Authorization", required = false) String authorization
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		memberAuthService.resetTestUser(userId);
		return ApiResponse.ok("테스트 소셜 사용자를 초기화했습니다.", new TestSocialResetDto.Response(true));
	}

	@PostMapping("/social/test/sync-assets")
	public ApiResponse<TestSocialSyncDto.Response> syncTestSocialAssets(
		@Parameter(hidden = true)
		@RequestHeader(name = "Authorization", required = false) String authorization
	) {
		int userId = jwtAccessTokenReader.readUserId(authorization);
		syncTestUserAssetsUseCase.execute(userId);
		return ApiResponse.ok("테스트 금융 자산 동기화에 성공했습니다.", new TestSocialSyncDto.Response(true));
	}

	@PostMapping("/refresh")
	public ApiResponse<RefreshTokenDto.Response> refresh(
		@RequestBody RefreshTokenDto.Request request
	) {
		return ApiResponse.ok("토큰 재발급에 성공했습니다.", memberTokenService.reissue(request.refreshToken()));
	}

	@PostMapping("/logout")
	public ApiResponse<LogoutDto.Response> logout(
		@RequestBody LogoutDto.Request request
	) {
		return ApiResponse.ok("로그아웃에 성공했습니다.", memberTokenService.logout(request.refreshToken()));
	}
}
