package com.ssafy.amagetdone.domain.plan.model.vo;

public enum PyanTaskStatus {
	QUEUED,
	PROCESSING,
	READY,
	CANCELLED,
	ERROR,
	UNKNOWN;

	public static PyanTaskStatus from(String status) {
		if (status == null || status.isBlank()) {
			return UNKNOWN;
		}
		try {
			return PyanTaskStatus.valueOf(status.trim().toUpperCase());
		} catch (IllegalArgumentException exception) {
			return UNKNOWN;
		}
	}
}
