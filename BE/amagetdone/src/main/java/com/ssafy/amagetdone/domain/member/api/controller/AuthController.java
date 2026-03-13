package com.ssafy.amagetdone.domain.member.api.controller;

import com.ssafy.amagetdone.domain.member.api.dto.LogoutDto;
import com.ssafy.amagetdone.domain.member.api.dto.RefreshTokenDto;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.api.dto.TestSocialResetDto;
import com.ssafy.amagetdone.domain.member.application.service.MemberAuthService;
import com.ssafy.amagetdone.domain.member.application.service.MemberTokenService;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.global.response.ApiResponse;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

	private final MemberAuthService memberAuthService;
	private final MemberTokenService memberTokenService;

	public AuthController(MemberAuthService memberAuthService, MemberTokenService memberTokenService) {
		this.memberAuthService = memberAuthService;
		this.memberTokenService = memberTokenService;
	}

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
	public ApiResponse<SocialAuthDto.Response> socialTestAuth() {
		return ApiResponse.ok("테스트 소셜 로그인에 성공했습니다.", memberAuthService.authenticateTest());
	}

	@PostMapping("/social/test/reset")
	public ApiResponse<TestSocialResetDto.Response> resetTestSocialUser() {
		memberAuthService.resetTestUser();
		return ApiResponse.ok("테스트 소셜 사용자를 초기화했습니다.", new TestSocialResetDto.Response(true));
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
