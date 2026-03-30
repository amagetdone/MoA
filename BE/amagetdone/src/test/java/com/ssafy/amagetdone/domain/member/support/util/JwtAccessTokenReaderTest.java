package com.ssafy.amagetdone.domain.member.support.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.ssafy.amagetdone.global.config.JwtProperties;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtAccessTokenReaderTest {

	@Mock
	private RefreshTokenStore refreshTokenStore;

	@Test
	void readUserIdReturnsSubjectFromAccessToken() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("01234567890123456789012345678901");
		JwtTokenIssuer jwtTokenIssuer = new JwtTokenIssuer(properties);
		JwtAccessTokenReader jwtAccessTokenReader = new JwtAccessTokenReader(properties, refreshTokenStore);
		MemberTokenPair tokenPair = jwtTokenIssuer.issue(123, SocialProvider.KAKAO, 0L);
		given(refreshTokenStore.getTokenVersion(123)).willReturn(0L);

		int userId = jwtAccessTokenReader.readUserId("Bearer " + tokenPair.accessToken());

		assertEquals(123, userId);
	}

	@Test
	void readUserIdRejectsRefreshToken() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("01234567890123456789012345678901");
		JwtTokenIssuer jwtTokenIssuer = new JwtTokenIssuer(properties);
		JwtAccessTokenReader jwtAccessTokenReader = new JwtAccessTokenReader(properties, refreshTokenStore);
		MemberTokenPair tokenPair = jwtTokenIssuer.issue(123, SocialProvider.KAKAO, 0L);

		MemberException exception = assertThrows(
			MemberException.class,
			() -> jwtAccessTokenReader.readUserId("Bearer " + tokenPair.refreshToken())
		);
		assertEquals(MemberErrorCode.ACCESS_TOKEN_REQUIRED, exception.getErrorCode());
	}

	@Test
	void readUserIdRejectsMissingBearerHeader() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("01234567890123456789012345678901");
		JwtAccessTokenReader jwtAccessTokenReader = new JwtAccessTokenReader(properties, refreshTokenStore);

		MemberException exception = assertThrows(
			MemberException.class,
			() -> jwtAccessTokenReader.readUserId(null)
		);
		assertEquals(MemberErrorCode.AUTHORIZATION_HEADER_REQUIRED, exception.getErrorCode());
	}

	@Test
	void readUserIdRejectsVersionMismatch() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("01234567890123456789012345678901");
		JwtTokenIssuer jwtTokenIssuer = new JwtTokenIssuer(properties);
		JwtAccessTokenReader jwtAccessTokenReader = new JwtAccessTokenReader(properties, refreshTokenStore);
		MemberTokenPair tokenPair = jwtTokenIssuer.issue(123, SocialProvider.KAKAO, 0L);
		given(refreshTokenStore.getTokenVersion(123)).willReturn(1L);

		MemberException exception = assertThrows(
			MemberException.class,
			() -> jwtAccessTokenReader.readUserId("Bearer " + tokenPair.accessToken())
		);

		assertEquals(MemberErrorCode.INVALID_ACCESS_TOKEN, exception.getErrorCode());
	}
}
