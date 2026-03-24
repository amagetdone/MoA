package com.ssafy.amagetdone.domain.notification.model.repository;

import java.util.Optional;

import com.ssafy.amagetdone.domain.notification.model.entity.DeviceToken;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Integer> {

	Optional<DeviceToken> findByUser_IdAndInstallationIdAndDeletedFalse(Integer userId, String installationId);

	Optional<DeviceToken> findFirstByUser_IdAndEnabledTrueAndDeletedFalseOrderByLastSeenAtDescIdDesc(Integer userId);

	Optional<DeviceToken> findByIdAndDeletedFalse(Integer id);
}
