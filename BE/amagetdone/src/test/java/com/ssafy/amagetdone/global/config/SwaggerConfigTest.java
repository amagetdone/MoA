package com.ssafy.amagetdone.global.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.junit.jupiter.api.Test;

class SwaggerConfigTest {

	private final SwaggerConfig swaggerConfig = new SwaggerConfig();

	@Test
	void addsBearerAuthSecurityScheme() {
		OpenAPI openApi = swaggerConfig.moreOpenApi("");
		SecurityScheme bearerAuth = openApi.getComponents().getSecuritySchemes().get("bearerAuth");

		assertNotNull(bearerAuth);
		assertEquals(SecurityScheme.Type.HTTP, bearerAuth.getType());
		assertEquals("bearer", bearerAuth.getScheme());
		assertEquals("JWT", bearerAuth.getBearerFormat());
	}

	@Test
	void omitsServersWhenExternalBaseUrlIsBlank() {
		OpenAPI openApi = swaggerConfig.moreOpenApi("");

		assertTrue(openApi.getServers() == null || openApi.getServers().isEmpty());
	}

	@Test
	void addsConfiguredExternalBaseUrlAsServer() {
		OpenAPI openApi = swaggerConfig.moreOpenApi("https://j14d101.p.ssafy.io/api");

		assertNotNull(openApi.getServers());
		assertEquals(1, openApi.getServers().size());
		assertEquals("https://j14d101.p.ssafy.io/api", openApi.getServers().get(0).getUrl());
	}
}
