package com.ssafy.amagetdone.domain.notification.application.service;

import com.ssafy.amagetdone.domain.notification.application.event.TransactionSyncedEvent;
import com.ssafy.amagetdone.domain.notification.model.entity.DeviceToken;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(PushNotificationService.class)
public class NoopPushNotificationService implements PushNotificationService {

	@Override
	public SendResult sendTransactionNotification(DeviceToken deviceToken, TransactionSyncedEvent event) {
		return SendResult.SKIPPED;
	}
}
