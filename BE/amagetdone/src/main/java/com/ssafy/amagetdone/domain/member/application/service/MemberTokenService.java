package com.ssafy.amagetdone.domain.member.application.service;

import com.ssafy.amagetdone.domain.member.api.dto.LogoutDto;
import com.ssafy.amagetdone.domain.member.api.dto.RefreshTokenDto;
import com.ssafy.amagetdone.domain.member.application.usecase.LogoutMemberUseCase;
import com.ssafy.amagetdone.domain.member.application.usecase.ReissueMemberTokensUseCase;
import com.ssafy.amagetdone.domain.member.support.util.MemberTokenPair;

import org.springframework.stereotype.Service;

@Service
public class MemberTokenService {

	private final ReissueMemberTokensUseCase reissueMemberTokensUseCase;
	private final LogoutMemberUseCase logoutMemberUseCase;

	public MemberTokenService(
		ReissueMemberTokensUseCase reissueMemberTokensUseCase,
		LogoutMemberUseCase logoutMemberUseCase
	) {
		this.reissueMemberTokensUseCase = reissueMemberTokensUseCase;
		this.logoutMemberUseCase = logoutMemberUseCase;
	}

	public RefreshTokenDto.Response reissue(String refreshToken) {
		ReissueMemberTokensUseCase.Result result = reissueMemberTokensUseCase.execute(refreshToken);
		MemberTokenPair memberTokenPair = result.tokenPair();
		return new RefreshTokenDto.Response(
			result.status(),
			result.account(),
			result.plan(),
			memberTokenPair.accessToken(),
			memberTokenPair.refreshToken()
		);
	}

	public LogoutDto.Response logout(String refreshToken) {
		logoutMemberUseCase.execute(refreshToken);
		return new LogoutDto.Response(true);
	}
}
