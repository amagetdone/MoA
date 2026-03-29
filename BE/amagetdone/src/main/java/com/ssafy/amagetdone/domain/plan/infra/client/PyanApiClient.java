package com.ssafy.amagetdone.domain.plan.infra.client;

import com.ssafy.amagetdone.domain.plan.infra.client.dto.PyanGenerateResponse;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(prefix = "pyan", name = { "base-url", "api-key" })
public class PyanApiClient implements PyanClient {

	private static final Logger log = LoggerFactory.getLogger(PyanApiClient.class);
	private final RestClient restClient;
	private final PyanProperties properties;

	public PyanApiClient(RestClient.Builder restClientBuilder, PyanProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	@Override
	public PyanGenerateResponse generate(byte[] imageBytes, String goalId, String requestKey) {
		return generateWithRetry(
			imageBytes,
			goalId,
			requestKey,
			properties.getGeneratePath(),
			false,
			null,
			null,
			null
		);
	}

	@Override
	public PyanGenerateResponse generateDemo(
		byte[] imageBytes,
		String goalId,
		String requestKey,
		Integer userId,
		Integer planId,
		String planTitle
	) {
		return generateWithRetry(
			imageBytes,
			goalId,
			requestKey,
			properties.getDemoGeneratePath(),
			true,
			userId,
			planId,
			planTitle
		);
	}

	private PyanGenerateResponse generateWithRetry(
		byte[] imageBytes,
		String goalId,
		String requestKey,
		String endpointPath,
		boolean demoRequest,
		Integer userId,
		Integer planId,
		String planTitle
	) {
		IllegalStateException lastFailure = null;
		int maxGenerateAttempts = Math.max(1, properties.getGenerateMaxAttempts());
		for (int attempt = 1; attempt <= maxGenerateAttempts; attempt++) {
			try {
				PyanGenerateResponse response = generateOnce(
					imageBytes,
					goalId,
					requestKey,
					endpointPath,
					demoRequest,
					userId,
					planId,
					planTitle,
					attempt
				);
				log.info(
					"event=PYAN_HTTP_RESPONSE goalId={} attempt={} endpoint={} taskId={} status={} compressedDownloadUrl={} originalDownloadUrl={}",
					goalId,
					attempt,
					endpointPath,
					response.taskId(),
					response.status(),
					response.compressedDownloadUrl(),
					response.originalDownloadUrl()
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

	private PyanGenerateResponse generateOnce(
		byte[] imageBytes,
		String goalId,
		String requestKey,
		String endpointPath,
		boolean demoRequest,
		Integer userId,
		Integer planId,
		String planTitle,
		int attempt
	) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("image", new NamedByteArrayResource("plan.png", imageBytes));
		body.add("goal_id", goalId);
		body.add("pen_mode", properties.getPenMode());
		body.add("output_autocrop", String.valueOf(properties.isOutputAutocrop()));
		if (demoRequest) {
			body.add("user_id", String.valueOf(requireDemoValue(userId, "user_id")));
			body.add("plan_id", String.valueOf(requireDemoValue(planId, "plan_id")));
			body.add("plan_title", requireDemoTitle(planTitle));
		}
		String requestUrl = joinUrl(endpointPath);
		String idempotencyKey = requestKey;
		log.info(
			"event=PYAN_HTTP_REQUEST url={} goalId={} attempt={} imageBytesLength={} penMode={} outputAutocrop={} userId={} planId={} planTitle={}",
			requestUrl,
			goalId,
			attempt,
			imageBytes.length,
			properties.getPenMode(),
			properties.isOutputAutocrop(),
			userId,
			planId,
			planTitle
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
		} catch (RestClientResponseException exception) {
			String responseBody = exception.getResponseBodyAsString();
			String message = "Pyan generate request failed status="
				+ exception.getStatusCode().value()
				+ " body="
				+ abbreviate(responseBody, 300);
			throw new IllegalStateException(message, exception);
		} catch (RuntimeException exception) {
			throw new IllegalStateException("Pyan generate request failed cause=" + exception.getClass().getSimpleName(), exception);
		}
		return requireResponse(response, "Pyan generation returned no response");
	}

	@Override
	public PyanGenerateResponse fetchStatus(String statusUrl) {
		try {
			PyanGenerateResponse response = restClient.get()
				.uri(statusUrl)
				.header("X-API-Key", properties.getApiKey())
				.retrieve()
				.body(PyanGenerateResponse.class);
			return requireResponse(response, "Pyan status polling returned no response");
		} catch (RestClientResponseException exception) {
			String responseBody = exception.getResponseBodyAsString();
			String message = "Pyan status polling failed status="
				+ exception.getStatusCode().value()
				+ " body="
				+ abbreviate(responseBody, 300);
			throw new IllegalStateException(message, exception);
		} catch (RuntimeException exception) {
			throw new IllegalStateException("Pyan status polling failed cause=" + exception.getClass().getSimpleName(), exception);
		}
	}

	private PyanGenerateResponse requireResponse(PyanGenerateResponse response, String message) {
		if (response == null) {
			throw new IllegalStateException(message);
		}
		return response;
	}

	private boolean shouldRetry(IllegalStateException exception, int attempt) {
		if (attempt >= Math.max(1, properties.getGenerateMaxAttempts())) {
			return false;
		}
		String message = exception.getMessage();
		if (message == null) {
			return true;
		}
		if (message.startsWith("Pyan generation cancelled")) {
			return false;
		}
		if (message.startsWith("Pyan generation returned no response")) {
			return false;
		}
		return true;
	}

	private String joinUrl(String path) {
		String baseUrl = properties.getBaseUrl();
		if (baseUrl.endsWith("/")) {
			return baseUrl.substring(0, baseUrl.length() - 1) + path;
		}
		return baseUrl + path;
	}

	private String abbreviate(String value, int maxLength) {
		if (value == null || value.isBlank()) {
			return "<empty>";
		}
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength) + "...";
	}

	private Integer requireDemoValue(Integer value, String fieldName) {
		return Objects.requireNonNull(value, "Pyan demo request requires " + fieldName);
	}

	private String requireDemoTitle(String planTitle) {
		if (planTitle == null || planTitle.isBlank()) {
			throw new IllegalArgumentException("Pyan demo request requires plan_title");
		}
		return planTitle;
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
