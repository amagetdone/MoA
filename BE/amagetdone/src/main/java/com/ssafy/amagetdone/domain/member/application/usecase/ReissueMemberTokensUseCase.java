package com.ssafy.amagetdone.domain.member.application.usecase;

import com.ssafy.amagetdone.config.JwtProperties;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
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
	private final UserRepository userRepository;
	private final AccountRepository accountRepository;
	private final PlanRepository planRepository;

	public ReissueMemberTokensUseCase(
		RefreshTokenStore refreshTokenStore,
		JwtRefreshTokenReader jwtRefreshTokenReader,
		JwtTokenIssuer jwtTokenIssuer,
		JwtProperties jwtProperties,
		UserRepository userRepository,
		AccountRepository accountRepository,
		PlanRepository planRepository
	) {
		this.refreshTokenStore = refreshTokenStore;
		this.jwtRefreshTokenReader = jwtRefreshTokenReader;
		this.jwtTokenIssuer = jwtTokenIssuer;
		this.jwtProperties = jwtProperties;
		this.userRepository = userRepository;
		this.accountRepository = accountRepository;
		this.planRepository = planRepository;
	}

	public Result execute(String refreshToken) {
		JwtRefreshTokenReader.RefreshTokenClaims claims = jwtRefreshTokenReader.read(refreshToken);
		if (!refreshTokenStore.exists(claims.userId(), refreshToken)) {
			refreshTokenStore.invalidateAll(claims.userId());
			throw new MemberException(MemberErrorCode.REFRESH_TOKEN_REUSED);
		}
		User user = userRepository.findById(claims.userId())
			.orElseThrow(() -> new MemberException(MemberErrorCode.USER_NOT_FOUND));

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

		return new Result(
			user.getStatus().name(),
			accountRepository.existsByUserId(claims.userId()),
			planRepository.existsByUserId(claims.userId()),
			memberTokenPair
		);
	}

	public record Result(
		String status,
		boolean account,
		boolean plan,
		MemberTokenPair tokenPair
	) {
	}
}
