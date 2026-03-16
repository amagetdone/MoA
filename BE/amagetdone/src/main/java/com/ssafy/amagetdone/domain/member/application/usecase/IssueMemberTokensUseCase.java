package com.ssafy.amagetdone.domain.member.application.usecase;

import com.ssafy.amagetdone.global.config.JwtProperties;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.domain.member.support.util.JwtTokenIssuer;
import com.ssafy.amagetdone.domain.member.support.util.MemberTokenPair;

import org.springframework.stereotype.Component;

@Component
public class IssueMemberTokensUseCase {

	private final JwtTokenIssuer jwtTokenIssuer;
	private final RefreshTokenStore refreshTokenStore;
	private final JwtProperties jwtProperties;

	public IssueMemberTokensUseCase(
		JwtTokenIssuer jwtTokenIssuer,
		RefreshTokenStore refreshTokenStore,
		JwtProperties jwtProperties
	) {
		this.jwtTokenIssuer = jwtTokenIssuer;
		this.refreshTokenStore = refreshTokenStore;
		this.jwtProperties = jwtProperties;
	}

	public MemberTokenPair execute(User user, SocialProvider provider) {
		long tokenVersion = refreshTokenStore.getTokenVersion(user.getId());
		MemberTokenPair memberTokenPair = jwtTokenIssuer.issue(user.getId(), provider, tokenVersion);
		refreshTokenStore.save(user.getId(), memberTokenPair.refreshToken(), jwtProperties.getRefreshTokenExpirationSeconds());
		return memberTokenPair;
	}
}
