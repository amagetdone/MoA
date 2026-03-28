package com.ssafy.amagetdone.domain.accountverification.application.event;

import java.time.LocalDateTime;

public record VerificationRequestedEvent(
	Integer userId,
	String userKey,
	String accountNo,
	String authText,
	LocalDateTime requestedAt
) {
}
