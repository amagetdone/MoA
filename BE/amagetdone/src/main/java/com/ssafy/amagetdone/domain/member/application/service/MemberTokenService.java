package com.ssafy.amagetdone.domain.member.application.service;

import com.ssafy.amagetdone.domain.member.api.dto.LogoutDto;
import com.ssafy.amagetdone.domain.member.api.dto.RefreshTokenDto;
import com.ssafy.amagetdone.domain.member.application.usecase.LogoutMemberUseCase;
import com.ssafy.amagetdone.domain.member.application.usecase.ReissueMemberTokensUseCase;
import com.ssafy.amagetdone.domain.member.support.util.MemberTokenPair;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberTokenService {

	private final ReissueMemberTokensUseCase reissueMemberTokensUseCase;
	private final LogoutMemberUseCase logoutMemberUseCase;

	public RefreshTokenDto.Response reissue(String refreshToken) {
		MemberTokenPair memberTokenPair = reissueMemberTokensUseCase.execute(refreshToken);
		return new RefreshTokenDto.Response(memberTokenPair.accessToken(), memberTokenPair.refreshToken());
	}

	public LogoutDto.Response logout(String refreshToken) {
		logoutMemberUseCase.execute(refreshToken);
		return new LogoutDto.Response(true);
	}
}
