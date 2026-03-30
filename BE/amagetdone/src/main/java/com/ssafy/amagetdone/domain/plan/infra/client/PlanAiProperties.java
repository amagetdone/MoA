package com.ssafy.amagetdone.domain.plan.infra.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plan-ai")
public class PlanAiProperties {

	private String baseUrl;
	private String generatePath = "/v1/image-generation";

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getGeneratePath() {
		return generatePath;
	}

	public void setGeneratePath(String generatePath) {
		this.generatePath = generatePath;
	}
}
