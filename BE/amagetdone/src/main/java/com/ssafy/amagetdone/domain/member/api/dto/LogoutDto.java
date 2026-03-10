package com.ssafy.amagetdone.domain.member.api.dto;

public final class LogoutDto {

	private LogoutDto() {
	}

	public record Request(
		String refreshToken
	) {
	}

	public record Response(
		boolean success
	) {
	}
}
