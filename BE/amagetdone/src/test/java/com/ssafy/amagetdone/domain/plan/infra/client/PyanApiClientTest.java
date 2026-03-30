package com.ssafy.amagetdone.domain.plan.infra.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ssafy.amagetdone.domain.plan.infra.client.dto.PyanGenerateResponse;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PyanApiClientTest {

	@Test
	void generateMapsSnakeCaseResponseFields() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		PyanApiClient client = new PyanApiClient(builder, createProperties());

		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andExpect(header("X-API-Key", "pyan-api-key"))
			.andExpect(header("Idempotency-Key", "plan-10-pyan-1"))
			.andRespond(withSuccess("""
				{
				  "status": "ready",
				  "task_id": "task-1",
				  "goal_id": "plan-10",
				  "status_url": "https://sketch.pyan.kr/api/status/task-1",
				  "video_url": "https://sketch.pyan.kr/api/video/task-1?variant=original",
				  "goal_status_url": "https://sketch.pyan.kr/api/goals/plan-10",
				  "download_url": "https://sketch.pyan.kr/cdn/original.mp4",
				  "original_download_url": "https://sketch.pyan.kr/cdn/original.mp4",
				  "compressed_download_url": "https://sketch.pyan.kr/cdn/compressed.mp4"
				}
				""", APPLICATION_JSON));

		PyanGenerateResponse response = client.generate("generated-image".getBytes(), "plan-10", "plan-10-pyan-1");

		assertEquals("task-1", response.taskId());
		assertEquals("plan-10", response.goalId());
		assertEquals("https://sketch.pyan.kr/api/status/task-1", response.statusUrl());
		assertEquals("https://sketch.pyan.kr/api/goals/plan-10", response.goalStatusUrl());
		assertEquals("https://sketch.pyan.kr/cdn/original.mp4", response.originalDownloadUrl());
		assertEquals("https://sketch.pyan.kr/cdn/compressed.mp4", response.compressedDownloadUrl());
	}

	@Test
	void propertiesDefaultPenModeToNone() {
		PyanProperties properties = new PyanProperties();

		assertEquals("none", properties.getPenMode());
	}

	@Test
	void generateReturnsQueuedResponseWithoutPollingStatus() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		PyanApiClient client = new PyanApiClient(builder, createProperties());

		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andExpect(header("Idempotency-Key", "plan-11-pyan-1"))
			.andRespond(withSuccess("""
				{
				  "status": "queued",
				  "task_id": "task-2",
				  "goal_id": "plan-11",
				  "status_url": "https://sketch.pyan.kr/api/status/task-2"
				}
				""", APPLICATION_JSON));
		PyanGenerateResponse response = client.generate("generated-image".getBytes(), "plan-11", "plan-11-pyan-1");

		assertEquals("queued", response.status());
		assertEquals("task-2", response.taskId());
		assertEquals("https://sketch.pyan.kr/api/status/task-2", response.statusUrl());
	}

	@Test
	void generateDemoCallsDemoEndpointWithRequiredMultipartFields() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		PyanApiClient client = new PyanApiClient(builder, createProperties());

		server.expect(requestTo("https://sketch.pyan.kr/api/demo/generate"))
			.andExpect(method(POST))
			.andExpect(header("X-API-Key", "pyan-api-key"))
			.andExpect(header("Idempotency-Key", "plan-12-pyan-1"))
			.andExpect(content().string(containsString("name=\"goal_id\"")))
			.andExpect(content().string(containsString("plan-12")))
			.andExpect(content().string(containsString("name=\"user_id\"")))
			.andExpect(content().string(containsString("12")))
			.andExpect(content().string(containsString("name=\"plan_id\"")))
			.andExpect(content().string(containsString("name=\"plan_title\"")))
			.andExpect(content().string(containsString("바닷속 탐험 자금")))
			.andRespond(withSuccess("""
				{
				  "status": "queued",
				  "task_id": "task-demo-1",
				  "goal_id": "plan-12",
				  "status_url": "https://sketch.pyan.kr/api/status/task-demo-1",
				  "user_id": 12,
				  "plan_id": 12,
				  "plan_title": "바닷속 탐험 자금",
				  "demo_fallback_eligible": true,
				  "demo_fallback_scheduled": true
				}
				""", APPLICATION_JSON));

		PyanGenerateResponse response = client.generateDemo(
			"generated-image".getBytes(),
			"plan-12",
			"plan-12-pyan-1",
			12,
			12,
			"바닷속 탐험 자금"
		);

		assertEquals("task-demo-1", response.taskId());
		assertEquals(12, response.userId());
		assertEquals(12, response.planId());
		assertEquals("바닷속 탐험 자금", response.planTitle());
		assertEquals(Boolean.TRUE, response.demoFallbackEligible());
		assertEquals(Boolean.TRUE, response.demoFallbackScheduled());
	}

	@Test
	void fetchStatusLoadsCurrentTaskState() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		PyanApiClient client = new PyanApiClient(builder, createProperties());

		server.expect(requestTo("https://sketch.pyan.kr/api/status/task-3"))
			.andExpect(method(GET))
			.andRespond(withSuccess("""
				{
				  "status": "ready",
				  "task_id": "task-3",
				  "goal_id": "plan-12",
				  "compressed_download_url": "https://sketch.pyan.kr/cdn/task-3.compressed.mp4"
				}
				""", APPLICATION_JSON));

		PyanGenerateResponse response = client.fetchStatus("https://sketch.pyan.kr/api/status/task-3");

		assertEquals("ready", response.status());
		assertEquals("https://sketch.pyan.kr/cdn/task-3.compressed.mp4", response.compressedDownloadUrl());
	}

	@Test
	void generateRetriesWhenInitialRequestFails() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
		PyanApiClient client = new PyanApiClient(builder, createProperties());

		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andExpect(header("Idempotency-Key", "plan-13-pyan-1"))
			.andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());
		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andExpect(header("Idempotency-Key", "plan-13-pyan-1"))
			.andRespond(withSuccess("""
				{
				  "status": "queued",
				  "task_id": "task-4",
				  "goal_id": "plan-13",
				  "status_url": "https://sketch.pyan.kr/api/status/task-4"
				}
				""", APPLICATION_JSON));

		PyanGenerateResponse response = client.generate("generated-image".getBytes(), "plan-13", "plan-13-pyan-1");

		assertEquals("queued", response.status());
		assertEquals("task-4", response.taskId());
	}

	@Test
	void generateIncludesHttpStatusAndBodyWhenInitialRequestReturnsError() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		PyanProperties properties = createProperties();
		properties.setGenerateMaxAttempts(1);
		PyanApiClient client = new PyanApiClient(builder, properties);

		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(HttpStatus.UNAUTHORIZED)
				.contentType(APPLICATION_JSON)
				.body("""
					{"detail":"invalid api key"}
					"""));

		IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
			IllegalStateException.class,
			() -> client.generate("generated-image".getBytes(), "plan-14", "plan-14-pyan-1")
		);

		assertTrue(exception.getMessage().contains("status=401"));
		assertTrue(exception.getMessage().contains("invalid api key"));
	}

	@Test
	void generateReturnsCancelledResponseForCallerToHandle() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		PyanApiClient client = new PyanApiClient(builder, createProperties());

		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andRespond(withSuccess("""
				{
				  "status": "cancelled",
				  "task_id": "task-7",
				  "goal_id": "plan-16",
				  "message": "cancelled"
				}
				""", APPLICATION_JSON));

		PyanGenerateResponse response = client.generate("generated-image".getBytes(), "plan-16", "plan-16-pyan-1");

		assertEquals("cancelled", response.status());
		assertEquals("cancelled", response.message());
	}

	private PyanProperties createProperties() {
		PyanProperties properties = new PyanProperties();
		properties.setBaseUrl("https://sketch.pyan.kr");
		properties.setApiKey("pyan-api-key");
		properties.setPollInitialDelayMillis(1);
		properties.setPollMaxDelayMillis(1);
		properties.setPollTimeoutMillis(100);
		properties.setReadyMissingCompressedInitialDelayMillis(1);
		properties.setReadyMissingCompressedMaxAttempts(3);
		return properties;
	}
}
