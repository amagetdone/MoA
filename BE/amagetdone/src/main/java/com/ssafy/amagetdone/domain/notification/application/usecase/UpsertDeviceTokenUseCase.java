package com.ssafy.amagetdone.domain.notification.application.usecase;

import com.ssafy.amagetdone.domain.notification.api.dto.DeviceTokenUpsertRequest;
import com.ssafy.amagetdone.domain.notification.application.service.DeviceTokenService;
import com.ssafy.amagetdone.domain.notification.model.vo.DevicePlatform;

import org.springframework.stereotype.Component;

@Component
public class UpsertDeviceTokenUseCase {

	private final DeviceTokenService deviceTokenService;

	public UpsertDeviceTokenUseCase(DeviceTokenService deviceTokenService) {
		this.deviceTokenService = deviceTokenService;
	}

	public void execute(Integer userId, DeviceTokenUpsertRequest request) {
		deviceTokenService.upsert(
			userId,
			request.installationId(),
			request.fcmToken(),
			DevicePlatform.valueOf(request.platform().trim().toUpperCase())
		);
	}
}
