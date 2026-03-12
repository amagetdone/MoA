package com.ssafy.amagetdone.domain.member.support.log;

import java.util.UUID;

import org.slf4j.MDC;

public final class MemberLogHelper {

	private MemberLogHelper() {
	}

	public static String requestId() {
		String requestId = MDC.get("requestId");
		if (requestId == null || requestId.isBlank()) {
			return UUID.randomUUID().toString();
		}
		return requestId;
	}

	public static String maskEmail(String email) {
		if (email == null || email.isBlank()) {
			return "unknown";
		}

		int atIndex = email.indexOf('@');
		if (atIndex <= 0) {
			return "***";
		}

		String localPart = email.substring(0, atIndex);
		String domain = email.substring(atIndex);
		if (localPart.length() <= 2) {
			return localPart.charAt(0) + "***" + domain;
		}
		if (localPart.length() <= 4) {
			return localPart.substring(0, 1) + "***" + localPart.charAt(localPart.length() - 1) + domain;
		}
		return localPart.substring(0, 2) + "***" + localPart.substring(localPart.length() - 2) + domain;
	}
}
