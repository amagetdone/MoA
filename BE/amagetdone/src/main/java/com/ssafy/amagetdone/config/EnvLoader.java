package com.ssafy.amagetdone.config;

import java.nio.file.Files;
import java.nio.file.Path;

import io.github.cdimascio.dotenv.Dotenv;

public final class EnvLoader {

	private EnvLoader() {
	}

	public static void loadRequiredEnv(Path baseDirectory) {
		Path envPath = baseDirectory.resolve(".env");
		if (!Files.exists(envPath)) {
			throw new IllegalStateException(".env file is required at " + envPath);
		}

		Dotenv dotenv = Dotenv.configure()
			.directory(baseDirectory.toAbsolutePath().toString())
			.filename(".env")
			.load();

		dotenv.entries().forEach(entry -> {
			System.setProperty(entry.getKey(), entry.getValue());
			if ("SPRING_PROFILES_ACTIVE".equals(entry.getKey())) {
				System.setProperty("spring.profiles.active", entry.getValue());
			}
		});
	}
}
