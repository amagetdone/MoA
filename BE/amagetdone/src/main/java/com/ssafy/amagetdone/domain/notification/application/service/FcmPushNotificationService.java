package com.ssafy.amagetdone.domain.notification.application.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.ssafy.amagetdone.domain.notification.application.event.PlanAiCompletedEvent;
import com.ssafy.amagetdone.domain.notification.application.event.TransactionSyncedEvent;
import com.ssafy.amagetdone.domain.notification.model.entity.DeviceToken;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(FirebaseMessaging.class)
public class FcmPushNotificationService implements PushNotificationService {

	private final FirebaseMessaging firebaseMessaging;

	public FcmPushNotificationService(FirebaseMessaging firebaseMessaging) {
		this.firebaseMessaging = firebaseMessaging;
	}

	@Override
	public SendResult sendTransactionNotification(DeviceToken deviceToken, TransactionSyncedEvent event) {
		try {
			firebaseMessaging.send(Message.builder()
				.setToken(deviceToken.getFcmToken())
				.setNotification(Notification.builder()
					.setTitle("\uacb0\uc81c \uc54c\ub9bc")
					.setBody(buildBody(event))
					.build())
				.putAllData(buildData(event))
				.build());
			return SendResult.SENT;
		} catch (FirebaseMessagingException exception) {
			if (isInvalidToken(exception.getMessagingErrorCode())) {
				return SendResult.INVALID_TOKEN;
			}
			return SendResult.FAILED;
		}
	}

	@Override
	public SendResult sendPlanAiCompletedNotification(DeviceToken deviceToken, PlanAiCompletedEvent event) {
		try {
			firebaseMessaging.send(Message.builder()
				.setToken(deviceToken.getFcmToken())
				.setNotification(Notification.builder()
					.setTitle(buildPlanAiTitle(event))
					.setBody(buildPlanAiBody(event))
					.build())
				.putAllData(buildPlanAiData(event))
				.build());
			return SendResult.SENT;
		} catch (FirebaseMessagingException exception) {
			if (isInvalidToken(exception.getMessagingErrorCode())) {
				return SendResult.INVALID_TOKEN;
			}
			return SendResult.FAILED;
		}
	}

	private String buildBody(TransactionSyncedEvent event) {
		String merchant = event.merchantOrMemo();
		if (merchant == null || merchant.isBlank()) {
			return event.amount() + "\uc6d0 \uacb0\uc81c";
		}
		return merchant + " " + event.amount() + "\uc6d0 \uacb0\uc81c";
	}

	private Map<String, String> buildData(TransactionSyncedEvent event) {
		Map<String, String> data = new LinkedHashMap<>();
		data.put("type", "TRANSACTION_POSTED");
		data.put("sourceType", event.sourceType());
		data.put("transactionId", event.transactionId());
		data.put("amount", String.valueOf(event.amount()));
		data.put("occurredAt", String.valueOf(event.occurredAt()));
		return data;
	}

	private String buildPlanAiTitle(PlanAiCompletedEvent event) {
		return event.aiPipelineStatus() == PlanAiPipelineStatus.SUCCEEDED
			? "\ubaa9\ud45c \uc601\uc0c1 \uc0dd\uc131 \uc644\ub8cc"
			: "\ubaa9\ud45c \uc601\uc0c1 \uc0dd\uc131 \uc2e4\ud328";
	}

	private String buildPlanAiBody(PlanAiCompletedEvent event) {
		String planTitle = event.planTitle();
		if (planTitle == null || planTitle.isBlank()) {
			return event.aiPipelineStatus() == PlanAiPipelineStatus.SUCCEEDED
				? "\ubaa9\ud45c \uc601\uc0c1\uc774 \uc900\ube44\ub418\uc5c8\uc2b5\ub2c8\ub2e4."
				: "\ubaa9\ud45c \uc601\uc0c1 \uc0dd\uc131\uc5d0 \uc2e4\ud328\ud588\uc2b5\ub2c8\ub2e4.";
		}
		return event.aiPipelineStatus() == PlanAiPipelineStatus.SUCCEEDED
			? planTitle + " \ubaa9\ud45c \uc601\uc0c1\uc774 \uc900\ube44\ub418\uc5c8\uc2b5\ub2c8\ub2e4."
			: planTitle + " \ubaa9\ud45c \uc601\uc0c1 \uc0dd\uc131\uc5d0 \uc2e4\ud328\ud588\uc2b5\ub2c8\ub2e4.";
	}

	private Map<String, String> buildPlanAiData(PlanAiCompletedEvent event) {
		Map<String, String> data = new LinkedHashMap<>();
		data.put("type", "PLAN_AI_COMPLETED");
		data.put("planId", String.valueOf(event.planId()));
		data.put("aiPipelineStatus", event.aiPipelineStatus().name());
		return data;
	}

	private boolean isInvalidToken(MessagingErrorCode errorCode) {
		return errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT;
	}
}
