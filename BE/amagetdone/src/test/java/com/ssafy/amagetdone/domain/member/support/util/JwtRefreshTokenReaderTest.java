package com.ssafy.amagetdone.domain.member.support.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.ssafy.amagetdone.config.JwtProperties;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtRefreshTokenReaderTest {

	@Mock
	private RefreshTokenStore refreshTokenStore;

	@Test
	void readReturnsClaimsForRefreshTokenWhenVersionMatches() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("01234567890123456789012345678901");
		JwtTokenIssuer jwtTokenIssuer = new JwtTokenIssuer(properties);
		JwtRefreshTokenReader jwtRefreshTokenReader = new JwtRefreshTokenReader(properties, refreshTokenStore);
		MemberTokenPair tokenPair = jwtTokenIssuer.issue(123, SocialProvider.KAKAO, 0L);
		given(refreshTokenStore.getTokenVersion(123)).willReturn(0L);

		JwtRefreshTokenReader.RefreshTokenClaims claims = jwtRefreshTokenReader.read(tokenPair.refreshToken());

		assertEquals(123, claims.userId());
		assertEquals(SocialProvider.KAKAO, claims.provider());
		assertEquals(0L, claims.tokenVersion());
	}

	@Test
	void readRejectsAccessToken() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("01234567890123456789012345678901");
		JwtTokenIssuer jwtTokenIssuer = new JwtTokenIssuer(properties);
		JwtRefreshTokenReader jwtRefreshTokenReader = new JwtRefreshTokenReader(properties, refreshTokenStore);
		MemberTokenPair tokenPair = jwtTokenIssuer.issue(123, SocialProvider.KAKAO, 0L);

		MemberException exception = assertThrows(
			MemberException.class,
			() -> jwtRefreshTokenReader.read(tokenPair.accessToken())
		);

		assertEquals(MemberErrorCode.REFRESH_TOKEN_REQUIRED, exception.getErrorCode());
	}

	@Test
	void readRejectsVersionMismatch() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("01234567890123456789012345678901");
		JwtTokenIssuer jwtTokenIssuer = new JwtTokenIssuer(properties);
		JwtRefreshTokenReader jwtRefreshTokenReader = new JwtRefreshTokenReader(properties, refreshTokenStore);
		MemberTokenPair tokenPair = jwtTokenIssuer.issue(123, SocialProvider.KAKAO, 0L);
		given(refreshTokenStore.getTokenVersion(123)).willReturn(1L);

		MemberException exception = assertThrows(
			MemberException.class,
			() -> jwtRefreshTokenReader.read(tokenPair.refreshToken())
		);

		assertEquals(MemberErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
	}
}
