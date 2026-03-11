package com.ssafy.amagetdone.domain.member.api.dto;

import java.time.LocalDate;

public final class ProfileCompletionDto {

	private ProfileCompletionDto() {
	}

	public record Request(
		String nickname,
		String email,
		LocalDate birthDate,
		Boolean hasFixedIncome
	) {
	}

	public record Response(
		Integer userId,
		boolean profileCompleted,
		String status,
		String nextAction
	) {
	}
}
