package com.ssafy.amagetdone.domain.notification.api.controller;

import com.ssafy.amagetdone.domain.notification.api.dto.DeviceTokenUpsertRequest;
import com.ssafy.amagetdone.domain.notification.application.usecase.UpsertDeviceTokenUseCase;
import com.ssafy.amagetdone.global.response.ApiResponse;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/device-tokens")
public class DeviceTokenController {

	private final UpsertDeviceTokenUseCase upsertDeviceTokenUseCase;

	public DeviceTokenController(UpsertDeviceTokenUseCase upsertDeviceTokenUseCase) {
		this.upsertDeviceTokenUseCase = upsertDeviceTokenUseCase;
	}

	@PostMapping
	public ApiResponse<Void> upsert(
		@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
		@Valid @RequestBody DeviceTokenUpsertRequest request
	) {
		upsertDeviceTokenUseCase.execute(authenticatedMember.userId(), request);
		return ApiResponse.ok("FCM \ud1a0\ud070\uc774 \ub4f1\ub85d\ub418\uc5c8\uc2b5\ub2c8\ub2e4.", null);
	}
}
