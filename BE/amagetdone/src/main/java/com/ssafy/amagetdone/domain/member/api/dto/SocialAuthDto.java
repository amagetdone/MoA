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
		String status,
		boolean profile,
		boolean account,
		boolean plan,
		String accessToken,
		String refreshToken
	) {
	}
}
