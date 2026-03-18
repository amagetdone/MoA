package com.ssafy.amagetdone.domain.plan.infra.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ssafy.amagetdone.domain.plan.infra.client.dto.PyanGenerateResponse;

import org.junit.jupiter.api.Test;
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
			.andExpect(header("Idempotency-Key", "plan-10-attempt-1"))
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

		PyanGenerateResponse response = client.generate("generated-image".getBytes(), "plan-10");

		assertEquals("task-1", response.taskId());
		assertEquals("plan-10", response.goalId());
		assertEquals("https://sketch.pyan.kr/api/status/task-1", response.statusUrl());
		assertEquals("https://sketch.pyan.kr/api/goals/plan-10", response.goalStatusUrl());
		assertEquals("https://sketch.pyan.kr/cdn/original.mp4", response.originalDownloadUrl());
		assertEquals("https://sketch.pyan.kr/cdn/compressed.mp4", response.compressedDownloadUrl());
	}

	@Test
	void propertiesDefaultPenModeToNo() {
		PyanProperties properties = new PyanProperties();

		assertEquals("no", properties.getPenMode());
	}

	@Test
	void generatePollsUntilReadyAndReturnsCompressedDownloadUrl() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		PyanApiClient client = new PyanApiClient(builder, createProperties());

		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andExpect(header("Idempotency-Key", "plan-11-attempt-1"))
			.andRespond(withSuccess("""
				{
				  "status": "queued",
				  "task_id": "task-2",
				  "goal_id": "plan-11",
				  "status_url": "https://sketch.pyan.kr/api/status/task-2"
				}
				""", APPLICATION_JSON));
		server.expect(requestTo("https://sketch.pyan.kr/api/status/task-2"))
			.andExpect(method(GET))
			.andRespond(withSuccess("""
				{
				  "status": "ready",
				  "task_id": "task-2",
				  "goal_id": "plan-11",
				  "compressed_download_url": "https://sketch.pyan.kr/cdn/task-2.compressed.mp4",
				  "original_download_url": "https://sketch.pyan.kr/cdn/task-2.mp4"
				}
				""", APPLICATION_JSON));

		PyanGenerateResponse response = client.generate("generated-image".getBytes(), "plan-11");

		assertEquals("ready", response.status());
		assertEquals("https://sketch.pyan.kr/cdn/task-2.compressed.mp4", response.compressedDownloadUrl());
	}

	@Test
	void generateRetriesWhenPolledStatusReturnsError() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		PyanApiClient client = new PyanApiClient(builder, createProperties());

		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andExpect(header("Idempotency-Key", "plan-12-attempt-1"))
			.andRespond(withSuccess("""
				{
				  "status": "queued",
				  "task_id": "task-3",
				  "goal_id": "plan-12",
				  "status_url": "https://sketch.pyan.kr/api/status/task-3"
				}
				""", APPLICATION_JSON));
		server.expect(requestTo("https://sketch.pyan.kr/api/status/task-3"))
			.andExpect(method(GET))
			.andRespond(withSuccess("""
				{
				  "status": "error",
				  "task_id": "task-3",
				  "goal_id": "plan-12",
				  "message": "generation failed"
				}
				""", APPLICATION_JSON));
		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andExpect(header("Idempotency-Key", "plan-12-attempt-2"))
			.andRespond(withSuccess("""
				{
				  "status": "ready",
				  "task_id": "task-3-retry",
				  "goal_id": "plan-12",
				  "status_url": "https://sketch.pyan.kr/api/status/task-3-retry",
				  "compressed_download_url": "https://sketch.pyan.kr/cdn/task-3-retry.compressed.mp4"
				}
				""", APPLICATION_JSON));

		PyanGenerateResponse response = client.generate("generated-image".getBytes(), "plan-12");

		assertEquals("ready", response.status());
		assertEquals("https://sketch.pyan.kr/cdn/task-3-retry.compressed.mp4", response.compressedDownloadUrl());
	}

	@Test
	void generateThrowsWhenPollingTimesOutAfterRetry() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
		PyanProperties properties = createProperties();
		properties.setPollTimeoutMillis(5);
		PyanApiClient client = new PyanApiClient(builder, properties);

		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andExpect(header("Idempotency-Key", "plan-13-attempt-1"))
			.andRespond(withSuccess("""
				{
				  "status": "queued",
				  "task_id": "task-4",
				  "goal_id": "plan-13",
				  "status_url": "https://sketch.pyan.kr/api/status/task-4"
				}
				""", APPLICATION_JSON));
		server.expect(ExpectedCount.manyTimes(), requestTo("https://sketch.pyan.kr/api/status/task-4"))
			.andExpect(method(GET))
			.andRespond(withSuccess("""
				{
				  "status": "processing",
				  "task_id": "task-4",
				  "goal_id": "plan-13",
				  "message": "still working"
				}
				""", APPLICATION_JSON));
		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andExpect(header("Idempotency-Key", "plan-13-attempt-2"))
			.andRespond(withSuccess("""
				{
				  "status": "queued",
				  "task_id": "task-4-retry",
				  "goal_id": "plan-13",
				  "status_url": "https://sketch.pyan.kr/api/status/task-4-retry"
				}
				""", APPLICATION_JSON));
		server.expect(ExpectedCount.manyTimes(), requestTo("https://sketch.pyan.kr/api/status/task-4-retry"))
			.andExpect(method(GET))
			.andRespond(withSuccess("""
				{
				  "status": "processing",
				  "task_id": "task-4-retry",
				  "goal_id": "plan-13",
				  "message": "still working"
				}
				""", APPLICATION_JSON));

		IllegalStateException exception = assertThrows(
			IllegalStateException.class,
			() -> client.generate("generated-image".getBytes(), "plan-13")
		);

		assertEquals("Pyan generation timed out after 5ms", exception.getMessage());
	}

	@Test
	void generatePollsReadyWithoutCompressedUrlBeforeRetryingGenerate() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		PyanProperties properties = createProperties();
		properties.setReadyMissingCompressedInitialDelayMillis(1);
		properties.setReadyMissingCompressedMaxAttempts(3);
		PyanApiClient client = new PyanApiClient(builder, properties);

		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andExpect(header("Idempotency-Key", "plan-14-attempt-1"))
			.andRespond(withSuccess("""
				{
				  "status": "ready",
				  "task_id": "task-5",
				  "goal_id": "plan-14",
				  "status_url": "https://sketch.pyan.kr/api/status/task-5",
				  "original_download_url": "https://sketch.pyan.kr/cdn/task-5.mp4"
				}
				""", APPLICATION_JSON));
		server.expect(requestTo("https://sketch.pyan.kr/api/status/task-5"))
			.andExpect(method(GET))
			.andRespond(withSuccess("""
				{
				  "status": "ready",
				  "task_id": "task-5",
				  "goal_id": "plan-14",
				  "compressed_download_url": "https://sketch.pyan.kr/cdn/task-5.compressed.mp4"
				}
				""", APPLICATION_JSON));

		PyanGenerateResponse response = client.generate("generated-image".getBytes(), "plan-14");

		assertEquals("ready", response.status());
		assertEquals("https://sketch.pyan.kr/cdn/task-5.compressed.mp4", response.compressedDownloadUrl());
	}

	@Test
	void generateRetriesWhenReadyDoesNotContainCompressedUrlAfterCompensation() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		PyanProperties properties = createProperties();
		properties.setReadyMissingCompressedInitialDelayMillis(1);
		properties.setReadyMissingCompressedMaxAttempts(2);
		PyanApiClient client = new PyanApiClient(builder, properties);

		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andExpect(header("Idempotency-Key", "plan-15-attempt-1"))
			.andRespond(withSuccess("""
				{
				  "status": "ready",
				  "task_id": "task-6",
				  "goal_id": "plan-15",
				  "status_url": "https://sketch.pyan.kr/api/status/task-6",
				  "original_download_url": "https://sketch.pyan.kr/cdn/task-6.mp4"
				}
				""", APPLICATION_JSON));
		server.expect(requestTo("https://sketch.pyan.kr/api/status/task-6"))
			.andExpect(method(GET))
			.andRespond(withSuccess("""
				{
				  "status": "ready",
				  "task_id": "task-6",
				  "goal_id": "plan-15",
				  "original_download_url": "https://sketch.pyan.kr/cdn/task-6.mp4"
				}
				""", APPLICATION_JSON));
		server.expect(requestTo("https://sketch.pyan.kr/api/status/task-6"))
			.andExpect(method(GET))
			.andRespond(withSuccess("""
				{
				  "status": "ready",
				  "task_id": "task-6",
				  "goal_id": "plan-15",
				  "original_download_url": "https://sketch.pyan.kr/cdn/task-6.mp4"
				}
				""", APPLICATION_JSON));
		server.expect(requestTo("https://sketch.pyan.kr/api/generate"))
			.andExpect(method(POST))
			.andExpect(header("Idempotency-Key", "plan-15-attempt-2"))
			.andRespond(withSuccess("""
				{
				  "status": "ready",
				  "task_id": "task-6-retry",
				  "goal_id": "plan-15",
				  "status_url": "https://sketch.pyan.kr/api/status/task-6-retry",
				  "compressed_download_url": "https://sketch.pyan.kr/cdn/task-6-retry.compressed.mp4"
				}
				""", APPLICATION_JSON));

		PyanGenerateResponse response = client.generate("generated-image".getBytes(), "plan-15");

		assertEquals("https://sketch.pyan.kr/cdn/task-6-retry.compressed.mp4", response.compressedDownloadUrl());
	}

	@Test
	void generateThrowsWhenCancelled() {
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

		IllegalStateException exception = assertThrows(
			IllegalStateException.class,
			() -> client.generate("generated-image".getBytes(), "plan-16")
		);

		assertEquals("Pyan generation cancelled: cancelled", exception.getMessage());
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
