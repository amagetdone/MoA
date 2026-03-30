package com.ssafy.amagetdone.domain.notification.application.usecase;

import static org.mockito.Mockito.verify;

import com.ssafy.amagetdone.domain.notification.api.dto.DeviceTokenUpsertRequest;
import com.ssafy.amagetdone.domain.notification.application.service.DeviceTokenService;
import com.ssafy.amagetdone.domain.notification.model.vo.DevicePlatform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpsertDeviceTokenUseCaseTest {

	@Mock
	private DeviceTokenService deviceTokenService;

	@InjectMocks
	private UpsertDeviceTokenUseCase upsertDeviceTokenUseCase;

	@Test
	void upsertDelegatesToDeviceTokenService() {
		upsertDeviceTokenUseCase.execute(1, new DeviceTokenUpsertRequest("install-1", "token-1", "ANDROID"));

		verify(deviceTokenService).upsert(1, "install-1", "token-1", DevicePlatform.ANDROID);
	}
}
