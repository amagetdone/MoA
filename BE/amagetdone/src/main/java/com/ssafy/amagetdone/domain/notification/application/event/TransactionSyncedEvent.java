package com.ssafy.amagetdone.domain.notification.application.event;

import java.time.LocalDateTime;

public record TransactionSyncedEvent(
	Integer userId,
	String sourceType,
	String transactionId,
	Long amount,
	String merchantOrMemo,
	LocalDateTime occurredAt
) {
}
