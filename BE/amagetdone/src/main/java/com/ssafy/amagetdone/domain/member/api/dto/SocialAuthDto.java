package com.ssafy.amagetdone.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public final class SocialAuthDto {

	private SocialAuthDto() {
	}

	@Schema(name = "SocialAuthRequest")
	public record Request(
		String accessToken,
		String idToken
	) {
	}

	@Schema(name = "SocialAuthResponse")
	public record Response(
		String status,
		boolean account,
		boolean plan,
		String accessToken,
		String refreshToken
	) {
	}
}
