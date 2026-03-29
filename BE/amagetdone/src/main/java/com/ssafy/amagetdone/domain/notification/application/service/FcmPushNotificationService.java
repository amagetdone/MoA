package com.ssafy.amagetdone.domain.notification.application.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.ssafy.amagetdone.domain.notification.application.event.PlanAiCompletedEvent;
import com.ssafy.amagetdone.domain.notification.application.event.TransactionSyncedEvent;
import com.ssafy.amagetdone.domain.notification.model.entity.DeviceToken;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnBean(FirebaseMessaging.class)
public class FcmPushNotificationService implements PushNotificationService {

	private static final String VERIFICATION_ALARM_DEMO_TOPIC = "verification_alarm_demo";
	private static final String VERIFICATION_ALARM_DEMO_CHANNEL_ID = "bank_alarm_demo";
	private static final Pattern VERIFICATION_TEXT_PATTERN = Pattern.compile("(?i)(AMAGETDONE)\\s*(\\d{4})");

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
					.setTitle(buildTransactionTitle(event))
					.setBody(buildBody(event))
					.build())
				.putAllData(buildData(event))
				.build());
			log.info(
				"FCM transaction push sent. userId={}, deviceTokenId={}, platform={}, tokenSuffix={}, transactionId={}",
				event.userId(),
				deviceToken.getId(),
				deviceToken.getPlatform(),
				maskTokenSuffix(deviceToken.getFcmToken()),
				event.transactionId()
			);
			return SendResult.SENT;
		} catch (FirebaseMessagingException exception) {
			if (isInvalidToken(exception.getMessagingErrorCode())) {
				log.warn(
					"FCM transaction push returned invalid token. userId={}, deviceTokenId={}, platform={}, tokenSuffix={}, errorCode={}",
					event.userId(),
					deviceToken.getId(),
					deviceToken.getPlatform(),
					maskTokenSuffix(deviceToken.getFcmToken()),
					exception.getMessagingErrorCode()
				);
				return SendResult.INVALID_TOKEN;
			}
			log.error(
				"FCM transaction push failed. userId={}, deviceTokenId={}, platform={}, tokenSuffix={}, errorCode={}",
				event.userId(),
				deviceToken.getId(),
				deviceToken.getPlatform(),
				maskTokenSuffix(deviceToken.getFcmToken()),
				exception.getMessagingErrorCode(),
				exception
			);
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
			log.info(
				"FCM plan AI push sent. userId={}, planId={}, deviceTokenId={}, platform={}, tokenSuffix={}, aiPipelineStatus={}",
				event.userId(),
				event.planId(),
				deviceToken.getId(),
				deviceToken.getPlatform(),
				maskTokenSuffix(deviceToken.getFcmToken()),
				event.aiPipelineStatus()
			);
			return SendResult.SENT;
		} catch (FirebaseMessagingException exception) {
			if (isInvalidToken(exception.getMessagingErrorCode())) {
				log.warn(
					"FCM plan AI push returned invalid token. userId={}, planId={}, deviceTokenId={}, platform={}, tokenSuffix={}, errorCode={}",
					event.userId(),
					event.planId(),
					deviceToken.getId(),
					deviceToken.getPlatform(),
					maskTokenSuffix(deviceToken.getFcmToken()),
					exception.getMessagingErrorCode()
				);
				return SendResult.INVALID_TOKEN;
			}
			log.error(
				"FCM plan AI push failed. userId={}, planId={}, deviceTokenId={}, platform={}, tokenSuffix={}, errorCode={}",
				event.userId(),
				event.planId(),
				deviceToken.getId(),
				deviceToken.getPlatform(),
				maskTokenSuffix(deviceToken.getFcmToken()),
				exception.getMessagingErrorCode(),
				exception
			);
			return SendResult.FAILED;
		}
	}

	@Override
	public SendResult sendVerificationAlarmDemoNotification(String verificationText) {
		try {
			firebaseMessaging.send(Message.builder()
				.setTopic(VERIFICATION_ALARM_DEMO_TOPIC)
				.setNotification(Notification.builder()
					.setTitle("1원 입금")
					.setBody(buildVerificationAlarmBody(verificationText))
					.build())
				.setAndroidConfig(AndroidConfig.builder()
					.setPriority(AndroidConfig.Priority.HIGH)
					.setNotification(AndroidNotification.builder()
						.setChannelId(VERIFICATION_ALARM_DEMO_CHANNEL_ID)
						.setPriority(AndroidNotification.Priority.MAX)
						.setSound("default")
						.build())
					.build())
				.putAllData(buildVerificationAlarmData(verificationText))
				.build());
			log.info(
				"FCM verification demo push sent. topic={}, verificationText={}",
				VERIFICATION_ALARM_DEMO_TOPIC
				, verificationText
			);
			return SendResult.SENT;
		} catch (FirebaseMessagingException exception) {
			log.error(
				"FCM verification demo push failed. topic={}, verificationText={}, errorCode={}",
				VERIFICATION_ALARM_DEMO_TOPIC,
				verificationText,
				exception.getMessagingErrorCode(),
				exception
			);
			return SendResult.FAILED;
		}
	}

	private String buildTransactionTitle(TransactionSyncedEvent event) {
		return resolveTransactionLabel(event) + " \uc54c\ub9bc";
	}

	private String buildBody(TransactionSyncedEvent event) {
		String transactionLabel = resolveTransactionLabel(event);
		String merchant = event.merchantOrMemo();
		if (merchant == null || merchant.isBlank()) {
			return event.amount() + "\uc6d0 " + transactionLabel;
		}
		return merchant + " " + event.amount() + "\uc6d0 " + transactionLabel;
	}

	private Map<String, String> buildData(TransactionSyncedEvent event) {
		Map<String, String> data = new LinkedHashMap<>();
		data.put("type", "TRANSACTION_POSTED");
		data.put("sourceType", event.sourceType());
		data.put("transactionId", event.transactionId());
		data.put("amount", String.valueOf(event.amount()));
		data.put("occurredAt", String.valueOf(event.occurredAt()));
		if (event.transactionTypeName() != null && !event.transactionTypeName().isBlank()) {
			data.put("transactionTypeName", event.transactionTypeName());
		}
		return data;
	}

	private String resolveTransactionLabel(TransactionSyncedEvent event) {
		if ("\uc785\uae08".equals(event.transactionTypeName())) {
			return "\uc785\uae08";
		}
		if ("\ucd9c\uae08".equals(event.transactionTypeName())) {
			return "\ucd9c\uae08";
		}
		return "\uacb0\uc81c";
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

	private String buildVerificationAlarmBody(String verificationText) {
		if (verificationText == null || verificationText.isBlank()) {
			return "AMAGETDONE 입금";
		}
		String normalized = verificationText.trim();
		Matcher matcher = VERIFICATION_TEXT_PATTERN.matcher(normalized);
		if (matcher.find()) {
			return "AMAGETDONE " + matcher.group(2) + " 1원 입금";
		}
		return normalized + " 1원 입금";
	}

	private Map<String, String> buildVerificationAlarmData(String verificationText) {
		Map<String, String> data = new LinkedHashMap<>();
		data.put("type", "VERIFICATION_ALARM_DEMO");
		data.put("verificationText", verificationText == null ? "" : verificationText);
		return data;
	}

	private boolean isInvalidToken(MessagingErrorCode errorCode) {
		return errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT;
	}

	private String maskTokenSuffix(String token) {
		if (token == null || token.isBlank()) {
			return "empty";
		}
		int length = token.length();
		return length <= 8 ? token : token.substring(length - 8);
	}
}
