package com.ssafy.amagetdone.domain.transaction.infra.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SsafyApiPropertiesConfigurationTest {

	@Test
	void applicationPropertiesContainsSsafyInfraKeys() throws IOException {
		String applicationProperties = Files.readString(Path.of("src/main/resources/application.properties"));

		assertTrue(applicationProperties.contains("ssafy.fin-api.base-url="));
		assertTrue(applicationProperties.contains("ssafy.fin-api.api-key=${SSAFY_FIN_API_KEY:}"));
		assertTrue(applicationProperties.contains("asset.ssafy.base-url="));
		assertTrue(applicationProperties.contains("asset.ssafy.institution-code="));
		assertTrue(applicationProperties.contains("asset.ssafy.fintech-app-no="));
		assertTrue(applicationProperties.contains("asset.ssafy.api-key=${SSAFY_FIN_API_KEY:}"));
		assertTrue(applicationProperties.contains("transaction.ssafy.base-url="));
		assertTrue(applicationProperties.contains("transaction.ssafy.institution-code="));
		assertTrue(applicationProperties.contains("transaction.ssafy.fintech-app-no="));
		assertTrue(applicationProperties.contains("transaction.ssafy.api-key=${SSAFY_FIN_API_KEY:}"));
	}
}
