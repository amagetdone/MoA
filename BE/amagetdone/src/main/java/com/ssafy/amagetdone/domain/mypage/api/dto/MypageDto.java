package com.ssafy.amagetdone.domain.mypage.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public final class MypageDto {

	private MypageDto() {
	}

	@Schema(name = "MypageResponse")
	public record Response(
		String userName,
		String userEmail,
		String profileImageUrl,
		long activePlanCount,
		long completedPlanCount,
		long totalSavedAmount
	) {
	}
}
