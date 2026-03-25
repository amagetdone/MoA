package com.ssafy.amagetdone.domain.notification.application.listener;

import com.ssafy.amagetdone.domain.notification.application.event.PlanAiCompletedEvent;
import com.ssafy.amagetdone.domain.notification.application.service.DeviceTokenService;
import com.ssafy.amagetdone.domain.notification.application.service.PushNotificationService;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PlanAiNotificationListener {

	private final DeviceTokenService deviceTokenService;
	private final PushNotificationService pushNotificationService;

	public PlanAiNotificationListener(
		DeviceTokenService deviceTokenService,
		PushNotificationService pushNotificationService
	) {
		this.deviceTokenService = deviceTokenService;
		this.pushNotificationService = pushNotificationService;
	}

	@EventListener
	public void handlePlanAiCompleted(PlanAiCompletedEvent event) {
		deviceTokenService.findLatestActiveToken(event.userId())
			.ifPresent(deviceToken -> {
				PushNotificationService.SendResult result =
					pushNotificationService.sendPlanAiCompletedNotification(deviceToken, event);
				if (result == PushNotificationService.SendResult.INVALID_TOKEN && deviceToken.getId() != null) {
					deviceTokenService.disable(deviceToken.getId());
				}
			});
	}
}
