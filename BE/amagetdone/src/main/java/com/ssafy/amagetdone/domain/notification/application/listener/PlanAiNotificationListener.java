package com.ssafy.amagetdone.domain.notification.application.listener;

import com.ssafy.amagetdone.domain.notification.application.event.PlanAiCompletedEvent;
import com.ssafy.amagetdone.domain.notification.application.service.DeviceTokenService;
import com.ssafy.amagetdone.domain.notification.application.service.PushNotificationService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
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
		log.info(
			"Handling plan AI completed event. userId={}, planId={}, aiPipelineStatus={}",
			event.userId(),
			event.planId(),
			event.aiPipelineStatus()
		);
		deviceTokenService.findLatestActiveToken(event.userId())
			.ifPresent(deviceToken -> {
				PushNotificationService.SendResult result =
					pushNotificationService.sendPlanAiCompletedNotification(deviceToken, event);
				log.info(
					"Plan AI push result. userId={}, planId={}, deviceTokenId={}, platform={}, result={}",
					event.userId(),
					event.planId(),
					deviceToken.getId(),
					deviceToken.getPlatform(),
					result
				);
				if (result == PushNotificationService.SendResult.INVALID_TOKEN && deviceToken.getId() != null) {
					deviceTokenService.disable(deviceToken.getId());
				}
			});
	}
}
