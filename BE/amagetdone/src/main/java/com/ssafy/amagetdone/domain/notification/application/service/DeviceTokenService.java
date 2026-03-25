package com.ssafy.amagetdone.domain.notification.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.notification.model.entity.DeviceToken;
import com.ssafy.amagetdone.domain.notification.model.repository.DeviceTokenRepository;
import com.ssafy.amagetdone.domain.notification.model.vo.DevicePlatform;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeviceTokenService {

	private final DeviceTokenRepository deviceTokenRepository;
	private final EntityManager entityManager;
	private final Clock clock;

	public DeviceTokenService(
		DeviceTokenRepository deviceTokenRepository,
		EntityManager entityManager,
		Clock clock
	) {
		this.deviceTokenRepository = deviceTokenRepository;
		this.entityManager = entityManager;
		this.clock = clock;
	}

	public void upsert(Integer userId, String installationId, String fcmToken, DevicePlatform platform) {
		LocalDateTime now = LocalDateTime.now(clock);
		deviceTokenRepository.findByUser_IdAndInstallationIdAndDeletedFalse(userId, installationId)
			.ifPresentOrElse(
				token -> token.refresh(fcmToken, platform, now),
				() -> deviceTokenRepository.save(DeviceToken.create(
					entityManager.getReference(User.class, userId),
					installationId,
					fcmToken,
					platform,
					now
				))
			);
	}

	@Transactional(readOnly = true)
	public Optional<DeviceToken> findLatestActiveToken(Integer userId) {
		return deviceTokenRepository.findFirstByUser_IdAndEnabledTrueAndDeletedFalseOrderByLastSeenAtDescIdDesc(userId);
	}

	public void disable(Integer deviceTokenId) {
		deviceTokenRepository.findByIdAndDeletedFalse(deviceTokenId)
			.ifPresent(DeviceToken::disable);
	}
}
