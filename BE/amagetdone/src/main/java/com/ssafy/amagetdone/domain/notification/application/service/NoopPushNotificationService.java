package com.ssafy.amagetdone.domain.notification.application.service;

import com.ssafy.amagetdone.domain.notification.application.event.PlanAiCompletedEvent;
import com.google.firebase.messaging.FirebaseMessaging;
import com.ssafy.amagetdone.domain.notification.application.event.TransactionSyncedEvent;
import com.ssafy.amagetdone.domain.notification.model.entity.DeviceToken;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnMissingBean(FirebaseMessaging.class)
public class NoopPushNotificationService implements PushNotificationService {

	public NoopPushNotificationService() {
		log.warn(
			"FCM push notifications are disabled because FirebaseMessaging bean is missing. "
				+ "Check FIREBASE_ENABLED and FIREBASE_CREDENTIALS_PATH in the deployment environment."
		);
	}

	@Override
	public SendResult sendTransactionNotification(DeviceToken deviceToken, TransactionSyncedEvent event) {
		log.warn(
			"Skipping transaction push. userId={}, deviceTokenId={}, platform={}, reason=no FirebaseMessaging bean",
			event.userId(),
			deviceToken.getId(),
			deviceToken.getPlatform()
		);
		return SendResult.SKIPPED;
	}

	@Override
	public SendResult sendPlanAiCompletedNotification(DeviceToken deviceToken, PlanAiCompletedEvent event) {
		log.warn(
			"Skipping plan AI push. userId={}, planId={}, deviceTokenId={}, platform={}, reason=no FirebaseMessaging bean",
			event.userId(),
			event.planId(),
			deviceToken.getId(),
			deviceToken.getPlatform()
		);
		return SendResult.SKIPPED;
	}
}
