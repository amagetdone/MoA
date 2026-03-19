package com.ssafy.amagetdone.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public final class TestSocialResetDto {

	private TestSocialResetDto() {
	}

	@Schema(name = "TestSocialResetResponse")
	public record Response(
		boolean success
	) {
	}
}
