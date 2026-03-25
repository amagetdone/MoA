package com.ssafy.amagetdone.domain.notification.application.listener;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.notification.application.event.TransactionSyncedEvent;
import com.ssafy.amagetdone.domain.notification.application.service.DeviceTokenService;
import com.ssafy.amagetdone.domain.notification.application.service.PushNotificationService;
import com.ssafy.amagetdone.domain.notification.model.entity.DeviceToken;
import com.ssafy.amagetdone.domain.notification.model.vo.DevicePlatform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class TransactionNotificationListenerTest {

	@Mock
	private DeviceTokenService deviceTokenService;

	@Mock
	private PushNotificationService pushNotificationService;

	@InjectMocks
	private TransactionNotificationListener transactionNotificationListener;

	@Test
	void sendsToLatestActiveTokenWhenAvailable() {
		DeviceToken deviceToken = deviceToken(11, "token-1");
		TransactionSyncedEvent event = event();
		when(deviceTokenService.findLatestActiveToken(1)).thenReturn(Optional.of(deviceToken));
		when(pushNotificationService.sendTransactionNotification(deviceToken, event))
			.thenReturn(PushNotificationService.SendResult.SENT);

		transactionNotificationListener.handleTransactionSynced(event);

		verify(pushNotificationService).sendTransactionNotification(deviceToken, event);
		verify(deviceTokenService, never()).disable(11);
	}

	@Test
	void skipsWhenNoActiveTokenExists() {
		when(deviceTokenService.findLatestActiveToken(1)).thenReturn(Optional.empty());

		transactionNotificationListener.handleTransactionSynced(event());

		verify(pushNotificationService, never()).sendTransactionNotification(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void disablesTokenWhenPushServiceReportsInvalidToken() {
		DeviceToken deviceToken = deviceToken(11, "token-1");
		TransactionSyncedEvent event = event();
		when(deviceTokenService.findLatestActiveToken(1)).thenReturn(Optional.of(deviceToken));
		when(pushNotificationService.sendTransactionNotification(deviceToken, event))
			.thenReturn(PushNotificationService.SendResult.INVALID_TOKEN);

		transactionNotificationListener.handleTransactionSynced(event);

		verify(deviceTokenService).disable(11);
	}

	@Test
	void handlesTransactionsAfterCommit() throws NoSuchMethodException {
		TransactionalEventListener annotation = TransactionNotificationListener.class
			.getMethod("handleTransactionSynced", TransactionSyncedEvent.class)
			.getAnnotation(TransactionalEventListener.class);

		org.assertj.core.api.Assertions.assertThat(annotation).isNotNull();
		org.assertj.core.api.Assertions.assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
	}

	private TransactionSyncedEvent event() {
		return new TransactionSyncedEvent(1, "CARD", "txn-1", 5400L, "Starbucks", LocalDateTime.of(2026, 3, 24, 9, 30));
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
