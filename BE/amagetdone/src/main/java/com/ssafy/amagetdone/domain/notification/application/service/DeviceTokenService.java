package com.ssafy.amagetdone.domain.notification.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.notification.model.entity.DeviceToken;
import com.ssafy.amagetdone.domain.notification.model.repository.DeviceTokenRepository;
import com.ssafy.amagetdone.domain.notification.model.vo.DevicePlatform;

import jakarta.persistence.EntityManager;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
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
				token -> {
					token.refresh(fcmToken, platform, now);
					log.info(
						"Refreshed device token. userId={}, deviceTokenId={}, installationId={}, platform={}, tokenSuffix={}",
						userId,
						token.getId(),
						installationId,
						platform,
						maskTokenSuffix(fcmToken)
					);
				},
				() -> {
					DeviceToken created = deviceTokenRepository.save(DeviceToken.create(
						entityManager.getReference(User.class, userId),
						installationId,
						fcmToken,
						platform,
						now
					));
					log.info(
						"Registered new device token. userId={}, deviceTokenId={}, installationId={}, platform={}, tokenSuffix={}",
						userId,
						created.getId(),
						installationId,
						platform,
						maskTokenSuffix(fcmToken)
					);
				}
			);
	}

	@Transactional(readOnly = true)
	public Optional<DeviceToken> findLatestActiveToken(Integer userId) {
		Optional<DeviceToken> deviceToken =
			deviceTokenRepository.findFirstByUser_IdAndEnabledTrueAndDeletedFalseOrderByLastSeenAtDescIdDesc(userId);
		if (deviceToken.isPresent()) {
			DeviceToken token = deviceToken.get();
			log.info(
				"Found latest active device token. userId={}, deviceTokenId={}, installationId={}, platform={}, enabled={}",
				userId,
				token.getId(),
				token.getInstallationId(),
				token.getPlatform(),
				token.isEnabled()
			);
		} else {
			log.info("No active device token found for userId={}.", userId);
		}
		return deviceToken;
	}

	public void disable(Integer deviceTokenId) {
		deviceTokenRepository.findByIdAndDeletedFalse(deviceTokenId)
			.ifPresent(token -> {
				token.disable();
				log.warn(
					"Disabled device token after invalid FCM response. deviceTokenId={}, installationId={}, platform={}",
					deviceTokenId,
					token.getInstallationId(),
					token.getPlatform()
				);
			});
	}

	private String maskTokenSuffix(String token) {
		if (token == null || token.isBlank()) {
			return "empty";
		}
		int length = token.length();
		return length <= 8 ? token : token.substring(length - 8);
	}
}
