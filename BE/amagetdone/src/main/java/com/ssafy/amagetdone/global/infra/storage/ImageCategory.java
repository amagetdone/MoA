package com.ssafy.amagetdone.global.infra.storage;

public enum ImageCategory {
	PROFILE("profile"),
	PLAN("plan");

	private final String prefix;

	ImageCategory(String prefix) {
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}
}
