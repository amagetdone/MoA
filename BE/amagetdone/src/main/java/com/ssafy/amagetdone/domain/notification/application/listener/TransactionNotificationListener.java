package com.ssafy.amagetdone.domain.notification.application.listener;

import com.ssafy.amagetdone.domain.notification.application.event.TransactionSyncedEvent;
import com.ssafy.amagetdone.domain.notification.application.service.DeviceTokenService;
import com.ssafy.amagetdone.domain.notification.application.service.PushNotificationService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
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
		log.info(
			"Handling transaction synced event. userId={}, sourceType={}, transactionId={}, amount={}",
			event.userId(),
			event.sourceType(),
			event.transactionId(),
			event.amount()
		);
		if (isVerificationAlarmTarget(event)) {
			PushNotificationService.SendResult demoResult =
				pushNotificationService.sendVerificationAlarmDemoNotification(event.merchantOrMemo());
			log.info(
				"Verification demo push result. userId={}, transactionId={}, result={}",
				event.userId(),
				event.transactionId(),
				demoResult
			);
		}
		deviceTokenService.findLatestActiveToken(event.userId())
			.ifPresent(deviceToken -> {
				PushNotificationService.SendResult result =
					pushNotificationService.sendTransactionNotification(deviceToken, event);
				log.info(
					"Transaction push result. userId={}, deviceTokenId={}, platform={}, result={}",
					event.userId(),
					deviceToken.getId(),
					deviceToken.getPlatform(),
					result
				);
				if (result == PushNotificationService.SendResult.INVALID_TOKEN && deviceToken.getId() != null) {
					deviceTokenService.disable(deviceToken.getId());
				}
			});
	}

	private boolean isVerificationAlarmTarget(TransactionSyncedEvent event) {
		if (!"입금".equals(event.transactionTypeName())) {
			return false;
		}
		String merchantOrMemo = event.merchantOrMemo();
		if (merchantOrMemo == null) {
			return false;
		}
		String normalized = merchantOrMemo.trim().toUpperCase();
		return normalized.startsWith("AMAGETDONE");
	}
}
