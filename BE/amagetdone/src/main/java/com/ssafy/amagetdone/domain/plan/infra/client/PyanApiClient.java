package com.ssafy.amagetdone.domain.plan.infra.client;

import com.ssafy.amagetdone.domain.plan.infra.client.dto.PyanGenerateResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "pyan", name = { "base-url", "api-key" })
public class PyanApiClient implements PyanClient {

	private static final Logger log = LoggerFactory.getLogger(PyanApiClient.class);
	private static final int MAX_GENERATE_ATTEMPTS = 2;

	private final RestClient restClient;
	private final PyanProperties properties;

	public PyanApiClient(RestClient.Builder restClientBuilder, PyanProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	@Override
	public PyanGenerateResponse generate(byte[] imageBytes, String goalId) {
		IllegalStateException lastFailure = null;
		for (int attempt = 1; attempt <= MAX_GENERATE_ATTEMPTS; attempt++) {
			try {
				PyanGenerateResponse response = generateOnce(imageBytes, goalId, attempt);
				log.info(
					"event=PYAN_HTTP_RESPONSE goalId={} attempt={} taskId={} status={} compressedDownloadUrl={}",
					goalId,
					attempt,
					response.taskId(),
					response.status(),
					response.compressedDownloadUrl()
				);
				return response;
			} catch (IllegalStateException exception) {
				lastFailure = exception;
				if (!shouldRetry(exception, attempt)) {
					throw exception;
				}
				log.warn(
					"event=PYAN_GENERATE_RETRY goalId={} attempt={} message={}",
					goalId,
					attempt,
					exception.getMessage()
				);
			}
		}
		throw lastFailure == null ? new IllegalStateException("Pyan generation failed") : lastFailure;
	}

	private PyanGenerateResponse generateOnce(byte[] imageBytes, String goalId, int attempt) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("image", new NamedByteArrayResource("plan.png", imageBytes));
		body.add("goal_id", goalId);
		body.add("pen_mode", properties.getPenMode());
		body.add("output_autocrop", String.valueOf(properties.isOutputAutocrop()));
		String requestUrl = joinUrl("/api/generate");
		String idempotencyKey = goalId + "-attempt-" + attempt;
		log.info(
			"event=PYAN_HTTP_REQUEST url={} goalId={} attempt={} imageBytesLength={} penMode={} outputAutocrop={}",
			requestUrl,
			goalId,
			attempt,
			imageBytes.length,
			properties.getPenMode(),
			properties.isOutputAutocrop()
		);

		PyanGenerateResponse response;
		try {
			response = restClient.post()
				.uri(requestUrl)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.header("X-API-Key", properties.getApiKey())
				.header("Idempotency-Key", idempotencyKey)
				.body(body)
				.retrieve()
				.body(PyanGenerateResponse.class);
		} catch (RuntimeException exception) {
			throw new IllegalStateException("Pyan generate request failed", exception);
		}
		return waitForCompressedDownloadUrl(response);
	}

	private PyanGenerateResponse waitForCompressedDownloadUrl(PyanGenerateResponse response) {
		PyanGenerateResponse current = requireResponse(response, "Pyan generation returned no response");
		if (isCancelled(current)) {
			throw new IllegalStateException("Pyan generation cancelled: " + current.message());
		}
		if (isError(current)) {
			throw new IllegalStateException("Pyan generation failed: " + current.message());
		}
		if (hasCompressedDownloadUrl(current)) {
			return current;
		}
		if (isReady(current)) {
			return compensateReadyWithoutCompressed(current);
		}

		String statusUrl = requireStatusUrl(current);
		long timeoutMillis = properties.getPollTimeoutMillis();
		long deadline = System.currentTimeMillis() + timeoutMillis;
		long delayMillis = Math.max(1L, properties.getPollInitialDelayMillis());
		while (System.currentTimeMillis() < deadline) {
			sleep(Math.min(delayMillis, Math.max(1L, deadline - System.currentTimeMillis())));
			current = fetchStatus(statusUrl);
			if (isCancelled(current)) {
				throw new IllegalStateException("Pyan generation cancelled: " + current.message());
			}
			if (isError(current)) {
				throw new IllegalStateException("Pyan generation failed: " + current.message());
			}
			if (hasCompressedDownloadUrl(current)) {
				return current;
			}
			if (isReady(current)) {
				return compensateReadyWithoutCompressed(current);
			}
			delayMillis = nextDelayMillis(delayMillis);
		}
		throw new IllegalStateException("Pyan generation timed out after " + timeoutMillis + "ms");
	}

	private PyanGenerateResponse compensateReadyWithoutCompressed(PyanGenerateResponse response) {
		String taskId = response.taskId();
		if (!hasText(taskId)) {
			throw new IllegalStateException("Pyan ready response missing task id");
		}
		String statusUrl = joinUrl("/api/status/" + taskId);
		long delayMillis = Math.max(1L, properties.getReadyMissingCompressedInitialDelayMillis());
		int maxAttempts = Math.max(1, properties.getReadyMissingCompressedMaxAttempts());
		PyanGenerateResponse current = response;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			sleep(delayMillis);
			current = fetchStatus(statusUrl);
			if (isCancelled(current)) {
				throw new IllegalStateException("Pyan generation cancelled: " + current.message());
			}
			if (isError(current)) {
				throw new IllegalStateException("Pyan generation failed: " + current.message());
			}
			if (hasCompressedDownloadUrl(current)) {
				return current;
			}
			delayMillis += Math.max(1L, properties.getReadyMissingCompressedInitialDelayMillis());
		}
		throw new IllegalStateException("Pyan ready response missing compressed download URL");
	}

	private PyanGenerateResponse fetchStatus(String statusUrl) {
		try {
			PyanGenerateResponse response = restClient.get()
				.uri(statusUrl)
				.header("X-API-Key", properties.getApiKey())
				.retrieve()
				.body(PyanGenerateResponse.class);
			return requireResponse(response, "Pyan status polling returned no response");
		} catch (RuntimeException exception) {
			throw new IllegalStateException("Pyan status polling failed", exception);
		}
	}

	private PyanGenerateResponse requireResponse(PyanGenerateResponse response, String message) {
		if (response == null) {
			throw new IllegalStateException(message);
		}
		return response;
	}

	private String requireStatusUrl(PyanGenerateResponse response) {
		if (!hasText(response.statusUrl())) {
			throw new IllegalStateException("Pyan generation did not provide a status URL");
		}
		return response.statusUrl();
	}

	private boolean shouldRetry(IllegalStateException exception, int attempt) {
		if (attempt >= MAX_GENERATE_ATTEMPTS) {
			return false;
		}
		String message = exception.getMessage();
		if (message == null) {
			return true;
		}
		if (message.startsWith("Pyan generation cancelled")) {
			return false;
		}
		if (message.startsWith("Pyan generation did not provide a status URL")) {
			return false;
		}
		if (message.startsWith("Pyan ready response missing task id")) {
			return false;
		}
		if (message.startsWith("Pyan generation returned no response")) {
			return false;
		}
		if (message.startsWith("Pyan status polling returned no response")) {
			return false;
		}
		return true;
	}

	private boolean hasCompressedDownloadUrl(PyanGenerateResponse response) {
		return hasText(response.compressedDownloadUrl());
	}

	private boolean isReady(PyanGenerateResponse response) {
		return "ready".equalsIgnoreCase(response.status());
	}

	private boolean isCancelled(PyanGenerateResponse response) {
		return "cancelled".equalsIgnoreCase(response.status());
	}

	private boolean isError(PyanGenerateResponse response) {
		return "error".equalsIgnoreCase(response.status());
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private long nextDelayMillis(long currentDelayMillis) {
		long maxDelayMillis = Math.max(1L, properties.getPollMaxDelayMillis());
		long doubledDelay = currentDelayMillis * 2;
		if (doubledDelay < 0) {
			return maxDelayMillis;
		}
		return Math.min(maxDelayMillis, doubledDelay);
	}

	private void sleep(long delayMillis) {
		try {
			Thread.sleep(delayMillis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Pyan polling interrupted", exception);
		}
	}

	private String joinUrl(String path) {
		String baseUrl = properties.getBaseUrl();
		if (baseUrl.endsWith("/")) {
			return baseUrl.substring(0, baseUrl.length() - 1) + path;
		}
		return baseUrl + path;
	}

	private static final class NamedByteArrayResource extends ByteArrayResource {
		private final String fileName;

		private NamedByteArrayResource(String fileName, byte[] byteArray) {
			super(byteArray);
			this.fileName = fileName;
		}

		@Override
		public String getFilename() {
			return fileName;
		}
	}
}
