package com.ssafy.amagetdone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ssafy.amagetdone.global.config.EnvLoader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnvLoaderTest {

	private static final List<String> MANAGED_PROPERTIES = List.of(
		"SPRING_PROFILES_ACTIVE",
		"spring.profiles.active",
		"JWT_SECRET"
	);

	@TempDir
	Path tempDir;

	private final Map<String, String> originalProperties = new HashMap<>();

	@BeforeEach
	void captureOriginalProperties() {
		for (String propertyName : MANAGED_PROPERTIES) {
			originalProperties.put(propertyName, System.getProperty(propertyName));
		}
	}

	@AfterEach
	void restoreOriginalProperties() {
		for (String propertyName : MANAGED_PROPERTIES) {
			String originalValue = originalProperties.get(propertyName);
			if (originalValue == null) {
				System.clearProperty(propertyName);
				continue;
			}
			System.setProperty(propertyName, originalValue);
		}
		originalProperties.clear();
	}

	@Test
	void loadRequiredEnvThrowsWhenDotEnvFileIsMissing() {
		assertThrows(IllegalStateException.class, () -> EnvLoader.loadRequiredEnv(tempDir));
	}

	@Test
	void loadRequiredEnvCopiesValuesIntoSystemProperties() throws IOException {
		Files.writeString(tempDir.resolve(".env"), "SPRING_PROFILES_ACTIVE=prod\nJWT_SECRET=test-secret\n");

		EnvLoader.loadRequiredEnv(tempDir);

		assertEquals("prod", System.getProperty("spring.profiles.active"));
		assertEquals("test-secret", System.getProperty("JWT_SECRET"));
	}

	@Test
	void loadRequiredEnvFindsDotEnvInParentDirectory() throws IOException {
		Files.writeString(tempDir.resolve(".env"), "SPRING_PROFILES_ACTIVE=test\nJWT_SECRET=parent-secret\n");
		Path nestedDir = Files.createDirectories(tempDir.resolve("worktrees/feature/backend"));

		EnvLoader.loadRequiredEnv(nestedDir);

		assertEquals("test", System.getProperty("spring.profiles.active"));
		assertEquals("parent-secret", System.getProperty("JWT_SECRET"));
	}
}
