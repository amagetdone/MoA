package com.ssafy.amagetdone.domain.notification.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import com.google.firebase.messaging.FirebaseMessaging;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.notification.application.event.TransactionSyncedEvent;
import com.ssafy.amagetdone.domain.notification.model.entity.DeviceToken;
import com.ssafy.amagetdone.domain.notification.model.vo.DevicePlatform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FcmPushNotificationServiceTest {

	@Mock
	private FirebaseMessaging firebaseMessaging;

	@InjectMocks
	private FcmPushNotificationService fcmPushNotificationService;

	@Test
	void sendTransactionNotificationBuildsAndSendsMessage() throws Exception {
		when(firebaseMessaging.send(any())).thenReturn("message-id");

		PushNotificationService.SendResult result = fcmPushNotificationService.sendTransactionNotification(
			deviceToken("token-1"),
			new TransactionSyncedEvent(
				1,
				"CARD",
				"txn-1",
				5400L,
				"Starbucks",
				LocalDateTime.of(2026, 3, 24, 9, 30)
			)
		);

		ArgumentCaptor<com.google.firebase.messaging.Message> captor =
			ArgumentCaptor.forClass(com.google.firebase.messaging.Message.class);
		verify(firebaseMessaging).send(captor.capture());
		assertEquals("token-1", ReflectionTestUtils.getField(captor.getValue(), "token"));
		@SuppressWarnings("unchecked")
		java.util.Map<String, String> data = (java.util.Map<String, String>)ReflectionTestUtils.getField(captor.getValue(), "data");
		assertEquals("TRANSACTION_POSTED", data.get("type"));
		assertEquals("5400", data.get("amount"));
		assertEquals(PushNotificationService.SendResult.SENT, result);
	}

	private DeviceToken deviceToken(String fcmToken) {
		User user = User.createPendingSocialUser();
		ReflectionTestUtils.setField(user, "id", 1);
		return DeviceToken.create(user, "install-1", fcmToken, DevicePlatform.ANDROID, LocalDateTime.of(2026, 3, 24, 9, 0));
	}
}
