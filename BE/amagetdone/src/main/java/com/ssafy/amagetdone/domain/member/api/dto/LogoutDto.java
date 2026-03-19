package com.ssafy.amagetdone.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public final class LogoutDto {

	private LogoutDto() {
	}

	@Schema(name = "LogoutRequest")
	public record Request(
		String refreshToken
	) {
	}

	@Schema(name = "LogoutResponse")
	public record Response(
		boolean success
	) {
	}
}
