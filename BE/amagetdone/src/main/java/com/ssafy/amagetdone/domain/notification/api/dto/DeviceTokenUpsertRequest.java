package com.ssafy.amagetdone.domain.notification.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceTokenUpsertRequest(
	@NotBlank String installationId,
	@NotBlank String fcmToken,
	@NotBlank String platform
) {
}
