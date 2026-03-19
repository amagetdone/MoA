package com.ssafy.amagetdone.domain.plan.infra.client;

public interface PlanAiClient {

	String generatePlanImage(String userPrompt, String imageUrl);
}
