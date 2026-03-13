package com.ssafy.amagetdone.domain.transaction.infra.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class MerchantInferenceDto {

	private MerchantInferenceDto() {
	}

	public record Request(
		@JsonProperty("merchant_texts") List<String> merchantTexts
	) {
	}

	public record Response(
		List<Result> results
	) {
	}

	public record Result(
		@JsonProperty("merchant_text") String merchantText,
		@JsonProperty("normalized_merchant_text") String normalizedMerchantText,
		@JsonProperty("raw_label") String rawLabel,
		@JsonProperty("final_label") String finalLabel,
		Double confidence,
		@JsonProperty("fallback_applied") Boolean fallbackApplied,
		@JsonProperty("threshold_used") Double thresholdUsed
	) {
	}
}
