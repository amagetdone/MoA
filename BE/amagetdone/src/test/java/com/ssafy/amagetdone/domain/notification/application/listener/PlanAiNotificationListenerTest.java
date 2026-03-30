package com.ssafy.amagetdone.domain.notification.application.listener;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.notification.application.event.PlanAiCompletedEvent;
import com.ssafy.amagetdone.domain.notification.application.service.DeviceTokenService;
import com.ssafy.amagetdone.domain.notification.application.service.PushNotificationService;
import com.ssafy.amagetdone.domain.notification.model.entity.DeviceToken;
import com.ssafy.amagetdone.domain.notification.model.vo.DevicePlatform;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlanAiNotificationListenerTest {

	@Mock
	private DeviceTokenService deviceTokenService;

	@Mock
	private PushNotificationService pushNotificationService;

	@InjectMocks
	private PlanAiNotificationListener planAiNotificationListener;

	@Test
	void sendsToLatestActiveTokenWhenAvailable() {
		DeviceToken deviceToken = deviceToken(11, "token-1");
		PlanAiCompletedEvent event = event();
		when(deviceTokenService.findLatestActiveToken(1)).thenReturn(Optional.of(deviceToken));
		when(pushNotificationService.sendPlanAiCompletedNotification(deviceToken, event))
			.thenReturn(PushNotificationService.SendResult.SENT);

		planAiNotificationListener.handlePlanAiCompleted(event);

		verify(pushNotificationService).sendPlanAiCompletedNotification(deviceToken, event);
		verify(deviceTokenService, never()).disable(11);
	}

	@Test
	void skipsWhenNoActiveTokenExists() {
		when(deviceTokenService.findLatestActiveToken(1)).thenReturn(Optional.empty());

		planAiNotificationListener.handlePlanAiCompleted(event());

		verify(pushNotificationService, never()).sendPlanAiCompletedNotification(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	void disablesTokenWhenPushServiceReportsInvalidToken() {
		DeviceToken deviceToken = deviceToken(11, "token-1");
		PlanAiCompletedEvent event = event();
		when(deviceTokenService.findLatestActiveToken(1)).thenReturn(Optional.of(deviceToken));
		when(pushNotificationService.sendPlanAiCompletedNotification(deviceToken, event))
			.thenReturn(PushNotificationService.SendResult.INVALID_TOKEN);

		planAiNotificationListener.handlePlanAiCompleted(event);

		verify(deviceTokenService).disable(11);
	}

	private PlanAiCompletedEvent event() {
		return new PlanAiCompletedEvent(1, 7, "Europe trip fund", PlanAiPipelineStatus.SUCCEEDED);
	}

	private DeviceToken deviceToken(Integer id, String fcmToken) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 1);
		DeviceToken deviceToken = DeviceToken.create(
			user,
			"install-1",
			fcmToken,
			DevicePlatform.ANDROID,
			LocalDateTime.of(2026, 3, 24, 9, 0)
		);
		ReflectionTestUtils.setField(deviceToken, "id", id);
		return deviceToken;
	}
}
