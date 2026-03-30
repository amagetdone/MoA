package com.ssafy.amagetdone.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public final class TestSocialSyncDto {

	private TestSocialSyncDto() {
	}

	@Schema(name = "TestSocialSyncResponse")
	public record Response(
		boolean success
	) {
	}
}
