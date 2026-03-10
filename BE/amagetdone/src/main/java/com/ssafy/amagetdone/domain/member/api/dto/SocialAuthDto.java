package com.ssafy.amagetdone.domain.member.api.dto;

public final class SocialAuthDto {

	private SocialAuthDto() {
	}

	public record Request(
		String accessToken,
		String idToken
	) {
	}

	public record Response(
		Integer userId,
		String provider,
		boolean isNewUser,
		boolean profileCompleted,
		String nextAction,
		String accessToken,
		String refreshToken
	) {
	}
}
