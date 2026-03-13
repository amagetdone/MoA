package com.ssafy.amagetdone.domain.member.api.dto;

public final class TestSocialResetDto {

	private TestSocialResetDto() {
	}

	public record Response(
		boolean success
	) {
	}
}
