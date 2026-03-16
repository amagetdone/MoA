package com.ssafy.amagetdone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.ssafy.amagetdone.global.config.EnvLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnvLoaderTest {

	@TempDir
	Path tempDir;

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
