package com.ssafy.amagetdone.domain.notification.model.entity;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.notification.model.vo.DevicePlatform;
import com.ssafy.amagetdone.global.entity.BaseEntity;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "device_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "users_id", nullable = false)
	private User user;

	@Column(name = "installation_id", nullable = false, length = 120)
	private String installationId;

	@Column(name = "fcm_token", nullable = false, length = 512)
	private String fcmToken;

	@Enumerated(EnumType.STRING)
	@Column(name = "platform", nullable = false, length = 20)
	private DevicePlatform platform;

	@Column(name = "last_seen_at", nullable = false)
	private LocalDateTime lastSeenAt;

	@Column(name = "last_login_at", nullable = false)
	private LocalDateTime lastLoginAt;

	@ColumnDefault("TRUE")
	@Column(name = "enabled", nullable = false)
	private boolean enabled = true;

	public static DeviceToken create(
		User user,
		String installationId,
		String fcmToken,
		DevicePlatform platform,
		LocalDateTime now
	) {
		DeviceToken token = new DeviceToken();
		token.user = user;
		token.installationId = installationId;
		token.fcmToken = fcmToken;
		token.platform = platform;
		token.lastSeenAt = now;
		token.lastLoginAt = now;
		token.enabled = true;
		return token;
	}

	public void refresh(String fcmToken, DevicePlatform platform, LocalDateTime now) {
		this.fcmToken = fcmToken;
		this.platform = platform;
		this.lastSeenAt = now;
		this.lastLoginAt = now;
		this.enabled = true;
	}

	public void disable() {
		this.enabled = false;
	}
}
