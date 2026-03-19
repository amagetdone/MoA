package com.ssafy.amagetdone.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public final class RefreshTokenDto {

	private RefreshTokenDto() {
	}

	@Schema(name = "RefreshTokenRequest")
	public record Request(
		String refreshToken
	) {
	}

	@Schema(name = "RefreshTokenResponse")
	public record Response(
		String status,
		boolean account,
		boolean plan,
		String accessToken,
		String refreshToken
	) {
	}
}
