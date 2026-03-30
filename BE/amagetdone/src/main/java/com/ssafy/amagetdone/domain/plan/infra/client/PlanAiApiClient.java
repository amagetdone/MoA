package com.ssafy.amagetdone.domain.plan.infra.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.amagetdone.domain.plan.infra.client.dto.PlanAiGenerateRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "plan-ai", name = "base-url")
public class PlanAiApiClient implements PlanAiClient {

	private static final Logger log = LoggerFactory.getLogger(PlanAiApiClient.class);

	private final RestClient restClient;
	private final PlanAiProperties properties;

	public PlanAiApiClient(RestClient.Builder restClientBuilder, PlanAiProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	@Override
	public String generatePlanImage(String userPrompt, String imageUrl) {
		String requestUrl = joinUrl(properties.getGeneratePath());
		log.info(
			"event=PLAN_AI_HTTP_REQUEST url={} imageUrl={} userPrompt={}",
			requestUrl,
			imageUrl,
			userPrompt
		);
		JsonNode response = restClient.post()
			.uri(requestUrl)
			.body(new PlanAiGenerateRequest(userPrompt, imageUrl))
			.retrieve()
			.body(JsonNode.class);
		logResponseSummary(requestUrl, response);

		return extractBase64(response);
	}

	private String joinUrl(String path) {
		String baseUrl = properties.getBaseUrl();
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		if (path.startsWith("/")) {
			return baseUrl + path;
		}
		return baseUrl + "/" + path;
	}

	private String extractBase64(JsonNode response) {
		if (response == null || response.isNull()) {
			throw new IllegalStateException("AI image response is empty.");
		}
		if (response.isTextual()) {
			return response.asText();
		}
		for (String fieldName : new String[] { "image_base64", "base64", "image", "data" }) {
			JsonNode field = response.get(fieldName);
			if (field != null && field.isTextual() && !field.asText().isBlank()) {
				return field.asText();
			}
		}
		throw new IllegalStateException("AI image response does not contain a base64 image.");
	}

	private void logResponseSummary(String requestUrl, JsonNode response) {
		if (response == null || response.isNull()) {
			log.info("event=PLAN_AI_HTTP_RESPONSE_SUMMARY url={} responseType=empty", requestUrl);
			return;
		}
		if (response.isTextual()) {
			log.info(
				"event=PLAN_AI_HTTP_RESPONSE_SUMMARY url={} responseType=text imageBase64Length={}",
				requestUrl,
				response.asText().isBlank() ? null : response.asText().length()
			);
			return;
		}
		String imageBase64 = textValue(response, "image_base64");
		log.info(
			"event=PLAN_AI_HTTP_RESPONSE_SUMMARY url={} responseType=json mimeType={} model={} prompt={} imageBase64Length={}",
			requestUrl,
			textValue(response, "mime_type"),
			textValue(response, "model"),
			textValue(response, "prompt"),
			imageBase64 != null ? imageBase64.length() : null
		);
	}

	private String textValue(JsonNode response, String fieldName) {
		JsonNode field = response.get(fieldName);
		if (field == null || !field.isTextual()) {
			return null;
		}
		String value = field.asText();
		return value.isBlank() ? null : value;
	}
}
