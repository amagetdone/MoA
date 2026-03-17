package com.ssafy.amagetdone.global.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
