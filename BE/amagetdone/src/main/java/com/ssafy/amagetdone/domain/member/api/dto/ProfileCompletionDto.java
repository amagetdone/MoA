package com.ssafy.amagetdone.domain.member.api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

public final class ProfileCompletionDto {

	private ProfileCompletionDto() {
	}

	@Schema(name = "ProfileCompletionRequest")
	public record Request(
		String nickname,
		String email,
		LocalDate birthDate,
		Boolean hasFixedIncome
	) {
	}

	@Schema(name = "ProfileCompletionResponse")
	public record Response(
		String status
	) {
	}
}
