package com.ssafy.amagetdone.domain.member.api.controller;

import com.ssafy.amagetdone.domain.member.api.dto.LogoutDto;
import com.ssafy.amagetdone.domain.member.api.dto.RefreshTokenDto;
import com.ssafy.amagetdone.domain.member.api.dto.SocialAuthDto;
import com.ssafy.amagetdone.domain.member.application.service.MemberAuthService;
import com.ssafy.amagetdone.domain.member.application.service.MemberTokenService;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final MemberAuthService memberAuthService;
	private final MemberTokenService memberTokenService;

	public AuthController(MemberAuthService memberAuthService, MemberTokenService memberTokenService) {
		this.memberAuthService = memberAuthService;
		this.memberTokenService = memberTokenService;
	}

	@PostMapping("/social/{provider}")
	public SocialAuthDto.Response socialAuth(
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
		return memberAuthService.authenticate(socialProvider, request);
	}

	@PostMapping("/refresh")
	public RefreshTokenDto.Response refresh(
		@RequestBody RefreshTokenDto.Request request
	) {
		return memberTokenService.reissue(request.refreshToken());
	}

	@PostMapping("/logout")
	public LogoutDto.Response logout(
		@RequestBody LogoutDto.Request request
	) {
		return memberTokenService.logout(request.refreshToken());
	}
}
