package com.ssafy.amagetdone.domain.plan.infra.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlanAiGenerateRequest(
	@JsonProperty("user_prompt") String userPrompt,
	@JsonProperty("image_url") String imageUrl
) {
}
