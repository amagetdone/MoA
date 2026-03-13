package com.ssafy.amagetdone.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

class SecurityConfigPublicEndpointsTest {

	private final SecurityConfig securityConfig = new SecurityConfig();

	@Test
	void swaggerDocsArePublicInDevProfile() {
		RequestMatcher publicEndpoints = publicEndpointsFor("dev");

		assertTrue(publicEndpoints.matches(apiGet("/v3/api-docs")));
	}

	@Test
	void swaggerUiIsPublicInTestProfile() {
		RequestMatcher publicEndpoints = publicEndpointsFor("test");

		assertTrue(publicEndpoints.matches(apiGet("/swagger-ui/index.html")));
	}

	@Test
	void swaggerDocsRemainPrivateInProdProfile() {
		RequestMatcher publicEndpoints = publicEndpointsFor("prod");

		assertFalse(publicEndpoints.matches(apiGet("/v3/api-docs")));
	}

	@Test
	void authRefreshRemainsPublicInProdProfile() {
		RequestMatcher publicEndpoints = publicEndpointsFor("prod");

		assertTrue(publicEndpoints.matches(apiPost("/v1/auth/refresh")));
	}

	private RequestMatcher publicEndpointsFor(String... profiles) {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles(profiles);
		return securityConfig.publicEndpoints(environment);
	}

	private MockHttpServletRequest apiGet(String path) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api" + path);
		request.setContextPath("/api");
		request.setServletPath(path);
		return request;
	}

	private MockHttpServletRequest apiPost(String path) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api" + path);
		request.setContextPath("/api");
		request.setServletPath(path);
		return request;
	}
}
