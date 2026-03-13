package com.ssafy.amagetdone.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EnvLoader {

	private EnvLoader() {
	}

	public static void loadRequiredEnv(Path baseDirectory) {
		Path envPath = baseDirectory.resolve(".env");
		if (!Files.exists(envPath)) {
			throw new IllegalStateException(".env file is required at " + envPath);
		}

		readEnvFile(envPath).forEach((key, value) -> {
			System.setProperty(key, value);
			if ("SPRING_PROFILES_ACTIVE".equals(key)) {
				System.setProperty("spring.profiles.active", value);
			}
		});
	}

	private static Map<String, String> readEnvFile(Path envPath) {
		try {
			Map<String, String> envValues = new LinkedHashMap<>();
			for (String line : Files.readAllLines(envPath)) {
				String trimmedLine = line.trim();
				if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
					continue;
				}

				int separatorIndex = trimmedLine.indexOf('=');
				if (separatorIndex <= 0) {
					continue;
				}

				String key = trimmedLine.substring(0, separatorIndex).trim();
				String value = trimmedLine.substring(separatorIndex + 1).trim();
				envValues.put(key, value);
			}
			return envValues;
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read .env file at " + envPath, exception);
		}
	}
}
