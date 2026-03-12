package com.ssafy.amagetdone.domain.member.model.vo;

import java.util.Locale;

public enum SocialProvider {
	GOOGLE,
	KAKAO,
	NAVER,
	TEST;

	public static SocialProvider from(String value) {
		try {
			return SocialProvider.valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Unsupported social provider: " + value, exception);
		}
	}

	public boolean isSupportedForSocialAuth() {
		return this == GOOGLE || this == KAKAO;
	}

	public String apiValue() {
		return name().toLowerCase(Locale.ROOT);
	}
}
