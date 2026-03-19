package com.ssafy.amagetdone.support;

import java.util.HashMap;
import java.util.Map;

public final class TestSystemPropertySupport {

	private static final String[] SPRING_PROFILE_PROPERTIES = {
		"SPRING_PROFILES_ACTIVE",
		"spring.profiles.active"
	};

	private TestSystemPropertySupport() {
	}

	public static Map<String, String> captureSpringProfileProperties() {
		Map<String, String> snapshot = new HashMap<>();
		for (String propertyName : SPRING_PROFILE_PROPERTIES) {
			snapshot.put(propertyName, System.getProperty(propertyName));
		}
		return snapshot;
	}

	public static void restore(Map<String, String> snapshot) {
		if (snapshot == null) {
			return;
		}

		for (Map.Entry<String, String> entry : snapshot.entrySet()) {
			if (entry.getValue() == null) {
				System.clearProperty(entry.getKey());
				continue;
			}
			System.setProperty(entry.getKey(), entry.getValue());
		}
	}
}
