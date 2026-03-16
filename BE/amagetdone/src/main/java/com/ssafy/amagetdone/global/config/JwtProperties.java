package com.ssafy.amagetdone.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

	private String secret;
	private long accessTokenExpirationSeconds = 3600L;
	private long refreshTokenExpirationSeconds = 1209600L;

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public long getAccessTokenExpirationSeconds() {
		return accessTokenExpirationSeconds;
	}

	public void setAccessTokenExpirationSeconds(long accessTokenExpirationSeconds) {
		this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
	}

	public long getRefreshTokenExpirationSeconds() {
		return refreshTokenExpirationSeconds;
	}

	public void setRefreshTokenExpirationSeconds(long refreshTokenExpirationSeconds) {
		this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
	}
}
