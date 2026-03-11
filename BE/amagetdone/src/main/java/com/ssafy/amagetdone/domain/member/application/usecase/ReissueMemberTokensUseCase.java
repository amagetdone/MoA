package com.ssafy.amagetdone.domain.member.application.usecase;

import com.ssafy.amagetdone.config.JwtProperties;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.support.util.JwtRefreshTokenReader;
import com.ssafy.amagetdone.domain.member.support.util.JwtTokenIssuer;
import com.ssafy.amagetdone.domain.member.support.util.MemberTokenPair;

import org.springframework.stereotype.Component;

@Component
public class ReissueMemberTokensUseCase {

	private final RefreshTokenStore refreshTokenStore;
	private final JwtRefreshTokenReader jwtRefreshTokenReader;
	private final JwtTokenIssuer jwtTokenIssuer;
	private final JwtProperties jwtProperties;

	public ReissueMemberTokensUseCase(
		RefreshTokenStore refreshTokenStore,
		JwtRefreshTokenReader jwtRefreshTokenReader,
		JwtTokenIssuer jwtTokenIssuer,
		JwtProperties jwtProperties
	) {
		this.refreshTokenStore = refreshTokenStore;
		this.jwtRefreshTokenReader = jwtRefreshTokenReader;
		this.jwtTokenIssuer = jwtTokenIssuer;
		this.jwtProperties = jwtProperties;
	}

	public MemberTokenPair execute(String refreshToken) {
		JwtRefreshTokenReader.RefreshTokenClaims claims = jwtRefreshTokenReader.read(refreshToken);
		if (!refreshTokenStore.exists(claims.userId(), refreshToken)) {
			refreshTokenStore.invalidateAll(claims.userId());
			throw new MemberException(MemberErrorCode.REFRESH_TOKEN_REUSED);
		}

		MemberTokenPair memberTokenPair = jwtTokenIssuer.issue(
			claims.userId(),
			claims.provider(),
			claims.tokenVersion()
		);

		refreshTokenStore.delete(claims.userId(), refreshToken);
		refreshTokenStore.save(
			claims.userId(),
			memberTokenPair.refreshToken(),
			jwtProperties.getRefreshTokenExpirationSeconds()
		);
		return memberTokenPair;
	}
}
