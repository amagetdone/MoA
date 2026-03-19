package com.ssafy.amagetdone.global.infra.ssafy.support;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
public class SsafyHttpErrorParser {

	private final ObjectMapper objectMapper;

	public SsafyHttpErrorParser() {
		this(new ObjectMapper());
	}

	public SsafyHttpErrorParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public ParsedError parse(String responseBody) {
		if (responseBody == null || responseBody.isBlank()) {
			return new ParsedError(null, null);
		}
		try {
			Map<String, Object> payload = objectMapper.readValue(responseBody, new TypeReference<>() {
			});
			return new ParsedError(
				asString(payload.get("responseCode")),
				asString(payload.get("responseMessage"))
			);
		} catch (Exception exception) {
			return new ParsedError(null, null);
		}
	}

	private String asString(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	public record ParsedError(
		String responseCode,
		String responseMessage
	) {
	}
}
