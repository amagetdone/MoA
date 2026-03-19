package com.ssafy.amagetdone.domain.member.support.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import com.ssafy.amagetdone.global.config.JwtProperties;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenIssuer {

	private final JwtProperties jwtProperties;

	public JwtTokenIssuer(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}

	public MemberTokenPair issue(Integer userId, SocialProvider provider) {
		return issue(userId, provider, 0L);
	}

	public MemberTokenPair issue(Integer userId, SocialProvider provider, long tokenVersion) {
		return new MemberTokenPair(
			createToken(userId, provider, "access", tokenVersion, jwtProperties.getAccessTokenExpirationSeconds()),
			createToken(userId, provider, "refresh", tokenVersion, jwtProperties.getRefreshTokenExpirationSeconds())
		);
	}

	private String createToken(
		Integer userId,
		SocialProvider provider,
		String tokenType,
		long tokenVersion,
		long expirationSeconds
	) {
		Instant issuedAt = Instant.now();
		return Jwts.builder()
			.setSubject(String.valueOf(userId))
			.claim("provider", provider.name())
			.claim("tokenType", tokenType)
			.claim("tokenVersion", tokenVersion)
			.setIssuedAt(Date.from(issuedAt))
			.setExpiration(Date.from(issuedAt.plusSeconds(expirationSeconds)))
			.signWith(signingKey(), SignatureAlgorithm.HS256)
			.compact();
	}

	private SecretKey signingKey() {
		return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
	}
}
