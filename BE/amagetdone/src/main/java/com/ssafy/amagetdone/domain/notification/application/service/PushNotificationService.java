package com.ssafy.amagetdone.domain.notification.application.service;

import com.ssafy.amagetdone.domain.notification.application.event.PlanAiCompletedEvent;
import com.ssafy.amagetdone.domain.notification.application.event.TransactionSyncedEvent;
import com.ssafy.amagetdone.domain.notification.model.entity.DeviceToken;

public interface PushNotificationService {

	SendResult sendTransactionNotification(DeviceToken deviceToken, TransactionSyncedEvent event);

	SendResult sendPlanAiCompletedNotification(DeviceToken deviceToken, PlanAiCompletedEvent event);

	enum SendResult {
		SENT,
		INVALID_TOKEN,
		FAILED,
		SKIPPED
	}
}
