package com.ssafy.amagetdone.global.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.s3.AmazonS3;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SwaggerForwardedHeadersIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate testRestTemplate;

	@MockitoBean
	private AmazonS3 amazonS3;

	@Test
	@SuppressWarnings("unchecked")
	void apiDocsUseForwardedHttpsSchemeWhenProxyHeadersArePresent() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Forwarded-Proto", "https");
		headers.add("X-Forwarded-Host", "j14d101.p.ssafy.io");
		headers.add("X-Forwarded-Port", "443");

		ResponseEntity<Map> response = testRestTemplate.exchange(
			"http://localhost:" + port + "/api/v3/api-docs",
			HttpMethod.GET,
			new HttpEntity<>(headers),
			Map.class
		);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		List<Map<String, String>> servers = (List<Map<String, String>>) response.getBody().get("servers");
		assertEquals("https://j14d101.p.ssafy.io/api", servers.get(0).get("url"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void budgetHomeApiDocsExposeBudgetHomeResponseSchema() {
		ResponseEntity<Map> response = testRestTemplate.getForEntity(
			"http://localhost:" + port + "/api/v3/api-docs",
			Map.class
		);

		assertEquals(HttpStatus.OK, response.getStatusCode());

		Map<String, Object> body = response.getBody();
		Map<String, Object> paths = (Map<String, Object>) body.get("paths");
		Map<String, Object> budgetHomePath = (Map<String, Object>) paths.get("/budgets/home");
		Map<String, Object> get = (Map<String, Object>) budgetHomePath.get("get");
		Map<String, Object> responses = (Map<String, Object>) get.get("responses");
		Map<String, Object> ok = (Map<String, Object>) responses.get("200");
		Map<String, Object> content = (Map<String, Object>) ok.get("content");
		Map<String, Object> anyContent = (Map<String, Object>) content.get("*/*");
		Map<String, Object> schema = (Map<String, Object>) anyContent.get("schema");
		Map<String, Object> components = (Map<String, Object>) body.get("components");
		Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

		assertEquals("#/components/schemas/BudgetHomeApiResponse", schema.get("$ref"));

		Map<String, Object> apiResponseBudgetHome = (Map<String, Object>) schemas.get("BudgetHomeApiResponse");
		Map<String, Object> apiResponseBudgetHomeProperties = (Map<String, Object>) apiResponseBudgetHome.get("properties");
		Map<String, Object> data = (Map<String, Object>) apiResponseBudgetHomeProperties.get("data");
		assertEquals("#/components/schemas/BudgetHomeResponse", data.get("$ref"));

		Map<String, Object> budgetHome = (Map<String, Object>) schemas.get("BudgetHomeResponse");
		Map<String, Object> budgetHomeProperties = (Map<String, Object>) budgetHome.get("properties");
		assertTrue(budgetHomeProperties.containsKey("freeSpending"));
		assertTrue(budgetHomeProperties.containsKey("weeklySaving"));
		assertTrue(budgetHomeProperties.containsKey("assetSummary"));
		assertTrue(budgetHomeProperties.containsKey("plans"));
	}
}
