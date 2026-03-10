package com.ssafy.amagetdone.domain.member.support.util;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import com.ssafy.amagetdone.config.JwtProperties;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtAccessTokenReader {

	private final JwtProperties jwtProperties;
	private final RefreshTokenStore refreshTokenStore;

	public JwtAccessTokenReader(JwtProperties jwtProperties, RefreshTokenStore refreshTokenStore) {
		this.jwtProperties = jwtProperties;
		this.refreshTokenStore = refreshTokenStore;
	}

	public int readUserId(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			throw new MemberException(MemberErrorCode.AUTHORIZATION_HEADER_REQUIRED);
		}

		String token = authorizationHeader.substring("Bearer ".length()).trim();
		if (token.isEmpty()) {
			throw new MemberException(MemberErrorCode.AUTHORIZATION_HEADER_REQUIRED);
		}

		try {
			Claims claims = Jwts.parserBuilder()
				.setSigningKey(signingKey())
				.build()
				.parseClaimsJws(token)
				.getBody();

			if (!"access".equals(claims.get("tokenType", String.class))) {
				throw new MemberException(MemberErrorCode.ACCESS_TOKEN_REQUIRED);
			}

			int userId = Integer.parseInt(claims.getSubject());
			long tokenVersion = extractTokenVersion(claims);
			long currentVersion = refreshTokenStore.getTokenVersion(userId);
			if (tokenVersion != currentVersion) {
				throw new MemberException(MemberErrorCode.INVALID_ACCESS_TOKEN);
			}
			return userId;
		} catch (JwtException | IllegalArgumentException exception) {
			throw new MemberException(MemberErrorCode.INVALID_ACCESS_TOKEN, exception);
		}
	}

	private long extractTokenVersion(Claims claims) {
		Number tokenVersion = claims.get("tokenVersion", Number.class);
		if (tokenVersion == null) {
			throw new MemberException(MemberErrorCode.INVALID_ACCESS_TOKEN);
		}
		return tokenVersion.longValue();
	}

	private SecretKey signingKey() {
		return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
	}
}
