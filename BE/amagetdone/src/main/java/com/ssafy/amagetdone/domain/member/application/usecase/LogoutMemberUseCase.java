package com.ssafy.amagetdone.domain.member.application.usecase;

import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.support.util.JwtRefreshTokenReader;

import org.springframework.stereotype.Component;

@Component
public class LogoutMemberUseCase {

	private final RefreshTokenStore refreshTokenStore;
	private final JwtRefreshTokenReader jwtRefreshTokenReader;

	public LogoutMemberUseCase(
		RefreshTokenStore refreshTokenStore,
		JwtRefreshTokenReader jwtRefreshTokenReader
	) {
		this.refreshTokenStore = refreshTokenStore;
		this.jwtRefreshTokenReader = jwtRefreshTokenReader;
	}

	public void execute(String refreshToken) {
		JwtRefreshTokenReader.RefreshTokenClaims claims = jwtRefreshTokenReader.read(refreshToken);
		refreshTokenStore.invalidateAll(claims.userId());
	}
}
