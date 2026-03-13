package com.ssafy.amagetdone.domain.member.api.dto;

public final class RefreshTokenDto {

	private RefreshTokenDto() {
	}

	public record Request(
		String refreshToken
	) {
	}

	public record Response(
		String status,
		boolean account,
		boolean plan,
		String accessToken,
		String refreshToken
	) {
	}
}
