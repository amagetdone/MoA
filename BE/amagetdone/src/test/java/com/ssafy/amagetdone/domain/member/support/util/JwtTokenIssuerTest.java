package com.ssafy.amagetdone.domain.member.support.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import com.ssafy.amagetdone.global.config.JwtProperties;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtTokenIssuerTest {

	@Test
	void issueCreatesAccessAndRefreshTokensWithExpectedClaims() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("01234567890123456789012345678901");
		properties.setAccessTokenExpirationSeconds(1800L);
		properties.setRefreshTokenExpirationSeconds(1209600L);
		JwtTokenIssuer jwtTokenIssuer = new JwtTokenIssuer(properties);

		MemberTokenPair tokenPair = jwtTokenIssuer.issue(123, SocialProvider.KAKAO);

		assertFalse(tokenPair.accessToken().isBlank());
		assertFalse(tokenPair.refreshToken().isBlank());

		Claims accessClaims = parseClaims(tokenPair.accessToken(), properties);
		assertEquals("123", accessClaims.getSubject());
		assertEquals("KAKAO", accessClaims.get("provider", String.class));
		assertEquals("access", accessClaims.get("tokenType", String.class));

		Claims refreshClaims = parseClaims(tokenPair.refreshToken(), properties);
		assertEquals("123", refreshClaims.getSubject());
		assertEquals("refresh", refreshClaims.get("tokenType", String.class));
	}

	private Claims parseClaims(String token, JwtProperties properties) {
		SecretKey key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
		return Jwts.parserBuilder()
			.setSigningKey(key)
			.build()
			.parseClaimsJws(token)
			.getBody();
	}
}
