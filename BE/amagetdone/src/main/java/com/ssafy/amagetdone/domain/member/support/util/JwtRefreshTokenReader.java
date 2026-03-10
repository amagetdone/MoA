package com.ssafy.amagetdone.domain.member.support.util;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import com.ssafy.amagetdone.config.JwtProperties;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.repository.RefreshTokenStore;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtRefreshTokenReader {

	private final JwtProperties jwtProperties;
	private final RefreshTokenStore refreshTokenStore;

	public JwtRefreshTokenReader(JwtProperties jwtProperties, RefreshTokenStore refreshTokenStore) {
		this.jwtProperties = jwtProperties;
		this.refreshTokenStore = refreshTokenStore;
	}

	public RefreshTokenClaims read(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new MemberException(MemberErrorCode.REFRESH_TOKEN_REQUIRED);
		}

		try {
			Claims claims = Jwts.parserBuilder()
				.setSigningKey(signingKey())
				.build()
				.parseClaimsJws(refreshToken)
				.getBody();

			if (!"refresh".equals(claims.get("tokenType", String.class))) {
				throw new MemberException(MemberErrorCode.REFRESH_TOKEN_REQUIRED);
			}

			Integer userId = Integer.parseInt(claims.getSubject());
			long tokenVersion = extractTokenVersion(claims);
			long currentVersion = refreshTokenStore.getTokenVersion(userId);
			if (tokenVersion != currentVersion) {
				throw new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN);
			}

			SocialProvider provider = SocialProvider.valueOf(claims.get("provider", String.class));
			return new RefreshTokenClaims(userId, provider, tokenVersion);
		} catch (MemberException exception) {
			throw exception;
		} catch (JwtException | IllegalArgumentException | NullPointerException exception) {
			throw new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN, exception);
		}
	}

	private long extractTokenVersion(Claims claims) {
		Number tokenVersion = claims.get("tokenVersion", Number.class);
		if (tokenVersion == null) {
			throw new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN);
		}
		return tokenVersion.longValue();
	}

	private SecretKey signingKey() {
		return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
	}

	public record RefreshTokenClaims(
		Integer userId,
		SocialProvider provider,
		long tokenVersion
	) {
	}
}
