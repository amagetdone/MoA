package com.ssafy.amagetdone.domain.notification.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.notification.model.entity.DeviceToken;
import com.ssafy.amagetdone.domain.notification.model.repository.DeviceTokenRepository;
import com.ssafy.amagetdone.domain.notification.model.vo.DevicePlatform;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

	@Mock
	private DeviceTokenRepository deviceTokenRepository;

	@Mock
	private EntityManager entityManager;

	private final Clock clock = Clock.fixed(Instant.parse("2026-03-24T00:00:00Z"), ZoneId.of("Asia/Seoul"));

	private DeviceTokenService deviceTokenService;

	@BeforeEach
	void setUp() {
		deviceTokenService = new DeviceTokenService(deviceTokenRepository, entityManager, clock);
	}

	@Test
	void upsertCreatesNewTokenWhenInstallationIsUnknown() {
		User user = User.createPendingSocialUser();
		DeviceToken savedToken = instantiateDeviceToken();
		ReflectionTestUtils.setField(savedToken, "id", 101);
		ReflectionTestUtils.setField(savedToken, "installationId", "install-1");
		ReflectionTestUtils.setField(savedToken, "fcmToken", "token-1");
		ReflectionTestUtils.setField(savedToken, "platform", DevicePlatform.ANDROID);
		ReflectionTestUtils.setField(savedToken, "enabled", true);
		given(entityManager.getReference(User.class, 1)).willReturn(user);
		given(deviceTokenRepository.findByUser_IdAndInstallationIdAndDeletedFalse(1, "install-1"))
			.willReturn(Optional.empty());
		given(deviceTokenRepository.save(any(DeviceToken.class))).willReturn(savedToken);

		deviceTokenService.upsert(1, "install-1", "token-1", DevicePlatform.ANDROID);

		verify(deviceTokenRepository).save(org.mockito.ArgumentMatchers.argThat(token ->
			"install-1".equals(token.getInstallationId())
				&& "token-1".equals(token.getFcmToken())
				&& token.getPlatform() == DevicePlatform.ANDROID
				&& token.isEnabled()
		));
	}

	@Test
	void upsertRefreshesExistingInstallation() {
		DeviceToken token = instantiateDeviceToken();
		ReflectionTestUtils.setField(token, "installationId", "install-1");
		ReflectionTestUtils.setField(token, "fcmToken", "old-token");
		ReflectionTestUtils.setField(token, "platform", DevicePlatform.IOS);
		ReflectionTestUtils.setField(token, "enabled", false);

		given(deviceTokenRepository.findByUser_IdAndInstallationIdAndDeletedFalse(1, "install-1"))
			.willReturn(Optional.of(token));

		deviceTokenService.upsert(1, "install-1", "new-token", DevicePlatform.ANDROID);

		assertEquals("new-token", token.getFcmToken());
		assertEquals(DevicePlatform.ANDROID, token.getPlatform());
		assertEquals(LocalDateTime.of(2026, 3, 24, 9, 0), token.getLastSeenAt());
		assertEquals(LocalDateTime.of(2026, 3, 24, 9, 0), token.getLastLoginAt());
		assertEquals(true, token.isEnabled());
	}

	@Test
	void findLatestActiveTokenReturnsRepositoryResult() {
		DeviceToken token = instantiateDeviceToken();
		given(deviceTokenRepository.findFirstByUser_IdAndEnabledTrueAndDeletedFalseOrderByLastSeenAtDescIdDesc(1))
			.willReturn(Optional.of(token));

		Optional<DeviceToken> result = deviceTokenService.findLatestActiveToken(1);

		assertSame(token, result.orElseThrow());
	}

	@Test
	void disableMarksTokenDisabled() {
		DeviceToken token = instantiateDeviceToken();
		ReflectionTestUtils.setField(token, "enabled", true);
		given(deviceTokenRepository.findByIdAndDeletedFalse(10)).willReturn(Optional.of(token));

		deviceTokenService.disable(10);

		assertFalse(token.isEnabled());
	}

	private DeviceToken instantiateDeviceToken() {
		try {
			var constructor = DeviceToken.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
