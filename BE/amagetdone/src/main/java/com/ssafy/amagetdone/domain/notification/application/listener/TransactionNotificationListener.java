package com.ssafy.amagetdone.domain.notification.application.listener;

import com.ssafy.amagetdone.domain.notification.application.event.TransactionSyncedEvent;
import com.ssafy.amagetdone.domain.notification.application.service.DeviceTokenService;
import com.ssafy.amagetdone.domain.notification.application.service.PushNotificationService;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TransactionNotificationListener {

	private final DeviceTokenService deviceTokenService;
	private final PushNotificationService pushNotificationService;

	public TransactionNotificationListener(
		DeviceTokenService deviceTokenService,
		PushNotificationService pushNotificationService
	) {
		this.deviceTokenService = deviceTokenService;
		this.pushNotificationService = pushNotificationService;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleTransactionSynced(TransactionSyncedEvent event) {
		deviceTokenService.findLatestActiveToken(event.userId())
			.ifPresent(deviceToken -> {
				PushNotificationService.SendResult result =
					pushNotificationService.sendTransactionNotification(deviceToken, event);
				if (result == PushNotificationService.SendResult.INVALID_TOKEN && deviceToken.getId() != null) {
					deviceTokenService.disable(deviceToken.getId());
				}
			});
	}
}
